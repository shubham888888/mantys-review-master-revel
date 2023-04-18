package com.pearrity.mantys.quickBooks;

import com.pearrity.mantys.quickBooks.processor.QuickBooksProcessor;
import com.pearrity.mantys.quickBooks.reader.QuickBooksRestApiReader;
import com.pearrity.mantys.quickBooks.writer.QuickBooksJdbcWriter;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.utils.EtlStepListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.core.task.TaskExecutor;

import java.util.Date;

public class QuickBooksStepConfig {

  public static Step getQuickBooksStep(
      String type, String domain, String realmId, QuickBooksAuthUtil quickBooksAuthUtil, Date from)
      throws Exception {
    TaskExecutor taskExecutor = SpringContext.getBean(TaskExecutor.class);
    StepBuilderFactory stepBuilderFactory = SpringContext.getBean(StepBuilderFactory.class);
    return stepBuilderFactory
        .get(String.format("%s-quickbooks-%s-step", domain, type))
        .<JSONObject, JSONArray>chunk(type.toLowerCase().contains("profit") ? 1 : 10)
        .reader(new QuickBooksRestApiReader(quickBooksAuthUtil, type, realmId))
        .processor(new QuickBooksProcessor(type))
        .writer(new QuickBooksJdbcWriter(type, domain, realmId))
        .taskExecutor(taskExecutor)
        .listener(new EtlStepListener(type, domain, QuickBooksResourceUtil.platform, from, realmId))
        .build();
  }
}
