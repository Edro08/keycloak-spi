package org.keycloak.authentication.authenticators.resetcred;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.resetcred.steps.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import utils.config.Config;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;
import utils.logger.Logger;

public class NewResetCredentialEmail extends ResetCredentialEmail implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        ResetCredContext ctx = execute(context);
        if (ctx.getError() == null) {
            context.forkWithSuccessMessage(new FormMessage("200"));
        } else if (ctx.getError().getCode() != 500) {
            context.forkWithSuccessMessage(new FormMessage("200"));
        } else {
            context.forkWithErrorMessage(new FormMessage("500"));
        }
    }

    private static ResetCredContext execute(AuthenticationFlowContext context) {
        // Create Utils
        IConfig config = new Config(context);
        ILogger logger = new Logger();
        Catalog_Errors catalogErrors = new Catalog_Errors();

        // Create ResetCredContext
        ResetCredContext ctx = new ResetCredContext();
        ctx.setAuthenticationFlowContext(context);

        // Process Steps
        IResetCred sendEmail = new SendEmail(config, logger, catalogErrors, null);
        IResetCred generateToken = new GenerateToken(config, logger, catalogErrors, sendEmail);
        IResetCred validateUser = new ValidateUser(config, logger, catalogErrors, generateToken);
        IResetCred eventSave = new EventSave(config, logger, catalogErrors, validateUser);

        // Start
        eventSave.process(ctx);
        return ctx;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
        authenticationFlowContext.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
    }

    @Override
    public void close() {
    }
}
