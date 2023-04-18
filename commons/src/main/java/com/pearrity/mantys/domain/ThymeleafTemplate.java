package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThymeleafTemplate {
  private Long id;
  private String html;
  private String name;
}
