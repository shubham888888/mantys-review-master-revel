package com.pearrity.mantys.salesforce;

import com.pearrity.mantys.repository.config.SpringContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import java.util.Date;

public class SalesforceJobConfig {

  public static Job getSalesforceJob(String domain, Date from) throws Exception {
    final JobBuilderFactory jobBuilderFactory = SpringContext.getBean(JobBuilderFactory.class);
    SalesforceAuthUtil salesforceAuthUtil = new SalesforceAuthUtil(domain);
    return jobBuilderFactory
        .get(String.format("%s-salesforce-job", domain))
        .incrementer(new RunIdIncrementer())
        .start(
            SalesforceStepConfig.getSalesforceStep(
                SalesforceResourceUtil.SalesforceOpportunity, domain, salesforceAuthUtil, from))
        .next(
            SalesforceStepConfig.getSalesforceStep(
                SalesforceResourceUtil.SalesforceUser, domain, salesforceAuthUtil, from))
        .next(
            SalesforceStepConfig.getSalesforceStep(
                SalesforceResourceUtil.SalesforceAccount, domain, salesforceAuthUtil, from))
        .build();
  }
}
