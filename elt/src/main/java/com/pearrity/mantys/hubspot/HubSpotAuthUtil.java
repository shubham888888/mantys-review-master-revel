package com.pearrity.mantys.hubspot;

import com.pearrity.mantys.domain.OAuthSecret;
import com.pearrity.mantys.interfaces.AuthUtil;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.json.JSONObject;

import static com.pearrity.mantys.hubspot.HubSpotResourceUtil.*;
import static com.pearrity.mantys.utils.UtilFunctions.*;

public class HubSpotAuthUtil implements AuthUtil {

  public final String authBaseUrl;
  private final String resourceBaseUrl;
  private final String tenant;
  private final String clientId;
  private final String clientSecret;
  private String accessToken;
  private String refreshToken;

  public HubSpotAuthUtil(String tenant) throws Exception {
    this.tenant = tenant;
    String key = tenant + ".hubspot";
    OAuthSecret oauthSecret =
        SpringContext.getBean(AwsSecretsService.class).getSecret(key, OAuthSecret.class);
    if (oauthSecret == null) throw new RuntimeException("problem getting oauthSecret for " + key);
    resourceBaseUrl = oauthSecret.getResourceBaseUrl();
    refreshToken = UtilFunctions.getRefreshTokenFromRealmIdAndPlatform(tenant, null, "hubspot");
    clientId = oauthSecret.getClientId();
    clientSecret = oauthSecret.getClientSecret();
    authBaseUrl = "%s/oauth/v1/token".formatted(resourceBaseUrl);
    setAccessToken();
  }

  public String getAccessToken() {
    return this.accessToken;
  }

  public void setAccessToken() throws Exception {
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    String body =
        "grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s"
            .formatted(clientId, clientSecret, refreshToken);
    RequestBody requestBody = RequestBody.create(mediaType, body);
    Request request =
        new Request.Builder()
            .url(authBaseUrl)
            .method("POST", requestBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build();
    JSONObject jsonObject =
        makeWebRequest(body, request, tenant, this, true, RequestType.Auth, null, platform, null);

    accessToken = "Bearer " + jsonObject.getString("access_token");
    if (jsonObject.has("refresh_token")) {
      this.refreshToken = jsonObject.getString("refresh_token");
      updateRefreshTokenIfPresent(tenant, refreshToken, null, "hubspot");
    }
  }

  public String getTenant() {
    return tenant;
  }

  public String resourceBaseUrl(String type) {
    return resourceBaseUrl + HubSpotResourceUtil.endPointLookupMap.get(type);
  }

  public String resourceSearchUrl(String type) {
    return resourceBaseUrl + HubSpotResourceUtil.searchEndPointLookupMap.get(type);
  }
}
