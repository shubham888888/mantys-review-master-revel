package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileImport {

  @Null private Long id;
  @Null private Long importedByUserId;
  @NotNull private List<HashMap> data;
  @Null private Timestamp ttBegin;
  @NotNull private String version;
  @NotNull private Timestamp vtBegin;
  @NotNull private Timestamp vtEnd;
  @Null private Timestamp ttEnd;
  @NotNull private String fileExtensions;
  @NotNull private Long importTemplateId;
  @NotNull private String name;
}
