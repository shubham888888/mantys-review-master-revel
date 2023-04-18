package com.pearrity.mantys.quickBooks;

import com.pearrity.mantys.repository.config.SpringContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;

import java.util.Date;

public class QuickBooksJobConfig {

  public static Job getQuickBooksJob(String domain, String realmId, Date from) throws Exception {
    final JobBuilderFactory jobBuilderFactory = SpringContext.getBean(JobBuilderFactory.class);
    QuickBooksAuthUtil quickBooksAuthUtil = new QuickBooksAuthUtil(domain, realmId);
    return jobBuilderFactory
        .get(String.format("%s-quickbooks-job", domain))
        .incrementer(new RunIdIncrementer())
        .start(
            QuickBooksStepConfig.getQuickBooksStep(
                QuickBooksResourceUtil.Invoice, domain, realmId, quickBooksAuthUtil, from))
        .next(
            QuickBooksStepConfig.getQuickBooksStep(
                QuickBooksResourceUtil.Attachable2, domain, realmId, quickBooksAuthUtil, from))
        .build();
  }

  public static Job getQuickBooksReportJob(String domain, String realmId, Date from)
      throws Exception {
    final JobRepository jobRepository = SpringContext.getBean(JobRepository.class);
    QuickBooksAuthUtil quickBooksAuthUtil = new QuickBooksAuthUtil(domain, realmId);
    return new JobBuilder(String.format("%s-quickbooks-report-job", domain))
        .start(
            QuickBooksStepConfig.getQuickBooksStep(
                QuickBooksResourceUtil.ProfitAndLossDetail,
                domain,
                realmId,
                quickBooksAuthUtil,
                from))
        .next(
            QuickBooksStepConfig.getQuickBooksStep(
                QuickBooksResourceUtil.ProfitAndLossReport,
                domain,
                realmId,
                quickBooksAuthUtil,
                from))
        .next(
            QuickBooksStepConfig.getQuickBooksStep(
                QuickBooksResourceUtil.Attachable, domain, realmId, quickBooksAuthUtil, from))
        .repository(jobRepository)
        .build();
  }
}
