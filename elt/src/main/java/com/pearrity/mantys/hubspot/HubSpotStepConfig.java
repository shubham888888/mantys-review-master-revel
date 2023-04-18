package com.pearrity.mantys.hubspot;

import com.pearrity.mantys.hubspot.processor.HubSpotProcessor;
import com.pearrity.mantys.hubspot.reader.HubSpotRestApiReader;
import com.pearrity.mantys.hubspot.writer.HubSpotJdbcWriter;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.utils.EtlStepListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.core.task.TaskExecutor;

import java.util.Date;
import java.util.Objects;

import static com.pearrity.mantys.hubspot.HubSpotResourceUtil.*;

public class HubSpotStepConfig {

  public static Step getHubSpotStep(
      String type, String domain, HubSpotAuthUtil hubSpotAuthUtil, Date from) throws Exception {
    TaskExecutor taskExecutor = SpringContext.getBean(TaskExecutor.class);
    StepBuilderFactory stepBuilderFactory = SpringContext.getBean(StepBuilderFactory.class);
    return stepBuilderFactory
        .get(String.format("%s-hubspot-%s-step", domain, type))
        .<JSONObject, JSONArray>chunk(Objects.equals(type, Deals) ? 1 : 10)
        .reader(new HubSpotRestApiReader(hubSpotAuthUtil, type))
        .processor(new HubSpotProcessor(type))
        .writer(new HubSpotJdbcWriter(type, domain))
        .taskExecutor(taskExecutor)
        .listener(new EtlStepListener(type, domain, "hubspot", from, null))
        .build();
  }
}
