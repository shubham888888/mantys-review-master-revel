package com.pearrity.mantys.domain;

import com.pearrity.mantys.domain.enums.Action;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Privileges {
  private Long id;
  private Long userId;
  private Action action;
  private Long objectId;
}
