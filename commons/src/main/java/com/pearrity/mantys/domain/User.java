package com.pearrity.mantys.domain;

import com.pearrity.mantys.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
  private Long id;
  private String name;
  private String email;
  private String password;
  private Role role;
  private Timestamp deletionTime;
  private String primaryResetToken;
  private String secondaryResetToken;
  private Timestamp resetTokenCreationTime;
  private Timestamp resetTokenExpiryTime;
  private String refreshToken;
  private Timestamp refreshTokenCreationTime;

  public String getDomain() {
    return this.email.split("@")[1];
  }
}
