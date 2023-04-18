package com.pearrity.mantys.metadata.service;

import com.pearrity.mantys.domain.metadata.Currency;
import java.sql.Timestamp;
import java.util.List;

public interface MetadataService {

  public List<Currency> getCurrencyRates(
      String baseCurrency, Timestamp validFrom, Timestamp validTo) throws Exception;

  public void setCurrencyRates(Currency currency) throws Exception;
}
