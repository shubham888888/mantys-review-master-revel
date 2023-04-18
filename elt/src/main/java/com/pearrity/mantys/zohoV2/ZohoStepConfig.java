package com.pearrity.mantys.zohoV2;

import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.ZohoInvoiceAuthUtil;
import com.pearrity.mantys.zohoV2.processor.ZohoProcessor;
import com.pearrity.mantys.zohoV2.reader.ZohoApiReader;
import com.pearrity.mantys.zohoV2.writer.ZohoDbWriter;
import org.json.JSONArray;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.core.task.TaskExecutor;

import java.io.IOException;

public class ZohoStepConfig {

  public static Step getZohoInvoiceStep(
      String domain, String org, String type, ZohoInvoiceAuthUtil authUtil)
      throws IOException, InterruptedException {
    TaskExecutor taskExecutor = SpringContext.getBean(TaskExecutor.class);
    StepBuilderFactory stepBuilderFactory = SpringContext.getBean(StepBuilderFactory.class);
    return stepBuilderFactory
        .get(String.format("%s-%s-zoho-%s-step", domain, org, type))
        .<String, JSONArray>chunk(1)
        .reader(ZohoApiReader.getZohoItemReader(type, domain, org, authUtil))
        .processor(new ZohoProcessor(authUtil, type, org))
        .writer(new ZohoDbWriter(type, org, domain))
        .taskExecutor(taskExecutor)
        .build();
  }
}
