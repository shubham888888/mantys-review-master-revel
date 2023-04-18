package com.pearrity.mantys.repository;

import java.util.Map;

public interface SudoRepository {

  Object createUpdateDevAccountsForEachClient(String password);

  Object createAccountForUser(Map<String, String> map);

  Object updatePrivilegesForDevAccount();
}
