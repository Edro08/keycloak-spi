package org.keycloak.authentication.authenticators.resetcred.steps;

import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;

import java.util.LinkedList;
import java.util.List;

public class GenerateToken implements IResetCred {
    private final IConfig config;
    private final Catalog_Errors catalogErrors;
    private final IResetCred next;

    public GenerateToken(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next){
        this.config = config;
        this.catalogErrors = catalogErrors;
        this.next = next;
    }

    @Override
    public void process(ResetCredContext ctx){
        // Validate ctx
        if (ctx.getAuthenticationFlowContext() == null || ctx.getError() != null) return;

        // Get LifeSecondsToken and validate
        String LifeSecondsToken = config.GetString("LifeSecondsToken");
        if (LifeSecondsToken == null || !LifeSecondsToken.matches("\\d+")) {
            ctx.setError(catalogErrors.ErrLifeSpan);
            return;
        }

        // Get RedirectUri
        String redirectUri = config.GetString("RedirectUri");
        if (redirectUri == null) {
            ctx.setError(catalogErrors.ErrRedirectUri);
            return;
        }

        // Add required action to the list for the token
        String requiredAction = UserModel.RequiredAction.UPDATE_PASSWORD.name();
        List<String> actions = new LinkedList<>();
        actions.add(requiredAction);

        // Create expiration time for the token
        int expiration = Time.currentTime() + Integer.parseInt(LifeSecondsToken);

        // Gets the client and realm models and the session
        ClientModel client = ctx.getAuthenticationFlowContext().getAuthenticationSession().getClient();
        RealmModel realmModel = ctx.getAuthenticationFlowContext().getRealm();
        KeycloakSession keycloakSession = ctx.getAuthenticationFlowContext().getSession();

        // Reset password token creation
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(ctx.getUserModel().getId(), ctx.getUserModel().getEmail(), expiration, actions, redirectUri, client.getClientId());
        UriBuilder builder = LoginActionsService.actionTokenProcessor(keycloakSession.getContext().getUri());
        builder.queryParam("key", token.serialize(keycloakSession, realmModel, keycloakSession.getContext().getUri()));
        String enlace = builder.build(realmModel.getName()).toString();

        // Set to enlace
        ctx.setEnlace(enlace);
        next.process(ctx);
    }
}
