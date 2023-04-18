package com.pearrity.mantys.domain.metadata;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {

  private String baseCurrency;

  private String toCurrency;

  private Double rate;

  private Timestamp validFrom;

  private Timestamp validTo;
}
