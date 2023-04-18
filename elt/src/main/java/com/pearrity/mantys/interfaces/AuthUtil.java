package com.pearrity.mantys.interfaces;

public interface AuthUtil {
  String getAccessToken();

  void setAccessToken() throws Exception;

  String getTenant();
}
