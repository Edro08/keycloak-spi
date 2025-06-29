package org.keycloak.authentication.authenticators.resetcred.steps;

import org.keycloak.models.UserModel;
import utils.errors.Errors;
import org.keycloak.authentication.AuthenticationFlowContext;

public class ResetCredContext {
    private Errors error;
    private UserModel userModel;
    private String enlace;
    private AuthenticationFlowContext authenticationFlowContext;
    private String processID;

    public ResetCredContext() {}

    public ResetCredContext(AuthenticationFlowContext context, UserModel userModel, String enlace, Errors error, String processID) {
        this.userModel = userModel;
        this.error = error;
        this.enlace = enlace;
        this.authenticationFlowContext = context;
        this.processID = processID;
    }

    public Errors getError() {return error;}
    public void setError(Errors errors) {this.error = errors;}

    public UserModel getUserModel() {return userModel;}
    public void setUserModel(UserModel userModel) {this.userModel = userModel;}

    public String getEnlace() {return enlace;}
    public void setEnlace(String enlace) {this.enlace = enlace;}

    public AuthenticationFlowContext getAuthenticationFlowContext() {return authenticationFlowContext;}
    public void setAuthenticationFlowContext(AuthenticationFlowContext authenticationFlowContext) {this.authenticationFlowContext = authenticationFlowContext;}

    public String getProcessID() {return processID;}
    public void setProcessID(String processID) {this.processID = processID;}
}
