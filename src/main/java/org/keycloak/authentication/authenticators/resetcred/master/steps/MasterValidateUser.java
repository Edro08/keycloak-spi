package org.keycloak.authentication.authenticators.resetcred.master.steps;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;

public class MasterValidateUser implements IResetCred {
    private final Catalog_Errors catalogErrors;
    private final ILogger logger;
    private final IResetCred next;

    private static final int MillisSleep = 3000;

    public MasterValidateUser(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
        this.catalogErrors = catalogErrors;
        this.logger = logger;
        this.next = next;
    }

    @Override
    public void process(ResetCredContext ctx) {
        if (ctx.getAuthenticationFlowContext() == null || ctx.getError() != null) return;

        RealmModel currentRealm = ctx.getAuthenticationFlowContext().getRealm();
        if (currentRealm == null || !currentRealm.getName().equalsIgnoreCase("master")) {
            logger.Warn("MASTER RESET CREDENTIALS", "Warning", "Authentication flow executed outside master realm: " + (currentRealm != null ? currentRealm.getName() : "null"));
            ctx.setError(catalogErrors.ErrRealmNotMaster);
            return;
        }

        UserModel user = ctx.getAuthenticationFlowContext().getUser();
        ctx.setUserModel(user);

        if (user == null) {
            sleep();
            ctx.setError(catalogErrors.ErrUserNotFound);
            return;
        }

        // Note: In Master realm, the root administrator might not have an email set.
        // We log a notice if email is empty, but we allow token generation to proceed!
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.Warn("MASTER RESET CREDENTIALS", "Notice", "User " + user.getUsername() + " has no email configured. Token will still be generated for logging.");
        }

        next.process(ctx);
    }

    private void sleep() {
        try {
            Thread.sleep(MillisSleep);
        } catch (InterruptedException ignored) {}
    }
}
