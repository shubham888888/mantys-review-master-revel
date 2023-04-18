package com.pearrity.mantys.repository.config;

import com.pearrity.mantys.domain.DbSource;
import com.pearrity.mantys.domain.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.*;

@Component
public class DbConfiguration {

  @Autowired private AwsSecretsService awsSecretsService;

  public Map<Object, Object> getDataSourceMap() {
    return new Hashtable<>(jdbcTemplateMap);
  }

  private final Map<Object, Object> jdbcTemplateMap = new Hashtable<>();

  @PostConstruct
  public void loadAllClientsJdbcTemplate() {
    JdbcTemplate mantysJdbcTemplate = new JdbcTemplate(getDataSource(Constants.MANTYS_DB_NAME));
    this.jdbcTemplateMap.put(Constants.MANTYS_DOMAIN, mantysJdbcTemplate);
    List<Map<String, Object>> clients = mantysJdbcTemplate.queryForList(Constants.clientFetchQuery);

    for (Map<String, Object> c : clients) {
      jdbcTemplateMap.put(
          ((String) c.get("domain")).trim(),
          new JdbcTemplate(getDataSource((String) c.get("name"))));
    }
  }

  public JdbcTemplate getJdbcTemplateByDomain(String domain) {
    return (JdbcTemplate) this.jdbcTemplateMap.get(domain);
  }

  public DataSource getDataSource(String domain) {
    final DriverManagerDataSource dataSource = new DriverManagerDataSource();
    DbSource dbSource =
        awsSecretsService.getSecret(Constants.MANTYS_DOMAIN + ".datasource", DbSource.class);
    if (dbSource == null) throw new RuntimeException();
    dataSource.setDriverClassName(Constants.driverClassName);
    dataSource.setUrl(Constants.jdbcUrlInitial + dbSource.getHost() + Constants.port + domain);
    dataSource.setUsername(Constants.dbUser);
    dataSource.setPassword(dbSource.getPassword());
    return dataSource;
  }

  public Set<Object> getClientJdbcTemplateMapKeys() {
    Set<Object> set = new HashSet<>(this.jdbcTemplateMap.keySet());
    set.remove(Constants.MANTYS_DOMAIN);
    return set;
  }

  public Set<Object> getAllJdbcTemplateMapKeys() {
    return new HashSet<>(this.jdbcTemplateMap.keySet());
  }

  public List<JdbcTemplate> getAllClientJdbcTemplates() {
    List<JdbcTemplate> jdbcTemplates = new ArrayList<>();
    for (Object s : getClientJdbcTemplateMapKeys()) {
      jdbcTemplates.add(getJdbcTemplateByDomain((String) s));
    }
    return jdbcTemplates;
  }
}
