package com.pearrity.mantys.zoho.writer;

import com.pearrity.mantys.repository.config.DbConfiguration;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import static com.pearrity.mantys.utils.UtilFunctions.*;
import static org.springframework.util.Assert.*;

public class ZohoInvoiceDatabaseWriter<T> {

  private final ItemWriter<T> zohoInvoiceItemWriter;

  private final Class<?> type;

  public ZohoInvoiceDatabaseWriter(Class<?> k, String domain) {
    DbConfiguration dbConfiguration = SpringContext.getBean(DbConfiguration.class);
    notNull(dbConfiguration, "dbConfiguration cannot be null");
    DataSource source = dbConfiguration.getJdbcTemplateByDomain(domain).getDataSource();
    notNull(source, "data source cannot be null");
    this.type = k;
    this.zohoInvoiceItemWriter = zohoInvoiceDatabaseItemWriter(source);
  }

  private NamedParameterJdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  private ItemWriter<T> zohoInvoiceDatabaseItemWriter(DataSource dataSource) {
    JdbcBatchItemWriter<T> databaseItemWriter = new JdbcBatchItemWriter<>();
    databaseItemWriter.setDataSource(dataSource);
    databaseItemWriter.setItemSqlParameterSourceProvider(
        new BeanPropertyItemSqlParameterSourceProvider<>());
    String query = prepareQuery(type, ZohoInvoiceResourceUtil.getEndPoint(type));
    databaseItemWriter.setSql(query);
    databaseItemWriter.afterPropertiesSet();
    return databaseItemWriter;
  }

  public ItemWriter<T> getZohoInvoiceItemWriter() {
    return zohoInvoiceItemWriter;
  }
}
