package com.pearrity.mantys;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DomainSpecificDataSource extends AbstractRoutingDataSource {

  @Override
  protected Object determineCurrentLookupKey() {
    return DataSourceContextHolder.getTenant();
  }
}
