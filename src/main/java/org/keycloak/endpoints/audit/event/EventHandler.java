package org.keycloak.endpoints.audit.event;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * Recurso REST JAX-RS para registrar eventos de auditoría administrativa.
 * Montado por Keycloak bajo: /admin/realms/{realm}/audit/events
 */
public class EventHandler {

    private final RealmModel realmModel;
    private final AdminPermissionEvaluator auth;
    private final EventSaveUseCase saveUserEventUseCase;

    public EventHandler(KeycloakSession session, RealmModel realmModel, AdminPermissionEvaluator auth, AdminEventBuilder adminEventBuilder) {
        this.realmModel = realmModel;
        this.auth = auth;
        this.saveUserEventUseCase = new EventSaveUseCase(session, realmModel);
    }

    /**
     * Registra un evento de auditoría donde un Actor (Usuario A) realiza una acción sobre un Target (Usuario B).
     *
     * Requiere permisos de administración de usuarios en el realm (manage-users).
     *
     * @param request Payload JSON con targetUser, actor (opcional), clientId, action y detalles.
     * @return 200 OK con EventResponse si el evento fue persistido.
     *         400 Bad Request si faltan campos obligatorios.
     *         404 Not Found si el targetUser no existe en el realm.
     *         403 Forbidden si el token no posee permisos de administración.
     */
    @Path("/events")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveAuditEvent(EventRequest request) {
        if (auth != null) {
            auth.users().requireManage();
        }

        EventResponse response = saveUserEventUseCase.execute(request, auth);

        if (response.isSuccess()) {
            return Response.ok(response).build();
        } else if ("NOT_FOUND".equalsIgnoreCase(response.getAction())) {
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }

    /**
     * Endpoint de verificación de disponibilidad del SPI.
     */
    @Path("/health")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response health() {
        return Response.ok("{\"status\": \"UP\", \"realm\": \"" + realmModel.getName() + "\"}").build();
    }
}
