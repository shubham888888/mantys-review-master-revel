package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EtlMetadata {

  private Long id;

  private String domain;

  private String platform;

  private String insertQuery;

  private String jsonKeys;

  private String sqlTypes;
}
