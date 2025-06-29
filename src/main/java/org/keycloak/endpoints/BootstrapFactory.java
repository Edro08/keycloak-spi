package org.keycloak.endpoints;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import utils.ProvidersID;

public class BootstrapFactory implements AdminRealmResourceProviderFactory {

    @Override
    public AdminRealmResourceProvider create(KeycloakSession keycloakSession) {
        return new BootstrapProvider();
    }

    @Override
    public void init(Config.Scope scope) {}

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return ProvidersID.ENDPOINTS_CUSTOM;
    }
}
