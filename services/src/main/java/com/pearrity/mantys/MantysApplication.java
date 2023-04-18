package com.pearrity.mantys;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

import static com.pearrity.mantys.domain.utils.Constants.*;

@SpringBootApplication(
    scanBasePackages = "com.pearrity.mantys",
    exclude = {DataSourceAutoConfiguration.class})
@SecurityScheme(
    name = "token",
    scheme = "apiKey",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER)
@EnableAsync
public class MantysApplication extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(MantysApplication.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(MantysApplication.class);
  }

  @PostConstruct
  public void init() {
    /** Setting Spring Boot SetTimeZone */
    TimeZone.setDefault(TimeZone.getTimeZone(UTC));
  }
}
