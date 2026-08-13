# Keycloak Xbox Identity Provider (SPI)

Extensão de **Social Login** do Keycloak para a **conta Microsoft/Xbox Live**, permitindo que usuários façam login/cadastro no Keycloak usando sua conta Xbox (Microsoft Account) via OAuth 2.0 (Authorization Code Grant).

Compatível com **Keycloak 26.7.1** (Java 17).

> Ao contrário de um provider OAuth2 comum, o Xbox Live exige um **fluxo de autenticação em múltiplas etapas**: após o login na Microsoft, o token é trocado duas vezes nos serviços do Xbox até obter o **XUID** (Xbox User ID) e o **gamertag** do jogador. Este SPI implementa todo esse encadeamento.

## Fluxo de autenticação

```
Usuário  ──login──▶  Microsoft (OAuth2 code)  ──▶  MSA access token
                                                           │
                                                           ▼
                                    user.auth.xboxlive.com/user/authenticate   (RPS)
                                                           │  → Xbox User Token + uhs
                                                           ▼
                                    xsts.auth.xboxlive.com/xsts/authorize      (XSTS)
                                                           │  → XSTS Token + XUID + gamertag
                                                           ▼
                                    graph.microsoft.com/v1.0/me   (refresh p/ Graph, opcional)
                                                           │  → e-mail
                                                           ▼
                                              BrokeredIdentityContext (Keycloak)
```

1. **OAuth2 Authorization Code** padrão contra o Microsoft Identity (consumers) → `access_token` MSA.
2. **POST** `user.auth.xboxlive.com/user/authenticate` com `RpsTicket: d=[MSA token]` → Xbox User Token + `uhs` (user hash).
3. **POST** `xsts.auth.xboxlive.com/xsts/authorize` com o User Token → **XSTS Token** e `DisplayClaims.xui[]` contendo `xid` (XUID) e `gtg` (gamertag).
4. **(Opcional)** E-mail via Microsoft Graph: o `refresh_token` é resgatado por um token de Graph (`scope=User.Read`) e então `GET /me` retorna o e-mail.

A etapa 1 é feita pelo Keycloak; as etapas 2–4 são feitas dentro do SPI (em `doGetFederatedIdentity` / `getFederatedIdentity`).

## Endpoints utilizados

| Endpoint | URL |
|----------|-----|
| Authorization | `https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize` |
| Token | `https://login.microsoftonline.com/consumers/oauth2/v2.0/token` |
| Xbox User Auth (RPS) | `https://user.auth.xboxlive.com/user/authenticate` |
| XSTS Auth | `https://xsts.auth.xboxlive.com/xsts/authorize` |
| Microsoft Graph /me | `https://graph.microsoft.com/v1.0/me` |
| Scope padrão | `xboxlive.signin offline_access User.Read` |

> **Importante — audiência do token:** o access token inicial precisa ter audiência Xbox para que a troca RPS funcione. Por isso `xboxlive.signin` é mantido como **primeiro** scope. Se a troca RPS falhar em produção, a causa mais provável é reordenação/adição de scopes que mude a audiência do token emitido.

## Mapeamento de atributos

O `DisplayClaims.xui[]` retornado pelo XSTS é mapeado para o `BrokeredIdentityContext`:

| Claim Xbox (XSTS) | Destino no Keycloak |
|-------------------|---------------------|
| `xid` (XUID) | ID federado / `brokerUserId` |
| `gtg` (gamertag) | `username` (fallback: XUID) |
| `xid` | atributo `xbox.xuid` |
| `gtg` | atributo `xbox.gamertag` |
| `uhs` | atributo `xbox.uhs` |

E-mail (quando a busca via Graph está habilitada):

| Origem | Destino |
|--------|---------|
| Graph `/me` (`mail`, com fallback `userPrincipalName`) | `email` |

## Build

```bash
mvn clean package
```

O artefato final será gerado em `target/keycloak-xbox.jar` (fat jar com jsoup embutido).

## Deploy

