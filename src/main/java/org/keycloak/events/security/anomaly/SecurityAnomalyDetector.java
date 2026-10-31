package org.keycloak.events.security.anomaly;

import org.infinispan.Cache;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Detector de anomalías de seguridad respaldado directamente por Infinispan (Caché nativo de Keycloak).
 * Sincronizado automáticamente entre pods (HPA / Clúster) y con expiración nativa (TTL).
 */
public class SecurityAnomalyDetector {

    private final int ipThreshold;
    private final int realmThreshold;
    private final long windowSeconds;
    private final long cooldownSeconds;

    public static class AnomalyAlert {
        private final String title;
        private final String message;
        private final String type;
        private final int totalFailures;
        private final int distinctUsers;

        public AnomalyAlert(String type, String title, String message, int totalFailures, int distinctUsers) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.totalFailures = totalFailures;
            this.distinctUsers = distinctUsers;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public int getTotalFailures() { return totalFailures; }
        public int getDistinctUsers() { return distinctUsers; }
    }

    public SecurityAnomalyDetector(int ipThreshold, int realmThreshold, long windowSeconds, long cooldownSeconds) {
        this.ipThreshold = ipThreshold;
        this.realmThreshold = realmThreshold;
        this.windowSeconds = windowSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    public static SecurityAnomalyDetector fromEnvironment() {
        int ipThreshold = getEnvInt("KC_ALERT_IP_FAILURES", 5);
        int realmThreshold = getEnvInt("KC_ALERT_REALM_FAILURES", 20);
        long windowSeconds = getEnvLong("KC_ALERT_WINDOW_SECONDS", 60);
        long cooldownSeconds = getEnvLong("KC_ALERT_COOLDOWN_SECONDS", 120);

        return new SecurityAnomalyDetector(ipThreshold, realmThreshold, windowSeconds, cooldownSeconds);
    }

    /**
     * Registra un fallo de login en la caché distribuida de Infinispan y evalúa
     * si se superaron los umbrales configurados para generar alertas.
     */
    public List<AnomalyAlert> registerLoginFailure(KeycloakSession session, String ipAddress, String username, String realmName) {
        List<AnomalyAlert> alerts = new ArrayList<>();
        try {
            long now = System.currentTimeMillis();
            long cutoff = now - (windowSeconds * 1000L);

            String ip = (ipAddress != null && !ipAddress.trim().isEmpty()) ? ipAddress.trim() : "UNKNOWN_IP";
            String realm = (realmName != null && !realmName.trim().isEmpty()) ? realmName.trim() : "master";
            String user = (username != null && !username.trim().isEmpty()) ? username.trim() : "unknown";

            InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);
            if (ispn == null) {
                return alerts;
            }

            Cache<String, String> cache = null;
            try {
                cache = ispn.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
            } catch (Throwable ignored) {
                // Si loginFailures no está configurado en el perfil actual, intentar con fallback de sesiones o ignorar
            }
            if (cache == null) {
                return alerts;
            }

            // 1. Evaluación por IP en Infinispan (Password Spraying / Fuerza bruta)
            String ipKey = "kc:sec:ip:" + realm + ":" + ip;
            String rawIpData = cache.get(ipKey);

            List<String> validAttempts = new ArrayList<>();
            Set<String> uniqueUsers = new HashSet<>();

            if (rawIpData != null && !rawIpData.isEmpty()) {
                String[] entries = rawIpData.split(";");
                for (String entry : entries) {
                    int sep = entry.indexOf(":");
                    if (sep > 0) {
                        try {
                            long ts = Long.parseLong(entry.substring(0, sep));
                            if (ts >= cutoff) {
                                validAttempts.add(entry);
                                uniqueUsers.add(entry.substring(sep + 1));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            validAttempts.add(now + ":" + user);
            uniqueUsers.add(user);

            // Guardar en Infinispan con TTL automático igual a la ventana de tiempo
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < validAttempts.size(); i++) {
                if (i > 0) sb.append(";");
                sb.append(validAttempts.get(i));
            }
            cache.put(ipKey, sb.toString(), windowSeconds, TimeUnit.SECONDS);

            if (validAttempts.size() >= ipThreshold) {
                String cooldownKey = "kc:sec:cd:ip:" + realm + ":" + ip;
                // Bloqueo atómico distribuido en el clúster con TTL de enfriamiento
                if (cache.putIfAbsent(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS) == null) {
                    String alertMsg = String.format("La IP %s ha acumulado %d intentos fallidos en los últimos %d segundos afectando a %d cuenta(s) distinta(s).",
                            ip, validAttempts.size(), windowSeconds, uniqueUsers.size());

                    alerts.add(new AnomalyAlert(
                            "IP_BRUTE_FORCE",
                            "🚨 [ALERTA DE SEGURIDAD] Ataque de Fuerza Bruta / Spraying por IP",
                            alertMsg,
                            validAttempts.size(),
                            uniqueUsers.size()
                    ));
                }
            }

            // 2. Evaluación Global del Realm en Infinispan (Pico masivo de fallos)
            String realmKey = "kc:sec:realm:" + realm;
            String rawRealmData = cache.get(realmKey);

            List<Long> validRealmTimestamps = new ArrayList<>();
            if (rawRealmData != null && !rawRealmData.isEmpty()) {
                String[] entries = rawRealmData.split(",");
                for (String entry : entries) {
                    try {
                        long ts = Long.parseLong(entry);
                        if (ts >= cutoff) {
                            validRealmTimestamps.add(ts);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            validRealmTimestamps.add(now);

            StringBuilder sbRealm = new StringBuilder();
            for (int i = 0; i < validRealmTimestamps.size(); i++) {
                if (i > 0) sbRealm.append(",");
                sbRealm.append(validRealmTimestamps.get(i));
            }
            cache.put(realmKey, sbRealm.toString(), windowSeconds, TimeUnit.SECONDS);

            if (validRealmTimestamps.size() >= realmThreshold) {
                String cooldownRealmKey = "kc:sec:cd:realm:" + realm;
                if (cache.putIfAbsent(cooldownRealmKey, "1", cooldownSeconds, TimeUnit.SECONDS) == null) {
                    String alertMsg = String.format("Pico masivo de %d intentos fallidos de login detectados en el realm '%s' en los últimos %d segundos.",
                            validRealmTimestamps.size(), realm, windowSeconds);

                    alerts.add(new AnomalyAlert(
                            "REALM_SPIKE",
                            "🔥 [ALERTA DE SEGURIDAD] Pico Global Anómalo de Errores de Login",
                            alertMsg,
                            validRealmTimestamps.size(),
                            0
                    ));
                }
            }
        } catch (Throwable ignored) {
            // Protección integral contra excepciones en cache/parsing
        }

        return alerts;
    }

    private static int getEnvInt(String name, int defaultValue) {
        String val = System.getenv(name);
        if (val != null && val.matches("\\d+")) {
            return Integer.parseInt(val);
        }
        return defaultValue;
    }

    private static long getEnvLong(String name, long defaultValue) {
        String val = System.getenv(name);
        if (val != null && val.matches("\\d+")) {
            return Long.parseLong(val);
        }
        return defaultValue;
    }
}
