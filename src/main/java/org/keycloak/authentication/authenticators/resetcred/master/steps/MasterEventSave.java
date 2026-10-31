package org.keycloak.authentication.authenticators.resetcred.master.steps;

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

public class MasterEventSave implements IResetCred {
    private final ILogger logger;
    private final IResetCred next;

    private static final String TITLE_SERVICES = "MASTER RESET CREDENTIALS EMAIL";
    private String processID;

    private final String SUCCESS = "SUCCESS";
    private final String FAILURE = "FAILURE";

    public MasterEventSave(IConfig config, ILogger logger, Catalog_Errors catalogErrors, IResetCred next) {
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
        } else {
            eventSuccess(ctx.getAuthenticationFlowContext());
            logOperation(ctx.getAuthenticationFlowContext(), begin, SUCCESS);
        }
    }

    private void logOperation(AuthenticationFlowContext context, LocalDateTime begin, String status) {
        LocalDateTime endTime = LocalDateTime.now(ZoneId.systemDefault());
        String username = context.getUser() != null ? context.getUser().getUsername() :
                context.getHttpRequest().getDecodedFormParameters().getFirst("username");
        logger.Info(TITLE_SERVICES,
                "Realm", context.getRealm().getName(),
                "Username", username,
                "Status", status,
                "Begin", begin.toString(),
                "End", endTime.toString(),
                "Took", Duration.between(begin, endTime).toString(),
                "ProcessID", processID);
    }

    private void eventSuccess(AuthenticationFlowContext context) {
        String username = context.getUser() != null ? context.getUser().getUsername() :
                context.getHttpRequest().getDecodedFormParameters().getFirst("username");
        String clientId = context.getAuthenticationSession() != null && context.getAuthenticationSession().getClient() != null
                ? context.getAuthenticationSession().getClient().getClientId()
                : "security-admin-console";

        EventBuilder event = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        event.event(EventType.SEND_RESET_PASSWORD)
                .user(context.getUser())
                .detail("ProcessID", processID)
                .detail("Realm", context.getRealm().getName())
                .detail("Username", username)
                .detail("ClientID", clientId)
                .detail("Status", SUCCESS)
                .detail("Flow", TITLE_SERVICES)
                .storeImmediately(true)
                .success();
    }

    private void eventFailure(AuthenticationFlowContext context, Errors err) {
        String username = context.getUser() != null ? context.getUser().getUsername() :
                context.getHttpRequest().getDecodedFormParameters().getFirst("username");
        String clientId = context.getAuthenticationSession() != null && context.getAuthenticationSession().getClient() != null
                ? context.getAuthenticationSession().getClient().getClientId()
                : "security-admin-console";

        EventBuilder event = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        event.event(EventType.SEND_RESET_PASSWORD_ERROR)
                .user(context.getUser())
                .detail("ProcessID", processID)
                .detail("Realm", context.getRealm().getName())
                .detail("Username", username)
                .detail("ClientID", clientId)
                .detail("Status", FAILURE)
                .detail("Flow", TITLE_SERVICES)
                .detail("Error Code", String.valueOf(err.getCode()))
                .detail("Error Description", String.valueOf(err.getDescription()))
                .storeImmediately(true)
                .error(err.getDescription());
    }
}
