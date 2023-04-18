package com.pearrity.mantys.sudo.service;

import java.util.Map;

public interface MantysSudoService {
  Object createUpdateDevAccountsForEachClient(String password);

  Object createAccountForUser(Map<String, String> map);

  Object updatePrivilegesForDevAccount();
}
