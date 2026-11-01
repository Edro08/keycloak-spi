package org.keycloak.events.metrics.telemetry;

import io.micrometer.core.instrument.Metrics;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Caso de uso dedicado al procesamiento, normalización y registro
 * de métricas dimensionales para eventos de usuario y administrativos vía Micrometer.
 */
public class MetricsTelemetryUseCase {

    private static final String METRIC_USER_EVENTS = "keycloak_user_events_total";
    private static final String METRIC_ADMIN_EVENTS = "keycloak_admin_events_total";

    private final KeycloakSession session;

    public MetricsTelemetryUseCase(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Procesa y registra un evento de usuario (login, error, registro, etc.).
     */
    public void recordUserEvent(Event event) {
        if (event == null) {
            return;
        }

        String realm = resolveRealmName(event.getRealmId());
        String clientId = resolveClientId(event);
        String eventType = event.getType() != null ? event.getType().name() : "UNKNOWN";
        String error = event.getError() != null && !event.getError().isBlank() 
                ? event.getError() 
                : "none";

        Metrics.counter(METRIC_USER_EVENTS,
                "realm", realm,
                "client_id", clientId,
                "type", eventType,
                "error", error
        ).increment();
    }

    /**
     * Procesa y registra una operación administrativa en Keycloak.
     */
    public void recordAdminEvent(AdminEvent adminEvent) {
        if (adminEvent == null) {
            return;
        }

        String realm = resolveRealmName(adminEvent.getRealmId());
        String resourceType = adminEvent.getResourceType() != null 
                ? adminEvent.getResourceType().name() 
                : "unknown";
        String operationType = adminEvent.getOperationType() != null 
                ? adminEvent.getOperationType().name() 
                : "unknown";
        String error = adminEvent.getError() != null && !adminEvent.getError().isBlank() 
                ? adminEvent.getError() 
                : "none";

        Metrics.counter(METRIC_ADMIN_EVENTS,
                "realm", realm,
                "resource_type", resourceType,
                "operation_type", operationType,
                "error", error
        ).increment();
    }

    /**
     * Resuelve el Client ID identificando el origen o recurriendo al contexto de la sesión.
     * Si es un evento del ciclo de vida interno (expiración, eliminación), retorna "system".
     */
    private String resolveClientId(Event event) {
        if (event.getClientId() != null && !event.getClientId().isBlank()) {
            return event.getClientId();
        }
        if (event.getDetails() != null) {
            String fromDetails = event.getDetails().get("client_id");
            if (fromDetails != null && !fromDetails.isBlank()) {
                return fromDetails;
            }
        }
        if (session != null && session.getContext() != null && session.getContext().getClient() != null) {
            String fromContext = session.getContext().getClient().getClientId();
            if (fromContext != null && !fromContext.isBlank()) {
                return fromContext;
            }
        }
        return "system";
    }

    /**
     * Resuelve el nombre legible del Realm desde el contexto o el modelo.
     */
    private String resolveRealmName(String realmId) {
        if (session != null && session.getContext() != null && session.getContext().getRealm() != null) {
            return session.getContext().getRealm().getName();
        }
        if (session != null && realmId != null) {
            try {
                RealmModel realmModel = session.realms().getRealm(realmId);
                if (realmModel != null) {
                    return realmModel.getName();
                }
            } catch (Exception ignored) {
            }
        }
        return realmId != null && !realmId.isBlank() ? realmId : "system";
    }
}
