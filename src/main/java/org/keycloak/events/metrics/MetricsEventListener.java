package org.keycloak.events.metrics;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.metrics.telemetry.MetricsTelemetryUseCase;
import org.keycloak.models.KeycloakSession;

/**
 * Proveedor SPI de escucha de eventos para métricas y observabilidad en Keycloak.
 * Desacoplado de la lógica de procesamiento; delega los eventos
 * al caso de uso {@link MetricsTelemetryUseCase}.
 */
public class MetricsEventListener implements EventListenerProvider {

    private final MetricsTelemetryUseCase telemetryUseCase;

    public MetricsEventListener(KeycloakSession session) {
        this.telemetryUseCase = new MetricsTelemetryUseCase(session);
    }

    @Override
    public void onEvent(Event event) {
        try {
            telemetryUseCase.recordUserEvent(event);
        } catch (Throwable t) {
            // Un listener de telemetría jamás debe interrumpir el flujo del usuario
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        try {
            telemetryUseCase.recordAdminEvent(adminEvent);
        } catch (Throwable t) {
            // Silencioso ante errores en operaciones administrativas
        }
    }

    @Override
    public void close() {
        // No-op
    }
}
