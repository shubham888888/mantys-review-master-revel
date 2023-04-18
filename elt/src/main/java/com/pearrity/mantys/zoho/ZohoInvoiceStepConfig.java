package com.pearrity.mantys.zoho;

import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.zoho.processor.ZohoInvoiceProcessor;
import com.pearrity.mantys.zoho.reader.ZohoInvoiceResourceRestApiReader;
import com.pearrity.mantys.zoho.reader.ZohoInvoiceResourceRestApiReaderBuilder;
import com.pearrity.mantys.zoho.writer.ZohoInvoiceDatabaseWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.core.task.TaskExecutor;

import java.io.IOException;

public class ZohoInvoiceStepConfig<T> {

  public <K> ZohoInvoiceStepConfig() {}

  public Step zohoInvoiceSyncStep(Class<?> K, String domain)
      throws IOException, InterruptedException {
    TaskExecutor taskExecutor = SpringContext.getBean(TaskExecutor.class);
    StepBuilderFactory stepBuilderFactory = SpringContext.getBean(StepBuilderFactory.class);
    ZohoInvoiceResourceRestApiReader<Object> zohoInvoiceResourceRestApiReader =
        new ZohoInvoiceResourceRestApiReaderBuilder<>().builder(K, domain);
    return stepBuilderFactory
        .get("zoho-invoice-step-" + K.toString() + "-" + domain)
        .<T, T>chunk(10)
        .reader(
            (ItemReader<? extends T>)
                new ListItemReader<>(zohoInvoiceResourceRestApiReader.loadAllResources()))
        .processor(
            (ItemProcessor<? super T, ? extends T>)
                new ZohoInvoiceProcessor<>(K, domain).getItemProcessor())
        .writer(new ZohoInvoiceDatabaseWriter<>(K, domain).getZohoInvoiceItemWriter())
        .taskExecutor(taskExecutor)
        .build();
  }
}
