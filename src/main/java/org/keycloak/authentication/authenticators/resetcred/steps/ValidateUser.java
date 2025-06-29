package org.keycloak.authentication.authenticators.resetcred.steps;

import org.keycloak.models.UserModel;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;

public class ValidateUser implements IResetCred {
    private final Catalog_Errors catalogErrors;
    private final IResetCred next;

    private static final int MillisSleep = 10000;

    public ValidateUser(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
        this.catalogErrors = catalogErrors;
        this.next = next;
    }

    @Override
    public void process(ResetCredContext ctx){
        // Validate ctx
        if (ctx.getAuthenticationFlowContext() == null || ctx.getError() != null) return;

        // Get UserModel and set in ResetPasswordContext
        UserModel user = ctx.getAuthenticationFlowContext().getUser();
        ctx.setUserModel(user);

        if (user == null) {
            sleep();
            ctx.setError(catalogErrors.ErrUserNotFound);
        } else if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            sleep();
            ctx.setError(catalogErrors.ErrUserInvalidEmail);
        }

        next.process(ctx);
    }

    private void sleep() {
        try {
            Thread.sleep(MillisSleep);
        } catch (InterruptedException ignored) {}
    }
}
