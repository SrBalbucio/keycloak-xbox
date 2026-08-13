package balbucio.keycloak.xbox;

import org.junit.jupiter.api.Test;
import org.keycloak.util.JsonSerialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class XboxAuthResponseTest {

    @Test
    void shouldDeserializeUserAuthResponse() throws Exception {
        String payload = "{"
                + "\"IssueInstant\":\"2024-01-01T00:00:00Z\","
                + "\"NotAfter\":\"2024-01-15T00:00:00Z\","
                + "\"Token\":\"d=usertoken123\","
                + "\"DisplayClaims\":{\"xui\":[{\"uhs\":\"1234567890\"}]}"
                + "}";

        XboxAuthResponse response = JsonSerialization.readValue(payload, XboxAuthResponse.class);

        assertEquals("d=usertoken123", response.getToken());
        assertNotNull(response.getDisplayClaims());
        assertEquals(1, response.getDisplayClaims().getXui().size());
        assertEquals("1234567890", response.getDisplayClaims().getXui().get(0).getUhs());
        assertNull(response.getDisplayClaims().getXui().get(0).getXid());
        assertNull(response.getDisplayClaims().getXui().get(0).getGtg());
    }

    @Test
    void shouldDeserializeXstsResponseWithClaims() throws Exception {
        String payload = "{"
                + "\"IssueInstant\":\"2024-01-01T00:00:00Z\","
                + "\"NotAfter\":\"2024-01-15T00:00:00Z\","
                + "\"Token\":\"xststoken456\","
                + "\"DisplayClaims\":{\"xui\":[{"
                + "\"uhs\":\"1234567890\","
                + "\"xid\":\"2533274794093555\","
                + "\"gtg\":\"MasterChief\","
                + "\"agg\":\"Adult\","
                + "\"prv\":\"1 2 3\","
                + "\"usr\":\"123 456\""
                + "}]}"
                + "}";

        XboxAuthResponse response = JsonSerialization.readValue(payload, XboxAuthResponse.class);

        assertEquals("xststoken456", response.getToken());
        XboxAuthResponse.UserClaim claim = response.getDisplayClaims().getXui().get(0);
        assertEquals("2533274794093555", claim.getXid());
        assertEquals("MasterChief", claim.getGtg());
        assertEquals("1234567890", claim.getUhs());
    }

    @Test
    void shouldDeserializeGraphMeResponseWithUpnFallback() throws Exception {
        String payload = "{"
                + "\"mail\":null,"
                + "\"userPrincipalName\":\"player@outlook.com\","
                + "\"displayName\":\"MasterChief\","
                + "\"id\":\"abcdef00-0000-0000-0000-000000000000\""
                + "}";

        GraphMeResponse me = JsonSerialization.readValue(payload, GraphMeResponse.class);

        assertNull(me.getMail());
        assertEquals("player@outlook.com", me.getUserPrincipalName());
        assertEquals("MasterChief", me.getDisplayName());
    }

    @Test
    void shouldHandleGraphMailPresent() throws Exception {
        String payload = "{\"mail\":\"real@email.com\",\"userPrincipalName\":\"upn@x.com\"}";

        GraphMeResponse me = JsonSerialization.readValue(payload, GraphMeResponse.class);

        assertEquals("real@email.com", me.getMail());
        assertEquals("upn@x.com", me.getUserPrincipalName());
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        String payload = "{\"Token\":\"t\",\"DisplayClaims\":{\"xui\":[{\"uhs\":\"u\",\"unexpected\":1}]},\"extra\":\"field\"}";

        XboxAuthResponse response = JsonSerialization.readValue(payload, XboxAuthResponse.class);

        assertEquals("t", response.getToken());
        assertEquals("u", response.getDisplayClaims().getXui().get(0).getUhs());
    }
}
