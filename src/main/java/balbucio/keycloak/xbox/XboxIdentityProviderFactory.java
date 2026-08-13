package balbucio.keycloak.xbox;

import com.google.auto.service.AutoService;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

@AutoService(SocialIdentityProviderFactory.class)
public class XboxIdentityProviderFactory extends AbstractIdentityProviderFactory<XboxIdentityProvider>
        implements SocialIdentityProviderFactory<XboxIdentityProvider> {

    public static final String PROVIDER_ID = "xbox";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        addStringProperty(XboxIdentityProviderConfig.XBOX_USER_AUTH_URL_KEY,
                "Xbox User Auth URL",
                XboxIdentityProviderConfig.DEFAULT_XBOX_USER_AUTH_URL,
                "Endpoint do Xbox Live usado para trocar o token da Microsoft por um Xbox User Token.",
                CONFIG_PROPERTIES);
        addStringProperty(XboxIdentityProviderConfig.XSTS_AUTH_URL_KEY,
                "XSTS Auth URL",
                XboxIdentityProviderConfig.DEFAULT_XSTS_AUTH_URL,
                "Endpoint do Xbox Secure Token Service usado para obter o token XSTS (XUID e gamertag).",
                CONFIG_PROPERTIES);
        addStringProperty(XboxIdentityProviderConfig.XSTS_RELYING_PARTY_KEY,
                "XSTS Relying Party",
                XboxIdentityProviderConfig.DEFAULT_XSTS_RELYING_PARTY,
                "Relying Party usado na troca XSTS. Use http://xboxlive.com para obter XUID e gamertag.",
                CONFIG_PROPERTIES);
        addStringProperty(XboxIdentityProviderConfig.SANDBOX_ID_KEY,
                "Sandbox ID",
                XboxIdentityProviderConfig.DEFAULT_SANDBOX_ID,
                "Sandbox do Xbox (RETAIL para usuarios finais).",
                CONFIG_PROPERTIES);
        addStringProperty(XboxIdentityProviderConfig.GRAPH_ME_URL_KEY,
                "Microsoft Graph /me URL",
                XboxIdentityProviderConfig.DEFAULT_GRAPH_ME_URL,
                "Endpoint do Microsoft Graph usado para obter o e-mail do usuario.",
                CONFIG_PROPERTIES);

        ProviderConfigProperty fetchEmail = new ProviderConfigProperty();
        fetchEmail.setName(XboxIdentityProviderConfig.FETCH_EMAIL_KEY);
        fetchEmail.setLabel("Buscar e-mail via Microsoft Graph");
        fetchEmail.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        fetchEmail.setDefaultValue("true");
        fetchEmail.setHelpText("Quando ativado, busca o e-mail do usuario no Microsoft Graph usando o refresh token.");
        CONFIG_PROPERTIES.add(fetchEmail);
    }

    private static void addStringProperty(String name, String label, String defaultValue,
                                          String helpText, List<ProviderConfigProperty> list) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(defaultValue);
        property.setHelpText(helpText);
        list.add(property);
    }

    @Override
    public String getName() {
        return "Xbox";
    }

    @Override
    public XboxIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        XboxIdentityProviderConfig config = new XboxIdentityProviderConfig(model);
        if (isBlank(config.getAuthorizationUrl())) {
            config.setAuthorizationUrl(XboxIdentityProviderConfig.DEFAULT_AUTH_URL);
        }
        if (isBlank(config.getTokenUrl())) {
            config.setTokenUrl(XboxIdentityProviderConfig.DEFAULT_TOKEN_URL);
        }
        if (isBlank(config.getDefaultScope())) {
            config.setDefaultScope(XboxIdentityProviderConfig.DEFAULT_SCOPE);
        }
        return new XboxIdentityProvider(session, config);
    }

    @Override
    public XboxIdentityProviderConfig createConfig() {
        return new XboxIdentityProviderConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
