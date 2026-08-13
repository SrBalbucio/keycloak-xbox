package balbucio.keycloak.xbox;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class XboxIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public static final String DEFAULT_AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    public static final String DEFAULT_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    public static final String DEFAULT_SCOPE = "xboxlive.signin offline_access User.Read";

    public static final String DEFAULT_XBOX_USER_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String DEFAULT_XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    public static final String DEFAULT_GRAPH_ME_URL = "https://graph.microsoft.com/v1.0/me";

    public static final String DEFAULT_USER_AUTH_RELYING_PARTY = "http://auth.xboxlive.com";
    public static final String DEFAULT_XSTS_RELYING_PARTY = "http://xboxlive.com";
    public static final String DEFAULT_SANDBOX_ID = "RETAIL";

    public static final String XBOX_USER_AUTH_URL_KEY = "xboxUserAuthUrl";
    public static final String XSTS_AUTH_URL_KEY = "xstsAuthUrl";
    public static final String GRAPH_ME_URL_KEY = "graphMeUrl";
    public static final String SANDBOX_ID_KEY = "sandboxId";
    public static final String XSTS_RELYING_PARTY_KEY = "xstsRelyingParty";
    public static final String FETCH_EMAIL_KEY = "fetchEmail";

    public XboxIdentityProviderConfig() {
        setAuthorizationUrl(DEFAULT_AUTH_URL);
        setTokenUrl(DEFAULT_TOKEN_URL);
        setDefaultScope(DEFAULT_SCOPE);
        setStoreToken(true);
    }

    public XboxIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getXboxUserAuthUrl() {
        return getConfig().getOrDefault(XBOX_USER_AUTH_URL_KEY, DEFAULT_XBOX_USER_AUTH_URL);
    }

    public void setXboxUserAuthUrl(String url) {
        getConfig().put(XBOX_USER_AUTH_URL_KEY, url);
    }

    public String getXstsAuthUrl() {
        return getConfig().getOrDefault(XSTS_AUTH_URL_KEY, DEFAULT_XSTS_AUTH_URL);
    }

    public void setXstsAuthUrl(String url) {
        getConfig().put(XSTS_AUTH_URL_KEY, url);
    }

    public String getGraphMeUrl() {
        return getConfig().getOrDefault(GRAPH_ME_URL_KEY, DEFAULT_GRAPH_ME_URL);
    }

    public void setGraphMeUrl(String url) {
        getConfig().put(GRAPH_ME_URL_KEY, url);
    }

    public String getSandboxId() {
        return getConfig().getOrDefault(SANDBOX_ID_KEY, DEFAULT_SANDBOX_ID);
    }

    public void setSandboxId(String sandboxId) {
        getConfig().put(SANDBOX_ID_KEY, sandboxId);
    }

    public String getXstsRelyingParty() {
        return getConfig().getOrDefault(XSTS_RELYING_PARTY_KEY, DEFAULT_XSTS_RELYING_PARTY);
    }

    public void setXstsRelyingParty(String relyingParty) {
        getConfig().put(XSTS_RELYING_PARTY_KEY, relyingParty);
    }

    public boolean isFetchEmail() {
        return Boolean.parseBoolean(getConfig().getOrDefault(FETCH_EMAIL_KEY, "true"));
    }

    public void setFetchEmail(boolean fetchEmail) {
        getConfig().put(FETCH_EMAIL_KEY, String.valueOf(fetchEmail));
    }
}
