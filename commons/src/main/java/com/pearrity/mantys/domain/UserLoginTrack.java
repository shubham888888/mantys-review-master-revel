package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginTrack {
  private Long userId;

  private String ipAddr;

  private Timestamp loginTime;
}
