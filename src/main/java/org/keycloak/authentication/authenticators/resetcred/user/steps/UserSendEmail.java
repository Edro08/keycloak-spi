package org.keycloak.authentication.authenticators.resetcred.user.steps;

import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;

public class UserSendEmail implements IResetCred {
    private final ILogger logger;
    private final IConfig config;
    private final Catalog_Errors catalogErrors;
    private final IResetCred next;

    public UserSendEmail(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
        this.config = config;
        this.logger = logger;
        this.catalogErrors = catalogErrors;
        this.next = next;
    }

    @Override
    public void process(UserResetCredContext ctx) {
        String username = ctx.getUserModel() != null ? ctx.getUserModel().getUsername() : "unknown";
        String enlace = ctx.getEnlace();

        // Prominent banner in server stdout
        System.out.println("=========================================================================================");
        System.out.println(">>> [KEYCLOAK SPI - USER REALM PASSWORD RESET]");
        System.out.println(">>> User      : " + username);
        System.out.println(">>> ProcessID : " + ctx.getProcessID());
        System.out.println(">>> RESET URL : " + enlace);
        System.out.println("=========================================================================================");

        if (next != null) {
            next.process(ctx);
        }
    }
}
