package org.keycloak.authentication.authenticators.resetcred.user;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialEmail;
import org.keycloak.authentication.authenticators.resetcred.user.steps.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import utils.config.Config;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;
import utils.logger.Logger;

public class UserResetCredentialEmail extends ResetCredentialEmail implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserResetCredContext ctx = execute(context);
        if (ctx.getError() == null) {
            context.forkWithSuccessMessage(new FormMessage("200"));
        } else if (ctx.getError().getCode() != 500) {
            context.forkWithSuccessMessage(new FormMessage("200"));
        } else {
            context.forkWithErrorMessage(new FormMessage("500"));
        }
    }

    private static UserResetCredContext execute(AuthenticationFlowContext context) {
        // Create Utils
        IConfig config = new Config(context);
        ILogger logger = new Logger();
        Catalog_Errors catalogErrors = new Catalog_Errors();

        // Create ResetCredContext
        UserResetCredContext ctx = new UserResetCredContext();
        ctx.setAuthenticationFlowContext(context);

        // Process Steps: EventSave -> ValidateUser -> GenerateToken -> SendEmail
        IResetCred sendEmail = new UserSendEmail(config, logger, catalogErrors, null);
        IResetCred generateToken = new UserGenerateToken(config, logger, catalogErrors, sendEmail);
        IResetCred validateUser = new UserValidateUser(config, logger, catalogErrors, generateToken);
        IResetCred eventSave = new UserEventSave(config, logger, catalogErrors, validateUser);

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
