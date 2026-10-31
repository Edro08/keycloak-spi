package org.keycloak.events.security;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.security.anomaly.SecurityAnomalyDetector;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import utils.ProvidersID;

/**
 * Fábrica para el SPI de eventos y detección de anomalías de seguridad.
 * Inicializa la configuración de umbrales del entorno y el detector de anomalías compartido.
 */
public class SecurityEventListenerFactory implements EventListenerProviderFactory {

    private SecurityAnomalyDetector anomalyDetector;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new SecurityEventListener(session, anomalyDetector);
    }

    @Override
    public void init(Config.Scope config) {
        this.anomalyDetector = SecurityAnomalyDetector.fromEnvironment();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ProvidersID.EVENT_LISTENER_SECURITY;
    }
}
