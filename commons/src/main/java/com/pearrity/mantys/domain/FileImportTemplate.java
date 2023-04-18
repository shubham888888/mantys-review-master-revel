package com.pearrity.mantys.domain;

import com.pearrity.mantys.domain.enums.Frequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FileImportTemplate {

  private Long id;
  private String label;
  private HashMap schema;
  private Frequency frequency;
  private Integer buffer;
  private String insertQuery;
  private String insertPlaceholders;
  private String insertTypes;
  private String tableName;
}
