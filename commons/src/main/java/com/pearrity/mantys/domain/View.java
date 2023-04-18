package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class View {
  private Long id;
  private Long objectId;
  private Long typeId;
  private String name;
  private String json;
}
