package com.pearrity.mantys.metadata.service;

import com.pearrity.mantys.domain.metadata.Currency;
import com.pearrity.mantys.repository.MetadataRepository;
import java.sql.Timestamp;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MetadataServiceImpl implements MetadataService {

  @Autowired MetadataRepository metadataRepository;

  @Override
  public List<Currency> getCurrencyRates(
      String baseCurrency, Timestamp validFrom, Timestamp validTo) throws Exception {
    return metadataRepository.getCurrencyRates(baseCurrency, validFrom, validTo);
  }

  @Override
  public void setCurrencyRates(Currency currency) throws Exception {
    metadataRepository.setCurrencyRates(currency);
  }
}
