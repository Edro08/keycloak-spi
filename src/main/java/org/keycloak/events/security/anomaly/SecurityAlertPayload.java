package org.keycloak.events.security.anomaly;

import java.time.Instant;
import java.util.Map;

/**
 * Modelo de datos inmutable para payloads de alerta de incidentes de seguridad.
 */
public class SecurityAlertPayload {

    private final String eventType;
    private final String realm;
    private final String user;
    private final String ipAddress;
    private final String clientId;
    private final String timestamp;
    private final Map<String, String> details;

    public SecurityAlertPayload(String eventType, String realm, String user,
                                String ipAddress, String clientId, Map<String, String> details) {
        this.eventType = eventType;
        this.realm = realm;
        this.user = user;
        this.ipAddress = ipAddress;
        this.clientId = clientId;
        this.timestamp = Instant.now().toString();
        this.details = details;
    }

    public String getEventType() { return eventType; }
    public String getRealm() { return realm; }
    public String getUser() { return user; }
    public String getIpAddress() { return ipAddress; }
    public String getClientId() { return clientId; }
    public String getTimestamp() { return timestamp; }
    public Map<String, String> getDetails() { return details; }
}
