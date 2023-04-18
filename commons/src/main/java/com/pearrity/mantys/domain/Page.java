package com.pearrity.mantys.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort.Direction;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page {
  private Long offset;
  private Long limit;
  private Direction dir;
  private String sortBy;
}
