package org.keycloak.events.security;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.security.anomaly.SecurityAlertUseCase;
import org.keycloak.events.security.anomaly.SecurityAnomalyDetector;
import org.keycloak.models.KeycloakSession;

/**
 * Proveedor SPI de escucha de eventos en Keycloak.
 * Desacoplado de la lógica de negocio; delega los fallos de autenticación
 * al caso de uso {@link SecurityAlertUseCase}.
 */
public class SecurityEventListener implements EventListenerProvider {

    private final SecurityAlertUseCase securityAlertUseCase;

    public SecurityEventListener(KeycloakSession session, SecurityAnomalyDetector anomalyDetector) {
        this.securityAlertUseCase = new SecurityAlertUseCase(session, anomalyDetector);
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.LOGIN_ERROR) {
            securityAlertUseCase.handleLoginFailure(event);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // No-op: este SPI no notifica eventos administrativos individuales
    }

    @Override
    public void close() {
    }
}
