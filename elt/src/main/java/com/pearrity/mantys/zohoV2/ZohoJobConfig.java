package com.pearrity.mantys.zohoV2;

import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.ZohoInvoiceAuthUtil;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import java.io.IOException;

import static com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil.*;

public class ZohoJobConfig {
  private static final String zoho = ".zoho.";

  public static Job getZohoJob(String domain, String org) throws IOException, InterruptedException {
    JobBuilderFactory jobBuilderFactory = SpringContext.getBean(JobBuilderFactory.class);
    ZohoInvoiceAuthUtil authUtil = getAuthUtilBYDomain(domain);
    return jobBuilderFactory
        .get(String.format("zoho-job-%s-%s", domain, org))
        .incrementer(new RunIdIncrementer())
        .start(
            ZohoStepConfig.getZohoInvoiceStep(
                domain, org, ZohoInvoiceResourceUtil.invoice, authUtil))
        .next(
            ZohoStepConfig.getZohoInvoiceStep(
                domain, org, ZohoInvoiceResourceUtil.creditNote, authUtil))
        .build();
  }
}
