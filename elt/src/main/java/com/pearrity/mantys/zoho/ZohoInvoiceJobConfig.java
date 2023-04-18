package com.pearrity.mantys.zoho;

import com.pearrity.mantys.domain.ZohoCreditNotes;
import com.pearrity.mantys.domain.ZohoInvoice;
import com.pearrity.mantys.repository.config.SpringContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import java.io.IOException;

public class ZohoInvoiceJobConfig {

  public static Job getZohoJob(String domain) throws IOException, InterruptedException {
    JobBuilderFactory jobBuilderFactory = SpringContext.getBean(JobBuilderFactory.class);
    return jobBuilderFactory
        .get("zoho-job")
        .incrementer(new RunIdIncrementer())
        .start(
            new ZohoInvoiceStepConfig<ZohoInvoice>().zohoInvoiceSyncStep(ZohoInvoice.class, domain))
        .next(
            new ZohoInvoiceStepConfig<ZohoCreditNotes>()
                .zohoInvoiceSyncStep(ZohoCreditNotes.class, domain))
        .build();
  }
}
