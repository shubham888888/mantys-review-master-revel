package com.pearrity.mantys.salesforce;

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

public class SalesforceAuthUtil implements AuthUtil {
  public final String apiBaseUrl;

  public final String resourceBaseUrl;
  private final String refreshToken;
  private final String client_secret;
  private final String resourceBaseUrl1;
  private final String client_id;
  private final String tenant;
  private String accessToken;

  public SalesforceAuthUtil(String tenant) throws Exception {
    String key = tenant + ".salesforce";
    OAuthSecret oauthSecret =
        SpringContext.getBean(AwsSecretsService.class).getSecret(key, OAuthSecret.class);
    if (oauthSecret == null) throw new RuntimeException("problem getting oauthSecret for " + key);
    this.refreshToken = oauthSecret.getRefreshToken();
    client_secret = oauthSecret.getClientSecret();
    client_id = oauthSecret.getClientId();
    resourceBaseUrl1 = oauthSecret.getResourceBaseUrl();
    apiBaseUrl = resourceBaseUrl1 + "/services/oauth2/token";
    resourceBaseUrl = resourceBaseUrl1 + "/services/data/v55.0/sobjects/";
    this.tenant = tenant;
    setAccessToken();
  }

  public String getAccessToken() {
    return "Bearer " + this.accessToken;
  }

  public void setAccessToken() throws Exception {
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    String body =
        "grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s"
            .formatted(client_id, client_secret, refreshToken);
    RequestBody requestBody = RequestBody.create(mediaType, body);
    Request request =
        new Request.Builder()
            .url(apiBaseUrl)
            .method("POST", requestBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
    JSONObject object =
        UtilFunctions.makeWebRequest(
            body,
            request,
            tenant,
            null,
            true,
            RequestType.Auth,
            null,
            SalesforceResourceUtil.platform,
            null);
    this.accessToken = object.getString("access_token");
  }

  @Override
  public String getTenant() {
    return this.tenant;
  }

  public String getQueryUrl() {
    return resourceBaseUrl1 + "/services/data/v55.0/query";
  }

  public String getBulkJobQueryUrl() {
    return resourceBaseUrl1 + "/services/data/v55.0/jobs/query";
  }

  public String getMainBaseUrl() {
    return resourceBaseUrl1;
  }
}
