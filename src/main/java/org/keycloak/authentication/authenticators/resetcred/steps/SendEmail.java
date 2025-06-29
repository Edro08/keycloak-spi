package org.keycloak.authentication.authenticators.resetcred.steps;

import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.logger.ILogger;

public class SendEmail implements IResetCred {
    private final ILogger logger;
    private final IConfig config;
    private final Catalog_Errors catalogErrors;
    private final IResetCred next;

    private static final String TITLE_SERVICES = "NEW RESET CREDENTIALS EMAIL";

    public SendEmail(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
        this.config = config;
        this.logger = logger;
        this.catalogErrors = catalogErrors;
        this.next = next;
    }

    @Override
    public void process(ResetCredContext ctx) {
        logger.Info(TITLE_SERVICES,"enlace",ctx.getEnlace(),"ProcessID", ctx.getProcessID());
    }
}
