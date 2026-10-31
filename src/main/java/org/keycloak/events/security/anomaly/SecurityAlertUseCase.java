package org.keycloak.events.security.anomaly;

import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso principal para el procesamiento de fallos de autenticación
 * y emisión de alertas de anomalías de seguridad respaldadas por Infinispan.
 */
public class SecurityAlertUseCase {

    private final KeycloakSession session;
    private final SecurityAnomalyDetector anomalyDetector;

    public SecurityAlertUseCase(KeycloakSession session, SecurityAnomalyDetector anomalyDetector) {
        this.session = session;
        this.anomalyDetector = anomalyDetector;
    }

    /**
     * Procesa un fallo de inicio de sesión (LOGIN_ERROR).
     * Si los intentos superan los umbrales configurados en la caché de Infinispan, despacha las alertas.
     */
    public void handleLoginFailure(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String username = resolveUsername(event);

        List<SecurityAnomalyDetector.AnomalyAlert> alerts = anomalyDetector.registerLoginFailure(
                session,
                event.getIpAddress(),
                username,
                realmName
        );

        for (SecurityAnomalyDetector.AnomalyAlert alert : alerts) {
            dispatchAlert(alert, event, realmName, username);
        }
    }

    private void dispatchAlert(SecurityAnomalyDetector.AnomalyAlert alert, Event event, String realmName, String username) {
        Map<String, String> alertDetails = new HashMap<>();
        alertDetails.put("alertType", alert.getType());
        alertDetails.put("totalFailures", String.valueOf(alert.getTotalFailures()));
        alertDetails.put("distinctUsersTargeted", String.valueOf(alert.getDistinctUsers()));
        alertDetails.put("ipAddress", event.getIpAddress());
        alertDetails.put("details", alert.getMessage());

        SecurityAlertPayload alertPayload = new SecurityAlertPayload(
                alert.getType(),
                realmName,
                username,
                event.getIpAddress(),
                event.getClientId(),
                alertDetails
        );

        SecurityAlertSender.send(alertPayload);
    }

    private String resolveRealmName(String realmId) {
        if (realmId == null) return "UNKNOWN";
        RealmModel realm = session.realms().getRealm(realmId);
        return realm != null ? realm.getName() : realmId;
    }

    private String resolveUsername(Event event) {
        if (event.getDetails() != null && event.getDetails().containsKey("username")) {
            String u = event.getDetails().get("username");
            if (u != null && !u.trim().isEmpty()) {
                return u.trim();
            }
        }

        if (event.getUserId() != null) {
            RealmModel realm = session.getContext().getRealm();
            if (realm != null) {
                UserModel user = session.users().getUserById(realm, event.getUserId());
                if (user != null) return user.getUsername();
            }
            return event.getUserId();
        }

        return "ANONYMOUS";
    }
}
