package org.keycloak.authentication.requiredactions;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

public class NewUpdatePassword extends UpdatePassword {

    @Deprecated
    public NewUpdatePassword() {
        this(null);
    }

    public NewUpdatePassword(KeycloakSession session) {
        super(session);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new NewUpdatePassword(session);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        UserModel user = context.getUser();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        event.event(EventType.UPDATE_CREDENTIAL);
        event.detail("credential_type", "password");
        EventBuilder deprecatedEvent = event.clone().event(EventType.UPDATE_PASSWORD);
        String passwordNew = (String)formData.getFirst("password-new");
        String passwordConfirm = (String)formData.getFirst("password-confirm");
        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_CREDENTIAL_ERROR).client(authSession.getClient()).user(authSession.getAuthenticatedUser());
        EventBuilder deprecatedErrorEvent = errorEvent.clone().event(EventType.UPDATE_PASSWORD_ERROR);
        if (Validation.isBlank(passwordNew)) {
            Response challenge = context.form().setAttribute("username", authSession.getAuthenticatedUser().getUsername()).addError(new FormMessage("password", "missingPasswordMessage")).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            context.challenge(challenge);
            errorEvent.error("password_missing");
            deprecatedErrorEvent.error("password_missing");
        } else if (!passwordNew.equals(passwordConfirm)) {
            Response challenge = context.form().setAttribute("username", authSession.getAuthenticatedUser().getUsername()).addError(new FormMessage("password-confirm", "notMatchPasswordMessage")).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            context.challenge(challenge);
            errorEvent.error("password_confirm_error");
            deprecatedErrorEvent.error("password_confirm_error");
        } else {
            if ("on".equals(formData.getFirst("logout-sessions"))) {
                AuthenticatorUtil.logoutOtherSessions(context);
            }

            try {
                user.credentialManager().updateCredential(UserCredentialModel.password(passwordNew, false));
                context.success();
                deprecatedEvent.success();
            } catch (ModelException me) {
                errorEvent.detail("reason", me.getMessage()).error("password_rejected");
                deprecatedErrorEvent.detail("reason", me.getMessage()).error("password_rejected");
                Response challenge = context.form().setAttribute("username", authSession.getAuthenticatedUser().getUsername()).setError(me.getMessage(), me.getParameters()).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
                context.challenge(challenge);
            } catch (Exception ape) {
                errorEvent.detail("reason", ape.getMessage()).error("password_rejected");
                deprecatedErrorEvent.detail("reason", ape.getMessage()).error("password_rejected");
                Response challenge = context.form().setAttribute("username", authSession.getAuthenticatedUser().getUsername()).setError(ape.getMessage(), new Object[0]).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
                context.challenge(challenge);
            }
        }
    }
}
