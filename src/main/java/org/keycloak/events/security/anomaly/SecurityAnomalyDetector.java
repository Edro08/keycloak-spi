package org.keycloak.events.security.anomaly;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String SECURITY_CACHE_NAME = "securityAnomalyCache";
    private static final Map<String, String> FALLBACK_MEMORY_CACHE = new ConcurrentHashMap<>();

    private Cache<String, String> resolveCache(InfinispanConnectionProvider ispn) {
        if (ispn == null) {
            return null;
        }

        try {
            // 1. Intentar obtener la caché si ya está definida
            Cache<String, String> cache = ispn.getCache(SECURITY_CACHE_NAME);
            if (cache != null) {
                return cache;
            }
        } catch (Throwable ignored) {}

        try {
            // 2. Definición dinámica por código en el CacheManager de Infinispan
            Cache<?, ?> baseCache = ispn.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
            if (baseCache != null && baseCache.getCacheManager() != null) {
                EmbeddedCacheManager manager = baseCache.getCacheManager();
                synchronized (SecurityAnomalyDetector.class) {
                    if (manager.getCacheConfiguration(SECURITY_CACHE_NAME) == null) {
                        Configuration baseCfg = manager.getCacheConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME);
                        ConfigurationBuilder builder = (baseCfg != null)
                                ? new ConfigurationBuilder().read(baseCfg)
                                : new ConfigurationBuilder();

                        builder.expiration()
                                .lifespan(Math.max(cooldownSeconds, 180), TimeUnit.SECONDS)
                                .maxIdle(Math.max(windowSeconds, 60), TimeUnit.SECONDS);

                        manager.defineConfiguration(SECURITY_CACHE_NAME, builder.build());
                    }
                }
                return manager.getCache(SECURITY_CACHE_NAME);
            }
        } catch (Throwable ignored) {}

        return null;
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
            Cache<String, String> cache = resolveCache(ispn);
            Map<String, String> store = (cache != null) ? cache : FALLBACK_MEMORY_CACHE;

            // 1. Evaluación por IP en Infinispan (Password Spraying / Fuerza bruta)
            String ipKey = "kc:sec:ip:" + realm + ":" + ip;
            String rawIpData = store.get(ipKey);

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

            // Guardar con TTL automático igual a la ventana de tiempo
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < validAttempts.size(); i++) {
                if (i > 0) sb.append(";");
                sb.append(validAttempts.get(i));
            }
            if (cache != null) {
                cache.put(ipKey, sb.toString(), windowSeconds, TimeUnit.SECONDS);
            } else {
                FALLBACK_MEMORY_CACHE.put(ipKey, sb.toString());
            }

            if (validAttempts.size() >= ipThreshold) {
                String cooldownKey = "kc:sec:cd:ip:" + realm + ":" + ip;
                boolean shouldAlert = (cache != null)
                        ? (cache.putIfAbsent(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS) == null)
                        : (FALLBACK_MEMORY_CACHE.putIfAbsent(cooldownKey, "1") == null);

                if (shouldAlert) {
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
            String rawRealmData = store.get(realmKey);

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
            if (cache != null) {
                cache.put(realmKey, sbRealm.toString(), windowSeconds, TimeUnit.SECONDS);
            } else {
                FALLBACK_MEMORY_CACHE.put(realmKey, sbRealm.toString());
            }

            if (validRealmTimestamps.size() >= realmThreshold) {
                String cooldownRealmKey = "kc:sec:cd:realm:" + realm;
                boolean shouldAlert = (cache != null)
                        ? (cache.putIfAbsent(cooldownRealmKey, "1", cooldownSeconds, TimeUnit.SECONDS) == null)
                        : (FALLBACK_MEMORY_CACHE.putIfAbsent(cooldownRealmKey, "1") == null);

                if (shouldAlert) {
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
