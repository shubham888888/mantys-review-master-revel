package com.pearrity.mantys.chargebee;

import com.pearrity.mantys.chargebee.processor.ChargeBeeProcessor;
import com.pearrity.mantys.chargebee.reader.ChargeBeeRestApiReader;
import com.pearrity.mantys.chargebee.writer.ChargebeeJdbcWriter;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.utils.EtlStepListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.core.task.TaskExecutor;

import java.util.Date;

public class ChargeBeeStepConfig {

  public static Step getChargeBeeStep(
      String type, String domain, ChargeBeeAuthUtil chargeBeeAuthUtil, Date from) throws Exception {
    TaskExecutor taskExecutor = SpringContext.getBean(TaskExecutor.class);
    StepBuilderFactory stepBuilderFactory = SpringContext.getBean(StepBuilderFactory.class);
    return stepBuilderFactory
        .get(String.format("%s-ChargeBee-%s-step", domain, type))
        .<JSONObject, JSONArray>chunk(10)
        .reader(new ChargeBeeRestApiReader(chargeBeeAuthUtil, type))
        .processor(new ChargeBeeProcessor(type))
        .writer(new ChargebeeJdbcWriter(type, domain))
        .taskExecutor(taskExecutor)
        .listener(new EtlStepListener(type, domain, "chargebee", from, null))
        .build();
  }
}
