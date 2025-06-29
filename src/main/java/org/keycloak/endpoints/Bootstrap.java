package org.keycloak.endpoints;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.*;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class Bootstrap {

    private final KeycloakSession session;
    protected final RealmModel realmModel;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEventBuilder;

    public Bootstrap(KeycloakSession session, RealmModel model, AdminPermissionEvaluator auth, AdminEventBuilder adminEventBuilder) {
        this.session = session;
        this.auth = auth;
        this.realmModel = model;
        this.adminEventBuilder = adminEventBuilder;
    }

    @Path("/users/{username}/event/save")
    @POST
    @Consumes({"application/json"})
    public Response resetPasswordAdmin(@PathParam("username") String username,
                                       @HeaderParam("clientId") String clientId) {

        if (username == null || username.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity(null).build();
        }

        if (clientId == null || clientId.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).entity(null).build();
        }

        return Response.ok().build();
    }
}
