package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthSecret {
  private String clientId;
  private String clientSecret;
  private String redirectUri;
  private String refreshToken;
  private String authKey;
  private String organizationIds;
  private String resourceBaseUrl;
  private String loginBaseUrl;
}
