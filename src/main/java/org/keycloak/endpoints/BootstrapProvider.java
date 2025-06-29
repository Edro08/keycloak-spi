package org.keycloak.endpoints;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class BootstrapProvider implements AdminRealmResourceProvider {
    @Override
    public Object getResource(KeycloakSession keycloakSession, RealmModel realmModel, AdminPermissionEvaluator adminPermissionEvaluator, AdminEventBuilder adminEventBuilder) {
        return new Bootstrap(keycloakSession,realmModel,adminPermissionEvaluator,adminEventBuilder);
    }

    @Override
    public void close() {}
}
