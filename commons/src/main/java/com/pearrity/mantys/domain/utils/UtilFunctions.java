package com.pearrity.mantys.domain.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;

import static com.pearrity.mantys.domain.utils.Constants.*;

public class UtilFunctions {
  public static String getDomainFromEmail(String mail) {
    return mail.split("@")[1];
  }

  public static String getDomainFromRefreshToken(String refresh) {
    return new String(Base64.getDecoder().decode(refresh.substring(refresh.lastIndexOf(" ") + 1)));
  }

  public static Date incrementMonth(Date start) {
    return Date.from(
        ZonedDateTime.from(start.toInstant().atZone(ZoneId.of(UTC))).plusMonths(1).toInstant());
  }
}
