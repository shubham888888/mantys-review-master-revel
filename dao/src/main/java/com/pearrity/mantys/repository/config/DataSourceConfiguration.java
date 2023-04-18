package com.pearrity.mantys.repository.config;

import com.pearrity.mantys.DomainSpecificDataSource;
import com.pearrity.mantys.domain.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Configuration
public class DataSourceConfiguration {

  @Autowired DbConfiguration dbConfiguration;

  @Bean
  public DataSource clientDatasource() {
    Set<Object> set = dbConfiguration.getAllJdbcTemplateMapKeys();
    Map<Object, Object> targetDataSources = new Hashtable<>();
    set.forEach(
        a ->
            targetDataSources.put(
                a, dbConfiguration.getJdbcTemplateByDomain((String) a).getDataSource()));
    DomainSpecificDataSource domainSpecificDataSource = new DomainSpecificDataSource();
    domainSpecificDataSource.setTargetDataSources(targetDataSources);
    domainSpecificDataSource.setDefaultTargetDataSource(
        Objects.requireNonNull(targetDataSources.get(Constants.MANTYS_DOMAIN)));
    return domainSpecificDataSource;
  }
}
