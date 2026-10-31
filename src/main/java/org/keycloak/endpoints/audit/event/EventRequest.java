package org.keycloak.endpoints.audit.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventRequest {

    /**
     * Usuario B: Sobre quien se realiza la acción (Objetivo).
     */
    @JsonProperty("targetUser")
    private String targetUser;

    /**
     * Usuario A: Quien realiza la acción (Actor).
     * Si no se envía en el JSON, se extrae automáticamente del token de administración.
     */
    @JsonProperty("actor")
    private String actor;

    /**
     * ClientId del sistema que envía o donde ocurre el evento.
     */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * Qué hizo el Usuario A (ej: RESET_PASSWORD, ROLE_ASSIGNED, USER_BLOCKED, etc.)
     */
    @JsonProperty("action")
    private String action;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("description")
    private String description;

    @JsonProperty("details")
    private Map<String, String> details = new HashMap<>();

    public EventRequest() {}

    public EventRequest(String targetUser, String actor, String clientId, String action, String eventType, String status, String description, Map<String, String> details) {
        this.targetUser = targetUser;
        this.actor = actor;
        this.clientId = clientId;
        this.action = action;
        this.eventType = eventType;
        this.status = status;
        this.description = description;
        this.details = details != null ? details : new HashMap<>();
    }

    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details != null ? details : new HashMap<>(); }
}
