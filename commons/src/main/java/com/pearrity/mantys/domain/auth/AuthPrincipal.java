package com.pearrity.mantys.domain.auth;

import com.pearrity.mantys.domain.Privileges;
import com.pearrity.mantys.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPrincipal {
  private Role role;
  private Long userId;
  private Set<Privileges> privileges;
  private String email;
  private String domain;
  private String name;
}