1. Copie `target/keycloak-xbox.jar` para o diretório `providers/` do Keycloak:
   ```bash
   cp target/keycloak-xbox.jar $KEYCLOAK_HOME/providers/
   ```
2. Reinicie o Keycloak.

## Configuração no Keycloak

### 1. Registrar a aplicação no Microsoft Entra ID

> O fluxo segue a [documentação oficial de sign-in para title websites](https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/s2s-auth-calls/service-authentication/live-website-authentication).

1. Acesse o [Azure Portal](https://portal.azure.com) → **Microsoft Entra ID → App registrations → New registration**.
2. Em **Supported account types**, selecione **Personal Microsoft accounts only** (contas Xbox/Live).
3. Defina o **Redirect URI** (Web) como:
   ```
   https://<SEU_KEYCLOAK>/realms/<SEU_REALM>/broker/xbox/endpoint
   ```
4. Anote o **Application (client) ID** e crie um **Client Secret** em *Certificates & secrets*.
5. Em **API permissions**, adicione a permissão delegada **Microsoft Graph → User.Read** (necessária para a busca de e-mail consentida no login). Caso não precise de e-mail, pode ignorar este passo e desabilitar a busca no provider.

> Para acessar serviços de um *title* específico do Xbox (além do perfil básico), é necessário ter um título publicado no [Partner Center](https://partner.microsoft.com/) com uma Relying Party própria — ajuste o campo **XSTS Relying Party** conforme necessário.

### 2. Configurar o provider no Keycloak

1. No admin-console, vá em **Identity Providers → Add provider → Xbox**.
2. Preencha:
   - **Client Id**: Application (client) ID do Entra.
   - **Client Secret**: Client Secret gerado.
   - **Default Scopes**: `xboxlive.signin offline_access User.Read` (padrão).
3. Campos opcionais (já vêm preenchidos com defaults):

   | Campo | Default | Descrição |
   |-------|---------|-----------|
   | Xbox User Auth URL | `https://user.auth.xboxlive.com/user/authenticate` | Troca do token MSA por User Token. |
   | XSTS Auth URL | `https://xsts.auth.xboxlive.com/xsts/authorize` | Troca do User Token por XSTS. |
   | XSTS Relying Party | `http://xboxlive.com` | RP do XSTS — retorna XUID + gamertag. |
   | Sandbox ID | `RETAIL` | `RETAIL` para usuários finais, `XDKS.1` para sandbox de desenvolvimento. |
   | Microsoft Graph /me URL | `https://graph.microsoft.com/v1.0/me` | Endpoint usado para obter o e-mail. |
   | Buscar e-mail via Microsoft Graph | `true` | Liga/desliga a etapa de e-mail via Graph. |

4. Salve.

### 3. (Opcional) Attribute Mapper

O provider registra o mapper **Xbox Attribute Mapper**, que permite mapear qualquer atributo do contexto broker (ex: `xbox.gamertag`, `xbox.xuid`) para um atributo de usuário do Keycloak, configurável via UI sem recompilar.

Exemplo:
- **Source attribute**: `xbox.gamertag`
- **Target user attribute**: `gamertag`

## Notas

- **E-mail é best-effort:** a busca via Graph falha de forma silenciosa (log em `DEBUG`) se não houver `refresh_token`, se faltar consentimento de `User.Read`, ou se o client secret estiver referenciado no vault (`${vault.xxx}`) — nesse caso o refresh não resolve via vault. O login continua funcionando, apenas sem e-mail.
- **Store Tokens** vem habilitado por padrão (preserva o `refresh_token` para o e-mail e para o `retrieveToken` do broker).
- O HTTP é feito com **Jsoup** (POST JSON para o Xbox, form-urlencoded para o refresh), consistente com os outros providers da série.

## Testes

```bash
mvn test
```

Os testes validam a desserialização das respostas do `user.auth.xboxlive.com` (User Token + `uhs`), do XSTS (XUID + gamertag), do Graph `/me` (e-mail com fallback de UPN) e a tolerância a campos desconhecidos.

## Licença

MIT

## Autor

- [SrBalbucio](https://github.com/SrBalbucio)
