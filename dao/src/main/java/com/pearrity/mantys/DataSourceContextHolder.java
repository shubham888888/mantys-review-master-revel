package com.pearrity.mantys;

public class DataSourceContextHolder {

  private static InheritableThreadLocal<String> tenant = new InheritableThreadLocal<>();

  public static void setTenant(String dataSource) {
    tenant.remove();
    tenant.set(dataSource);
  }

  public static String getTenant() {
    return tenant.get();
  }
}
