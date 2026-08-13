package balbucio.keycloak.xbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.jbosslog.JBossLog;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

@JBossLog
public class XboxIdentityProvider extends AbstractOAuth2IdentityProvider<XboxIdentityProviderConfig>
        implements SocialIdentityProvider<XboxIdentityProviderConfig> {

    private static final ObjectMapper JSON = new ObjectMapper();

    public XboxIdentityProvider(KeycloakSession session, XboxIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    protected String getDefaultScopes() {
        return XboxIdentityProviderConfig.DEFAULT_SCOPE;
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        BrokeredIdentityContext context = super.getFederatedIdentity(response);
        if (getConfig().isFetchEmail()) {
            String email = fetchEmailFromGraph(response);
            if (!isBlank(email)) {
                context.setEmail(email);
            }
        }
        return context;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String msaAccessToken) {
        XboxAuthResponse userAuth = postXboxAuth(
                getConfig().getXboxUserAuthUrl(), buildUserAuthBody(msaAccessToken));
        String userToken = requireToken(userAuth, "Xbox User Token");

        XboxAuthResponse xsts = postXboxAuth(
                getConfig().getXstsAuthUrl(), buildXstsBody(userToken));
        requireToken(xsts, "XSTS");
        XboxAuthResponse.UserClaim claim = firstUserClaim(xsts);

        String xuid = claim != null ? claim.getXid() : null;
        String gamertag = claim != null ? claim.getGtg() : null;

        if (isBlank(xuid)) {
            throw new IdentityBrokerException("A resposta do XSTS do Xbox nao inclui o identificador do usuario (xid).");
        }

        BrokeredIdentityContext context = new BrokeredIdentityContext(xuid, getConfig());
        context.setBrokerUserId(xuid);
        context.setUsername(!isBlank(gamertag) ? gamertag : xuid);
        context.setModelUsername(context.getUsername());
        context.setIdp(this);

        context.setUserAttribute("xbox.xuid", xuid);
        context.setUserAttribute("xbox.gamertag", nullToEmpty(gamertag));
        if (claim != null && !isBlank(claim.getUhs())) {
            context.setUserAttribute("xbox.uhs", claim.getUhs());
        }

        return context;
    }

    private XboxAuthResponse postXboxAuth(String url, String jsonBody) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .maxBodySize(0)
                    .method(Connection.Method.POST)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("x-xbl-contract-version", "1")
                    .requestBody(jsonBody)
                    .execute();
            String body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IdentityBrokerException("Endpoint Xbox " + url
                        + " retornou HTTP " + response.statusCode() + ": " + body);
            }
            return JsonSerialization.readValue(body, XboxAuthResponse.class);
        } catch (IOException e) {
            throw new IdentityBrokerException("Falha ao chamar o endpoint Xbox " + url, e);
        }
    }

    private String buildUserAuthBody(String msaAccessToken) {
        try {
            ObjectNode properties = JSON.createObjectNode();
            properties.put("AuthMethod", "RPS");
            properties.put("SiteName", "user.auth.xboxlive.com");
            properties.put("RpsTicket", "d=" + msaAccessToken);

            ObjectNode root = JSON.createObjectNode();
            root.put("RelyingParty", XboxIdentityProviderConfig.DEFAULT_USER_AUTH_RELYING_PARTY);
            root.put("TokenType", "JWT");
            root.set("Properties", properties);
            return JSON.writeValueAsString(root);
        } catch (IOException e) {
            throw new IdentityBrokerException("Falha ao montar o corpo da requisicao de autenticacao do Xbox.", e);
        }
    }

    private String buildXstsBody(String userToken) {
        try {
            ObjectNode properties = JSON.createObjectNode();
            properties.put("SandboxId", getConfig().getSandboxId());
            ArrayNode userTokens = JSON.createArrayNode();
            userTokens.add(userToken);
            properties.set("UserTokens", userTokens);

            ObjectNode root = JSON.createObjectNode();
            root.put("RelyingParty", getConfig().getXstsRelyingParty());
            root.put("TokenType", "JWT");
            root.set("Properties", properties);
            return JSON.writeValueAsString(root);
        } catch (IOException e) {
            throw new IdentityBrokerException("Falha ao montar o corpo da requisicao XSTS.", e);
        }
    }

    private String fetchEmailFromGraph(String tokenResponse) {
        String refreshToken = extractField(tokenResponse, "refresh_token");
        if (isBlank(refreshToken)) {
            return null;
        }
        try {
            Connection.Response tokenResp = Jsoup.connect(getConfig().getTokenUrl())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .maxBodySize(0)
                    .method(Connection.Method.POST)
                    .data("grant_type", "refresh_token")
                    .data("refresh_token", refreshToken)
                    .data("client_id", getConfig().getClientId())
                    .data("client_secret", getConfig().getClientSecret())
                    .data("scope", "User.Read")
                    .execute();
            String tokenBody = tokenResp.body();
            if (tokenResp.statusCode() < 200 || tokenResp.statusCode() >= 300) {
                log.debugf("Falha no refresh do token do Graph (%d): %s", tokenResp.statusCode(), tokenBody);
                return null;
            }
            String graphToken = extractField(tokenBody, "access_token");
            if (isBlank(graphToken)) {
                return null;
            }

            Connection.Response meResp = Jsoup.connect(getConfig().getGraphMeUrl()
                            + "?$select=mail,userPrincipalName,displayName")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .maxBodySize(0)
                    .header("Authorization", "Bearer " + graphToken)
                    .header("Accept", "application/json")
                    .execute();
            String meBody = meResp.body();
            if (meResp.statusCode() < 200 || meResp.statusCode() >= 300) {
                log.debugf("Falha no Graph /me (%d): %s", meResp.statusCode(), meBody);
                return null;
            }
            GraphMeResponse me = JsonSerialization.readValue(meBody, GraphMeResponse.class);
            return !isBlank(me.getMail()) ? me.getMail() : me.getUserPrincipalName();
        } catch (IOException e) {
            log.debug("Falha ao buscar e-mail no Microsoft Graph", e);
            return null;
        }
    }

    private String extractField(String json, String field) {
        try {
            JsonNode node = JSON.readTree(json);
            if (node.has(field) && !node.get(field).isNull()) {
                return node.get(field).asText();
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private String requireToken(XboxAuthResponse response, String label) {
        if (response == null || isBlank(response.getToken())) {
            throw new IdentityBrokerException(label + " ausente na resposta do Xbox.");
        }
        return response.getToken();
    }

    private XboxAuthResponse.UserClaim firstUserClaim(XboxAuthResponse response) {
        if (response == null || response.getDisplayClaims() == null
                || response.getDisplayClaims().getXui() == null
                || response.getDisplayClaims().getXui().isEmpty()) {
            return null;
        }
        return response.getDisplayClaims().getXui().get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
