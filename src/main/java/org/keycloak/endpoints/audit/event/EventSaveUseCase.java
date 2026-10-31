package org.keycloak.endpoints.audit.event;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import utils.logger.ILogger;
import utils.logger.Logger;

import java.util.Map;

/**
 * Caso de uso para registrar eventos de auditoría personalizados en Keycloak.
 * Modela la trazabilidad de acciones ejecutadas por un actor (Usuario A)
 * sobre un usuario objetivo (Usuario B).
 */
public class EventSaveUseCase {

    private final KeycloakSession session;
    private final RealmModel realmModel;
    private final ILogger logger;

    public EventSaveUseCase(KeycloakSession session, RealmModel realmModel) {
        this.session = session;
        this.realmModel = realmModel;
        this.logger = new Logger();
    }

    /**
     * Valida, procesa y persiste el evento de auditoría en el historial de eventos del realm.
     *
     * @param request Datos del evento (actor, targetUser, action, detalles adicionales).
     * @param auth    Evaluador de permisos del administrador autenticado.
     * @return EventResponse con el resultado de la operación y el identificador de proceso (UUID).
     */
    public EventResponse execute(EventRequest request, AdminPermissionEvaluator auth) {
        String clientId = resolveClientId(request);
        String actor = resolveActor(request, auth);

        if (request == null || isBlank(request.getTargetUser())) {
            return EventResponse.failure(null, actor, null, "VALIDATION", clientId, "Field 'targetUser' is required in JSON body");
        }

        UserModel targetUser = findTargetUser(request.getTargetUser().trim());
        if (targetUser == null) {
            logger.Warn("AUDIT EVENT FAILURE", "TargetUserNotFound", request.getTargetUser(), "Realm", realmModel.getName());
            return EventResponse.failure(null, actor, request.getTargetUser(), "NOT_FOUND", clientId, "Target user '" + request.getTargetUser() + "' not found in realm " + realmModel.getName());
        }

        EventRecord record = buildEventRecord(actor, targetUser.getUsername(), clientId, request);
        persistAuditEvent(record, targetUser);
        logAuditEvent(record);

        return buildSuccessResponse(record);
    }

    private String resolveClientId(EventRequest request) {
        if (request != null && !isBlank(request.getClientId())) {
            return request.getClientId().trim();
        }
        return "admin-cli";
    }

    private String resolveActor(EventRequest request, AdminPermissionEvaluator auth) {
        if (request != null && !isBlank(request.getActor())) {
            return request.getActor().trim();
        }
        if (auth != null && auth.adminAuth() != null && auth.adminAuth().getUser() != null) {
            return auth.adminAuth().getUser().getUsername();
        }
        return "SYSTEM";
    }

    private UserModel findTargetUser(String usernameOrEmail) {
        UserModel user = session.users().getUserByUsername(realmModel, usernameOrEmail);
        if (user == null) {
            user = session.users().getUserByEmail(realmModel, usernameOrEmail);
        }
        return user;
    }

    private EventRecord buildEventRecord(String actor, String targetUsername, String clientId, EventRequest request) {
        String action = !isBlank(request.getAction()) ? request.getAction().trim() : "CUSTOM_ADMIN_ACTION";

        return new EventRecord(
                actor,
                targetUsername,
                clientId,
                action,
                request.getEventType(),
                request.getStatus(),
                request.getDescription(),
                request.getDetails()
        );
    }

    private void persistAuditEvent(EventRecord record, UserModel targetUser) {
        EventType eventType = resolveEventType(record.getEventType());
        EventBuilder eventBuilder = new EventBuilder(realmModel, session, session.getContext().getConnection());

        eventBuilder.event(eventType)
                .user(targetUser)
                .detail("ProcessID", record.getProcessId())
                .detail("Actor", record.getActor())
                .detail("TargetUser", targetUser.getUsername())
                .detail("TargetUserId", targetUser.getId())
                .detail("Action", record.getAction())
                .detail("ClientID", record.getClientId())
                .detail("Status", record.getStatus())
                .storeImmediately(true);

        if (!isBlank(record.getDescription())) {
            eventBuilder.detail("Description", record.getDescription());
        }

        if (record.getDetails() != null) {
            for (Map.Entry<String, String> entry : record.getDetails().entrySet()) {
                if (!isBlank(entry.getKey()) && entry.getValue() != null) {
                    eventBuilder.detail(entry.getKey(), entry.getValue());
                }
            }
        }

        if (record.isSuccess()) {
            eventBuilder.success();
        } else {
            String errorMsg = !isBlank(record.getDescription()) ? record.getDescription() : "error";
            eventBuilder.error(errorMsg);
        }
    }

    private EventType resolveEventType(String typeName) {
        if (isBlank(typeName)) {
            return EventType.CUSTOM_REQUIRED_ACTION;
        }
        try {
            return EventType.valueOf(typeName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EventType.CUSTOM_REQUIRED_ACTION;
        }
    }

    private void logAuditEvent(EventRecord record) {
        logger.Info("AUDIT EVENT [USER A -> USER B]",
                "Actor", record.getActor(),
                "Action", record.getAction(),
                "Target", record.getTargetUser(),
                "Status", record.getStatus(),
                "ClientID", record.getClientId(),
                "ProcessID", record.getProcessId());
    }

    private EventResponse buildSuccessResponse(EventRecord record) {
        return EventResponse.success(
                record.getProcessId(),
                record.getActor(),
                record.getTargetUser(),
                record.getAction(),
                record.getClientId(),
                "Audit event recorded: User [" + record.getActor() + "] performed [" + record.getAction() + "] on User [" + record.getTargetUser() + "]"
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
