package com.pearrity.mantys.zoho;

import com.pearrity.mantys.utils.UtilFunctions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

public class ZohoInvoiceAuthUtil {

  public final String apiBaseUrl;
  public final String resourceBaseUrl;

  private final String refreshToken;
  private String accessToken;
  private final String clientSecret;
  private final String clientId;
  private final String redirectUri;

  public ZohoInvoiceAuthUtil(
      String apiBaseUrl,
      String resourceBaseUrl,
      String refreshToken,
      String clientSecret,
      String clientId,
      String redirectUri)
      throws IOException, InterruptedException {
    this.apiBaseUrl = apiBaseUrl;
    this.resourceBaseUrl = resourceBaseUrl;
    this.refreshToken = refreshToken;
    this.clientSecret = clientSecret;
    this.clientId = clientId;
    this.redirectUri = redirectUri;
    setAccessToken();
  }

  public String getAccessToken() throws IOException, InterruptedException {
    return this.accessToken;
  }

  public void setAccessToken() throws IOException, InterruptedException {
    String endpoint = "/oauth/v2/token";
    String key = "access_token";
    String grant_type = "refresh_token";
    String url =
        String.format(
            "%s%s?refresh_token=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=%s",
            apiBaseUrl, endpoint, refreshToken, clientId, clientSecret, redirectUri, grant_type);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "text/plain; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    this.accessToken = (String) UtilFunctions.makeWebRequest(request, key, 0);
  }
}
