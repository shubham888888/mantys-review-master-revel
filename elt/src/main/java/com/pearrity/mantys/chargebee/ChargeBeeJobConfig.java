package com.pearrity.mantys.chargebee;

import com.pearrity.mantys.repository.config.SpringContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import java.util.Date;

public class ChargeBeeJobConfig {

  public static Job getChargeBeeJob(String domain, Date from) throws Exception {
    final JobBuilderFactory jobBuilderFactory = SpringContext.getBean(JobBuilderFactory.class);
    ChargeBeeAuthUtil hubSpotAuthUtil = new ChargeBeeAuthUtil(domain);
    return jobBuilderFactory
        .get(String.format("%s-ChargeBee-job 🥰", domain))
        .incrementer(new RunIdIncrementer())
        .start(
            ChargeBeeStepConfig.getChargeBeeStep(
                ChargeBeeResourceUtil.Invoices, domain, hubSpotAuthUtil, from))
        .next(
            ChargeBeeStepConfig.getChargeBeeStep(
                ChargeBeeResourceUtil.Customers, domain, hubSpotAuthUtil, from))
        .next(
            ChargeBeeStepConfig.getChargeBeeStep(
                ChargeBeeResourceUtil.CreditNotes, domain, hubSpotAuthUtil, from))
        .build();
  }
}
