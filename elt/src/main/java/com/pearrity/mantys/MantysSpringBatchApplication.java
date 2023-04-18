package com.pearrity.mantys;

import com.pearrity.mantys.zoho.reader.ZohoInvoiceResourceRestApiReader;
import com.pearrity.mantys.zoho.reader.ZohoInvoiceResourceRestApiReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.TimeZone;

import static com.pearrity.mantys.domain.utils.Constants.*;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MantysSpringBatchApplication extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(MantysSpringBatchApplication.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(MantysSpringBatchApplication.class);
  }

  @PostConstruct
  public void init() {
    // Setting Spring Boot SetTimeZone
    TimeZone.setDefault(TimeZone.getTimeZone(UTC));
    //    try {
    //      syncData(ZohoInvoice.class, "locus.sh");
    //      syncData(ZohoCreditNotes.class, "locus.sh");
    //    } catch (IOException e) {
    //      throw new RuntimeException(e);
    //    } catch (InterruptedException e) {
    //      throw new RuntimeException(e);
    //    }
  }

  private void syncData(Class<?> K, String domain) throws IOException, InterruptedException {
    ZohoInvoiceResourceRestApiReader<Object> zohoInvoiceResourceRestApiReader =
        new ZohoInvoiceResourceRestApiReaderBuilder<>().builder(K, domain);
    zohoInvoiceResourceRestApiReader.loadAllResources();
  }
}
