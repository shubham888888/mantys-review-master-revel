package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserExperienceTrack {
  private Long id;
  private Long userId;
  private Long viewId;
  private String filterConfig;
  private Long navigatedFrom;
}
