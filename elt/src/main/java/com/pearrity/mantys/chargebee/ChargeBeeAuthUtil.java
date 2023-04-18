package com.pearrity.mantys.chargebee;

import com.pearrity.mantys.domain.OAuthSecret;
import com.pearrity.mantys.interfaces.AuthUtil;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.utils.UtilFunctions;
import org.apache.logging.log4j.util.Strings;

public class ChargeBeeAuthUtil implements AuthUtil {

  public final String resourceBaseUrl;
  private final String tenant;
  private final String authKey;

  public ChargeBeeAuthUtil(String tenant) {
    this.tenant = tenant;
    String key = tenant + ".chargebee";
    OAuthSecret oauthSecret =
        SpringContext.getBean(AwsSecretsService.class).getSecret(key, OAuthSecret.class);
    if (oauthSecret == null) throw new RuntimeException("problem getting oauthSecret for " + key);
    authKey = UtilFunctions.getAuthHeader(oauthSecret.getAuthKey(), Strings.EMPTY);
    resourceBaseUrl = oauthSecret.getResourceBaseUrl();
  }

  public String getTenant() {
    return tenant;
  }

  public String resourceBaseUrl(String type) {
    return resourceBaseUrl + ChargeBeeResourceUtil.endPointLookupMap.get(type);
  }

  @Override
  public String getAccessToken() {
    return this.authKey;
  }

  @Override
  public void setAccessToken() {}
}
