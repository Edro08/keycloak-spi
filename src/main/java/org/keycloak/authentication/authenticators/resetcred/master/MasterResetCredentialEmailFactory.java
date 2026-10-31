package org.keycloak.authentication.authenticators.resetcred.master;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import utils.ProvidersID;

import java.util.ArrayList;
import java.util.List;

public class MasterResetCredentialEmailFactory implements AuthenticatorFactory {
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    private final List<ProviderConfigProperty> configProperties;

    public MasterResetCredentialEmailFactory() {
        configProperties = new ArrayList<>();

        ProviderConfigProperty propertyURL = new ProviderConfigProperty();
        propertyURL.setName("RedirectUri");
        propertyURL.setLabel("Redirect Uri");
        propertyURL.setDefaultValue("");
        propertyURL.setHelpText("RedirectUri after updating password (e.g. /admin/master/console/). Defaults to Keycloak Admin Console.");
        propertyURL.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(propertyURL);

        ProviderConfigProperty propertyLifeSecondsToken = new ProviderConfigProperty();
        propertyLifeSecondsToken.setName("LifeSecondsToken");
        propertyLifeSecondsToken.setLabel("Life Seconds Token");
        propertyLifeSecondsToken.setDefaultValue("86400");
        propertyLifeSecondsToken.setHelpText("Token validity duration in seconds (default: 86400 = 24h).");
        propertyLifeSecondsToken.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(propertyLifeSecondsToken);
    }

    @Override
    public String getDisplayType() {
        return "NEW MASTER RESET CREDENTIAL EMAIL";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Generates a password reset token for the Master realm administrator and logs the reset URL to the server console instead of sending an email.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new MasterResetCredentialEmail();
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ProvidersID.FORM_SEND_EMAIL_MASTER;
    }
}
