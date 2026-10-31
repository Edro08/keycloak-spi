package org.keycloak.authentication.authenticators.resetcred.master.steps;

import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;

import java.util.LinkedList;
import java.util.List;

public class MasterGenerateToken implements IResetCred {
    private final IConfig config;
    private final ILogger logger;
    private final Catalog_Errors catalogErrors;
    private final IResetCred next;

    public MasterGenerateToken(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
        this.config = config;
        this.logger = logger;
        this.catalogErrors = catalogErrors;
        this.next = next;
    }

    @Override
    public void process(ResetCredContext ctx) {
        if (ctx.getAuthenticationFlowContext() == null || ctx.getError() != null) return;

        KeycloakSession keycloakSession = ctx.getAuthenticationFlowContext().getSession();
        RealmModel masterRealm = ctx.getAuthenticationFlowContext().getRealm();
        if (masterRealm == null || !masterRealm.getName().equalsIgnoreCase("master")) {
            masterRealm = keycloakSession.realms().getRealmByName("master");
        }

        if (masterRealm == null) {
            ctx.setError(catalogErrors.ErrRealmNotMaster);
            return;
        }

        // Get LifeSecondsToken with fallback (default: 86400 seconds = 24 hours)
        String lifeSecondsToken = config.GetString("LifeSecondsToken");
        int lifespan = 86400;
        if (lifeSecondsToken != null && lifeSecondsToken.matches("\\d+")) {
            lifespan = Integer.parseInt(lifeSecondsToken);
        }

        // Client: Use authentication session client if available, or fallback to security-admin-console
        ClientModel client = null;
        if (ctx.getAuthenticationFlowContext().getAuthenticationSession() != null) {
            client = ctx.getAuthenticationFlowContext().getAuthenticationSession().getClient();
        }
        if (client == null) {
            client = masterRealm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        }
        String clientId = client != null ? client.getClientId() : Constants.ADMIN_CONSOLE_CLIENT_ID;

        // RedirectUri: Use configured RedirectUri or default to Keycloak master admin console
        String redirectUri = config.GetString("RedirectUri");
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            redirectUri = keycloakSession.getContext().getUri().getBaseUri().toString() + "admin/master/console/";
        }

        // Action required: UPDATE_PASSWORD
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());

        int expiration = Time.currentTime() + lifespan;
        UserModel user = ctx.getUserModel();
        String userEmail = user.getEmail() != null ? user.getEmail() : "";

        // Generate token
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(
                user.getId(),
                userEmail,
                expiration,
                actions,
                redirectUri,
                clientId
        );

        UriBuilder builder = LoginActionsService.actionTokenProcessor(keycloakSession.getContext().getUri());
        builder.queryParam("key", token.serialize(keycloakSession, masterRealm, keycloakSession.getContext().getUri()));
        String enlace = builder.build(masterRealm.getName()).toString();

        ctx.setEnlace(enlace);
        next.process(ctx);
    }
}
