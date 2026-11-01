package org.keycloak.events.metrics;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import utils.ProvidersID;

/**
 * Fábrica para el SPI de métricas y telemetría de eventos.
 */
public class MetricsEventListenerFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MetricsEventListener(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ProvidersID.EVENT_LISTENER_METRICS;
    }
}
