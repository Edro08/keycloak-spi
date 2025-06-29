package org.keycloak.authentication.authenticators.resetcred.steps;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import utils.config.IConfig;
import utils.errors.Catalog_Errors;
import utils.errors.Errors;
import utils.logger.ILogger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class EventSave implements IResetCred {
    private final ILogger logger;
    private final IResetCred next;

    private static final String TITLE_SERVICES = "NEW RESET CREDENTIALS EMAIL";
    private String processID;

    private final String SUCCESS = "SUCCESS";
    private final String FAILURE = "FAILURE";

    public EventSave(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
        this.logger = logger;
        this.next = next;
    }

    @Override
    public void process(ResetCredContext ctx) {
        processID = UUID.randomUUID().toString();
        ctx.setProcessID(processID);
        LocalDateTime begin = LocalDateTime.now(ZoneId.systemDefault());

        // Process next step
        next.process(ctx);

        // Save event and log operation
        if (ctx.getError() != null) {
            eventFailure(ctx.getAuthenticationFlowContext(), ctx.getError());
            logOperation(ctx.getAuthenticationFlowContext(), begin, FAILURE);
        }else {
            eventSuccess(ctx.getAuthenticationFlowContext());
            logOperation(ctx.getAuthenticationFlowContext(), begin, SUCCESS);
        }
    }

    // logOperation: function that logs the operation
    private void logOperation(AuthenticationFlowContext context, LocalDateTime begin, String status) {
        LocalDateTime endTime = LocalDateTime.now(ZoneId.systemDefault());
        logger.Info(TITLE_SERVICES,
                "Username", context.getHttpRequest().getDecodedFormParameters().getFirst("username"),
                "Status", status,
                "Begin", begin.toString(),
                "End", endTime.toString(),
                "Took", Duration.between(begin, endTime).toString(),
                "ProcessID", processID);
    }

    private void eventSuccess(AuthenticationFlowContext context) {
        EventBuilder event = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        event.event(EventType.SEND_RESET_PASSWORD)
                .user(context.getUser())
                .detail("ProcessID", processID)
                .detail("Username", context.getHttpRequest().getDecodedFormParameters().getFirst("username"))
                .detail("ClientID", context.getAuthenticationSession().getClient().getClientId())
                .detail("Status", SUCCESS)
                .detail("Flow", TITLE_SERVICES)
                .storeImmediately(true)
                .success();
    }

    private void eventFailure(AuthenticationFlowContext context, Errors err) {
        EventBuilder event = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        event.event(EventType.SEND_RESET_PASSWORD_ERROR)
                .user(context.getUser())
                .detail("ProcessID", processID)
                .detail("Username", context.getHttpRequest().getDecodedFormParameters().getFirst("username"))
                .detail("ClientID", context.getAuthenticationSession().getClient().getClientId())
                .detail("Status", FAILURE)
                .detail("Flow", TITLE_SERVICES)
                .detail("Error Code", String.valueOf(err.getCode()))
                .detail("Error Description", String.valueOf(err.getDescription()))
                .storeImmediately(true)
                .error(err.getDescription());
    }
}
