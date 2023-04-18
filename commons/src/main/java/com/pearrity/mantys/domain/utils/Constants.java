package com.pearrity.mantys.domain.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.stream.Collectors;

public class Constants {
  public static final String MANTYS_DB_NAME = "mantys";

  public static final String MANTYS_DOMAIN = "mantys.io";

  public static final int GMAIL_SMTP_PORT = 587;

  public static final String MailHost = "smtp.gmail.com";

  public static final String MANTYS_MAIL_KEY = "mantys.mail";

  public static final String LOCAL = "local";
  public static final String port = ":5432/";
  public static final String driverClassName = "org.postgresql.Driver";
  public static final String jdbcUrlInitial = "jdbc:postgresql://";
  public static final String dbUser = "postgres";
  public static final String clientFetchQuery =
      "select c.* from public.client c where c.disabled = false";
  public static final String DEV = "dev";
  public static final String mantysSecretKeys = "mantysKeys";
  public static final String responseHeadersFromResponse = "response_headers_from_response";
  public static final String responseCodeFromResponse = "response_code_from_response";
  public static final String nullValue = "null";
  public static final String syncBegin = "sync_begin";
  public static final String SForceLocator = "Sforce-Locator";
  public static final String UTC = "UTC";
  public static final String fail = "failed";
  public static final String created = "created";
  public static final String success = "successful";
  public static final String TEXT_CSV = "text/csv";
  public static DateTimeFormatter salesforceDateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxxx");
  public static DateTimeFormatter salesforceDateTimeFormatter2 =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static String getResourceFileAsString(Class<?> classParam, String fileName) {
    InputStream is = getResourceFileAsInputStream(classParam, fileName);
    if (is != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } else {
      throw new RuntimeException("resource not found");
    }
  }

  public static InputStream getResourceFileAsInputStream(Class<?> classParam, String fileName) {
    ClassLoader classLoader = classParam.getClassLoader();
    return classLoader.getResourceAsStream(fileName);
  }

  public static String getDateTimeString(
      DateTimeFormatter offsetFormatter, TemporalAccessor dateTime) {
    return offsetFormatter == null
        ? OffsetDateTime.from(dateTime).toString()
        : OffsetDateTime.from(dateTime).format(offsetFormatter);
  }
}
