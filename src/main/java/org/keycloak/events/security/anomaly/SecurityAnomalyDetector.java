package org.keycloak.events.security.anomaly;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Detector de anomalías de seguridad en memoria con ventanas deslizantes de tiempo.
 * Identifica ataques de fuerza bruta por IP (Password Spraying) y picos masivos de errores globales.
 */
public class SecurityAnomalyDetector {

    private final int ipThreshold;
    private final int realmThreshold;
    private final long windowSeconds;
    private final long cooldownSeconds;

    // IP -> Cola de intentos en la ventana actual
    private final Map<String, Queue<FailureAttempt>> ipFailures = new ConcurrentHashMap<>();
    private final Map<String, Instant> ipLastAlertTime = new ConcurrentHashMap<>();

    // Realm -> Cola de timestamps de fallos globales en la ventana actual
    private final Map<String, Queue<Instant>> realmFailures = new ConcurrentHashMap<>();
    private final Map<String, Instant> realmLastAlertTime = new ConcurrentHashMap<>();

    public static class FailureAttempt {
        final Instant timestamp;
        final String username;

        FailureAttempt(Instant timestamp, String username) {
            this.timestamp = timestamp;
            this.username = username != null ? username : "unknown";
        }
    }

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
     * Registra un LOGIN_ERROR y evalúa si se superaron los umbrales de ataque.
     *
     * @return Lista de alertas disparadas (por IP y/o Global). Vacía si está dentro del umbral normal.
     */
    public List<AnomalyAlert> registerLoginFailure(String ipAddress, String username, String realmName) {
        List<AnomalyAlert> alerts = new ArrayList<>();
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(windowSeconds);

        String ip = (ipAddress != null && !ipAddress.trim().isEmpty()) ? ipAddress.trim() : "UNKNOWN_IP";
        String realm = (realmName != null && !realmName.trim().isEmpty()) ? realmName.trim() : "master";

        // 1. Evaluación por IP (Password Spraying / Fuerza bruta focalizada)
        Queue<FailureAttempt> ipQueue = ipFailures.computeIfAbsent(ip, k -> new ConcurrentLinkedQueue<>());
        ipQueue.removeIf(attempt -> attempt.timestamp.isBefore(cutoff));
        ipQueue.add(new FailureAttempt(now, username));

        if (ipQueue.size() >= ipThreshold) {
            Instant lastAlert = ipLastAlertTime.get(ip);
            if (lastAlert == null || lastAlert.isBefore(now.minusSeconds(cooldownSeconds))) {
                ipLastAlertTime.put(ip, now);

                Set<String> uniqueUsers = new HashSet<>();
                for (FailureAttempt a : ipQueue) {
                    uniqueUsers.add(a.username);
                }

                String alertMsg = String.format("La IP %s ha acumulado %d intentos fallidos en los últimos %d segundos afectando a %d cuenta(s) distinta(s).",
                        ip, ipQueue.size(), windowSeconds, uniqueUsers.size());

                alerts.add(new AnomalyAlert(
                        "IP_BRUTE_FORCE",
                        "🔥 [ALERTA DE SEGURIDAD] Ataque de Fuerza Bruta / Spraying por IP",
                        alertMsg,
                        ipQueue.size(),
                        uniqueUsers.size()
                ));
            }
        }

        // 2. Evaluación Global del Realm (Pico masivo / Detección de anomalías generales)
        Queue<Instant> rQueue = realmFailures.computeIfAbsent(realm, k -> new ConcurrentLinkedQueue<>());
        rQueue.removeIf(ts -> ts.isBefore(cutoff));
        rQueue.add(now);

        if (rQueue.size() >= realmThreshold) {
            Instant lastAlert = realmLastAlertTime.get(realm);
            if (lastAlert == null || lastAlert.isBefore(now.minusSeconds(cooldownSeconds))) {
                realmLastAlertTime.put(realm, now);

                String alertMsg = String.format("Pico masivo de %d intentos fallidos de login detectados en el realm '%s' en los últimos %d segundos.",
                        rQueue.size(), realm, windowSeconds);

                alerts.add(new AnomalyAlert(
                        "REALM_SPIKE",
                        "🔥 [ALERTA DE SEGURIDAD] Pico Global Anómalo de Errores de Login",
                        alertMsg,
                        rQueue.size(),
                        0
                ));
            }
        }

        // Limpieza periódica de IPs inactivas para evitar consumo residual de memoria
        if (ipFailures.size() > 500) {
            ipFailures.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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
