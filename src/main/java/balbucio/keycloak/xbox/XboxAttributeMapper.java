package balbucio.keycloak.xbox;

import com.google.auto.service.AutoService;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

@AutoService(IdentityProviderMapper.class)
public class XboxAttributeMapper extends AbstractIdentityProviderMapper {

    public static final String PROVIDER_ID = "xbox-attribute-mapper";
    public static final String SOURCE_ATTRIBUTE = "sourceAttribute";
    public static final String TARGET_ATTRIBUTE = "targetAttribute";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty source = new ProviderConfigProperty();
        source.setName(SOURCE_ATTRIBUTE);
        source.setLabel("Source attribute");
        source.setType(ProviderConfigProperty.STRING_TYPE);
        source.setHelpText("Atributo do contexto broker, ex: xbox.gamertag");
        CONFIG_PROPERTIES.add(source);

        ProviderConfigProperty target = new ProviderConfigProperty();
        target.setName(TARGET_ATTRIBUTE);
        target.setLabel("Target user attribute");
        target.setType(ProviderConfigProperty.STRING_TYPE);
        target.setHelpText("Chave do atributo de usuario onde o valor mapeado sera armazenado");
        CONFIG_PROPERTIES.add(target);
    }

    @Override
    public String[] getCompatibleProviders() {
        return new String[]{XboxIdentityProviderFactory.PROVIDER_ID};
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Xbox Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "Mapeia um atributo do perfil do Xbox para um atributo de usuario do Keycloak.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
                                            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        mapAttribute(context, mapperModel, null);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
                              IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        mapAttribute(context, mapperModel, user);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
                                   IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        mapAttribute(context, mapperModel, user);
    }

    private void mapAttribute(BrokeredIdentityContext context, IdentityProviderMapperModel mapperModel, UserModel user) {
        String source = mapperModel.getConfig().get(SOURCE_ATTRIBUTE);
        String target = mapperModel.getConfig().get(TARGET_ATTRIBUTE);

        if (isBlank(source) || isBlank(target)) {
            return;
        }

        String value = context.getUserAttribute(source);
        if (!isBlank(value) && user != null) {
            user.setSingleAttribute(target, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
