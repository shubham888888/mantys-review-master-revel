package com.pearrity.mantys.zoho.reader;

import com.pearrity.mantys.domain.OAuthSecret;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import com.pearrity.mantys.repository.config.SpringContext;

import java.io.IOException;
import java.util.Objects;

public class ZohoInvoiceResourceRestApiReaderBuilder<T> {
  private static final String zoho = ".zoho";

  public ZohoInvoiceResourceRestApiReader<T> builder(Class<?> K, String domain)
      throws IOException, InterruptedException {
    String key = domain + zoho;
    OAuthSecret oauthSecret =
        SpringContext.getBean(AwsSecretsService.class).getSecret(key, OAuthSecret.class);
    if (oauthSecret == null) throw new RuntimeException("problem getting oauthSecret for " + key);
    String[] organizationIds = Objects.requireNonNull(oauthSecret.getOrganizationIds()).split(",");
    return new ZohoInvoiceResourceRestApiReader<>(K, domain, organizationIds);
  }
}
