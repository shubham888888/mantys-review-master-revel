package com.pearrity.mantys.config;

import com.pearrity.mantys.repository.config.DbConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@Slf4j
@EnableScheduling
public class SpringBatchConfig {

  private final DbConfiguration dbConfiguration;

  public SpringBatchConfig(@Autowired DbConfiguration dbConfiguration) {
    this.dbConfiguration = dbConfiguration;
  }

  public DataSource batchDataSource() {
    return dbConfiguration.getDataSource("batch");
  }

  @Bean("taskExecutor")
  @Primary
  public TaskExecutor taskExecutor() {
    SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
    asyncTaskExecutor.setConcurrencyLimit(10);
    return asyncTaskExecutor;
  }

  @Bean
  BatchConfigurer configurer() {
    return new DefaultBatchConfigurer(batchDataSource());
  }
}
