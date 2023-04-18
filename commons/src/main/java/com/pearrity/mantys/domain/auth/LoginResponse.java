package com.pearrity.mantys.domain.auth;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Builder
@Data
public class LoginResponse {
  private String jwtToken;
  private String email;
  private ZonedDateTime expiryTime;
  private String refreshToken;
  private Long userId;
}
