package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.metadata.Currency;
import java.sql.Timestamp;
import java.util.List;

public interface MetadataRepository {

  List<Currency> getCurrencyRates(String baseCurrency, Timestamp validFrom, Timestamp validTo);

  void setCurrencyRates(Currency currency);
}
