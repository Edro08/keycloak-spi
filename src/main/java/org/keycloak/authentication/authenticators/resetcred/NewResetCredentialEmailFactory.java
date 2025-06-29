package org.keycloak.authentication.authenticators.resetcred;

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

public class NewResetCredentialEmailFactory implements AuthenticatorFactory {
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
    };

    private final List<ProviderConfigProperty> configProperties;

    public NewResetCredentialEmailFactory(){
        configProperties = new ArrayList<>();

        ProviderConfigProperty propertyURL = new ProviderConfigProperty();
        propertyURL.setName("RedirectUri");
        propertyURL.setLabel("Redirect Uri");
        propertyURL.setHelpText("RedirectUri final step execute action.");
        propertyURL.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(propertyURL);

        ProviderConfigProperty propertyLifeSecondsToken = new ProviderConfigProperty();
        propertyLifeSecondsToken.setName("LifeSecondsToken");
        propertyLifeSecondsToken.setLabel("Life Seconds Token");
        propertyLifeSecondsToken.setDefaultValue("86400");
        propertyLifeSecondsToken.setHelpText("Enter the life seconds Token.");
        propertyLifeSecondsToken.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(propertyLifeSecondsToken);
    }

    @Override
    public String getDisplayType() {
    return "NEW RESET CREDENTIAL EMAIL";
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
        return "NEW RESET CREDENTIAL EMAIL";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new NewResetCredentialEmail();
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
        return ProvidersID.FORM_SEND_EMAIL;
    }
}
