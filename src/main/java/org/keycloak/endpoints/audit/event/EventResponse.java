package org.keycloak.endpoints.audit.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class EventResponse {

    @JsonProperty("processId")
    private String processId;

    @JsonProperty("actor")
    private String actor;           // Usuario A

    @JsonProperty("targetUser")
    private String targetUser;      // Usuario B

    @JsonProperty("action")
    private String action;          // Acción realizada

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private String timestamp;

    public EventResponse() {}

    public EventResponse(String processId, String actor, String targetUser, String action, String clientId, String status, String message) {
        this.processId = processId;
        this.actor = actor;
        this.targetUser = targetUser;
        this.action = action;
        this.clientId = clientId;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
    }

    public static EventResponse success(String processId, String actor, String targetUser, String action, String clientId, String message) {
        return new EventResponse(processId, actor, targetUser, action, clientId, "SUCCESS", message);
    }

    public static EventResponse failure(String processId, String actor, String targetUser, String action, String clientId, String message) {
        return new EventResponse(processId, actor, targetUser, action, clientId, "FAILURE", message);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(this.status);
    }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
