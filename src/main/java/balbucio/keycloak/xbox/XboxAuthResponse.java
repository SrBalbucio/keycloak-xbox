package balbucio.keycloak.xbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class XboxAuthResponse {

    @JsonProperty("Token")
    private String token;

    @JsonProperty("IssueInstant")
    private String issueInstant;

    @JsonProperty("NotAfter")
    private String notAfter;

    @JsonProperty("DisplayClaims")
    private DisplayClaims displayClaims;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayClaims {
        @JsonProperty("xui")
        private List<UserClaim> xui;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserClaim {
        @JsonProperty("uhs")
        private String uhs;

        @JsonProperty("xid")
        private String xid;

        @JsonProperty("gtg")
        private String gtg;
    }
}
