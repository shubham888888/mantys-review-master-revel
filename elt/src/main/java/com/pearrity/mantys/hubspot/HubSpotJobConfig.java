package com.pearrity.mantys.hubspot;

import com.pearrity.mantys.repository.config.SpringContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import java.util.Date;

public class HubSpotJobConfig {

  public static Job getHubSpotJob(String domain, Date from) throws Exception {
    final JobBuilderFactory jobBuilderFactory = SpringContext.getBean(JobBuilderFactory.class);
    HubSpotAuthUtil hubSpotAuthUtil = new HubSpotAuthUtil(domain);
    return jobBuilderFactory
        .get(String.format("%s-HubSpot-job", domain))
        .incrementer(new RunIdIncrementer())
        .start(
            HubSpotStepConfig.getHubSpotStep(
                HubSpotResourceUtil.Deals, domain, hubSpotAuthUtil, from))
        .next(
            HubSpotStepConfig.getHubSpotStep(
                HubSpotResourceUtil.Companies, domain, hubSpotAuthUtil, from))
        .next(
            HubSpotStepConfig.getHubSpotStep(
                HubSpotResourceUtil.Owners, domain, hubSpotAuthUtil, from))
        .build();
  }
}
