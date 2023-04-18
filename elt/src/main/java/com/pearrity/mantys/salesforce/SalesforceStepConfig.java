package com.pearrity.mantys.salesforce;

import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.salesforce.processor.SalesforceProcessor;
import com.pearrity.mantys.salesforce.reader.SalesforceRestApiReader;
import com.pearrity.mantys.salesforce.writer.SalesForceJdbcWriter;
import com.pearrity.mantys.utils.EtlStepListener;
import org.json.JSONArray;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.core.task.TaskExecutor;

import java.util.Date;

public class SalesforceStepConfig {

  public static Step getSalesforceStep(
      String type, String domain, SalesforceAuthUtil salesforceAuthUtil, Date from)
      throws Exception {
    TaskExecutor taskExecutor = SpringContext.getBean(TaskExecutor.class);
    StepBuilderFactory stepBuilderFactory = SpringContext.getBean(StepBuilderFactory.class);

    return stepBuilderFactory
        .get(String.format("%s-salesforce-%s-step", domain, type))
        .<String, JSONArray>chunk(1)
        .reader(new SalesforceRestApiReader(type, domain, salesforceAuthUtil))
        .processor(new SalesforceProcessor(type, salesforceAuthUtil))
        .writer(new SalesForceJdbcWriter(type, domain))
        .taskExecutor(taskExecutor)
        .listener(new EtlStepListener(type, domain, SalesforceResourceUtil.platform, from, null))
        .build();
  }
}
