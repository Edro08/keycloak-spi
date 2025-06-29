package utils.config;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;

public class Config implements IConfig {
    private AuthenticatorConfigModel configModel = null;

    public Config(AuthenticationFlowContext context) {
        if (context != null) {
            configModel = context.getAuthenticatorConfig();
        }
    }

    public String GetString(String value) {
        if (configModel != null && value != null) return configModel.getConfig().get(value);
        return null;
    }
}
