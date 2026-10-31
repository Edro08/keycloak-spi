package org.keycloak.endpoints.audit.event;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class EventRecord {
    private final String processId;
    private final String actor;
    private final String targetUser;
    private final String clientId;
    private final String action;
    private final String eventType;
    private final String status;
    private final String description;
    private final Map<String, String> details;

    public EventRecord(String actor, String targetUser, String clientId, String action, String eventType, String status, String description, Map<String, String> details) {
        this.processId = UUID.randomUUID().toString();
        this.actor = actor != null && !actor.trim().isEmpty() ? actor.trim() : "SYSTEM";
        this.targetUser = targetUser;
        this.clientId = clientId != null && !clientId.trim().isEmpty() ? clientId.trim() : "admin-cli";
        this.action = action != null && !action.trim().isEmpty() ? action.trim().toUpperCase() : "ADMIN_ACTION";
        this.eventType = eventType != null && !eventType.trim().isEmpty() ? eventType.trim().toUpperCase() : "CUSTOM_REQUIRED_ACTION";
        this.status = status != null && status.equalsIgnoreCase("FAILURE") ? "FAILURE" : "SUCCESS";
        this.description = description != null ? description : "";
        this.details = details != null ? details : Collections.emptyMap();
    }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(this.status);
    }

    public String getProcessId() { return processId; }
    public String getActor() { return actor; }
    public String getTargetUser() { return targetUser; }
    public String getClientId() { return clientId; }
    public String getAction() { return action; }
    public String getEventType() { return eventType; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public Map<String, String> getDetails() { return details; }
}
