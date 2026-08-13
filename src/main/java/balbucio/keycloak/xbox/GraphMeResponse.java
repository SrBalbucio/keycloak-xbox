package balbucio.keycloak.xbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphMeResponse {

    private String mail;

    @JsonProperty("userPrincipalName")
    private String userPrincipalName;

    @JsonProperty("displayName")
    private String displayName;
}
