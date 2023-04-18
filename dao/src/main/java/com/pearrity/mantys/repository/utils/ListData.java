package com.pearrity.mantys.repository.utils;

import java.util.List;

@FunctionalInterface
public interface ListData {
  List<String> get(String tenant, String arg);
}
