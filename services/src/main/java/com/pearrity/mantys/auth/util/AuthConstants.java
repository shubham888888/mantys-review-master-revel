package com.pearrity.mantys.auth.util;

public class AuthConstants {

  public static final String EMAIL = "email";
  public static final String TOKEN_IDENTIFIER = "token";
  public static final String MANTYS_DOMAIN = "mantys.io";
  public static final String EXCEPTION = "exception";
  public static final String ROLE = "role";
  public static final String USER_ID = "userId";
  public static final String NAME = "name";

  private AuthConstants() {
    throw new RuntimeException("Cannot create instance of this class");
  }

  public static String getDomainFromEmail(String mail) {
    return mail.split("@")[1];
  }
}
