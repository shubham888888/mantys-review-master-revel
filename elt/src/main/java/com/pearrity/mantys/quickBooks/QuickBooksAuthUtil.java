package com.pearrity.mantys.quickBooks;

import com.google.gson.Gson;
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

import java.util.List;
import java.util.Objects;

import static com.pearrity.mantys.quickBooks.QuickBooksResourceUtil.*;
import static com.pearrity.mantys.utils.UtilFunctions.*;

public class QuickBooksAuthUtil implements AuthUtil {

  public static final long minorVersion = 63L;

  public final String resourceBaseUrl;
  public final String queryBaseUrl;
  private final String clientId;
  private final String clientSecret;
  private final String tenant;
  private final String realmId;
  private String refreshToken;
  private String accessToken;

  public QuickBooksAuthUtil(String tenant, String realmId) throws Exception {
    this.tenant = tenant;
    String key = tenant + ".quickbooks";
    this.refreshToken =
        UtilFunctions.getRefreshTokenFromRealmIdAndPlatform(tenant, realmId, platform);
    List oauthSecrets = SpringContext.getBean(AwsSecretsService.class).getSecret(key, List.class);
    if (oauthSecrets == null) throw new RuntimeException("problem getting oauthSecret for " + key);
    OAuthSecret oauthSecret =
        (OAuthSecret)
            oauthSecrets.stream()
                .filter(
                    a ->
                        Objects.equals(
                            (new Gson().fromJson(new Gson().toJson(a), OAuthSecret.class))
                                .getOrganizationIds()
                                .trim(),
                            realmId))
                .findFirst()
                .orElseThrow();
    clientSecret = oauthSecret.getClientSecret();
    clientId = oauthSecret.getClientId();
    resourceBaseUrl = oauthSecret.getResourceBaseUrl();
    this.queryBaseUrl = resourceBaseUrl + "/v3/company/%s/query?minorversion=%d";
    this.realmId = realmId;
    setAccessToken();
  }

  public String getAccessToken() {
    return this.accessToken;
  }

  public void setAccessToken() throws Exception {
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
    RequestBody requestBody = RequestBody.create(mediaType, body);
    Request request =
        new Request.Builder()
            .url("https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer")
            .method("POST", requestBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", getAuthHeader(clientId, clientSecret))
            .build();
    JSONObject jsonObject =
        makeWebRequest(
            body, request, tenant, this, true, RequestType.Auth, null, platform, realmId);
    accessToken = "Bearer " + jsonObject.getString("access_token");
    if (jsonObject.has("refresh_token")) {
      this.refreshToken = jsonObject.getString("refresh_token");
      updateRefreshTokenIfPresent(tenant, refreshToken, realmId, platform);
    }
  }

  public String getTenant() {
    return tenant;
  }
}
