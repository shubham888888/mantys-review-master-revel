package com.pearrity.mantys.sudo.service;

import com.pearrity.mantys.auth.AuthService;
import com.pearrity.mantys.repository.SudoRepository;
import com.pearrity.mantys.repository.UserRepository;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MantysSudoServiceImpl implements MantysSudoService {

  @Autowired private UserRepository userRepository;
  @Autowired private SudoRepository sudoRepository;

  @Autowired private AuthService authService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Override
  public Object createUpdateDevAccountsForEachClient(String password) {
    return sudoRepository.createUpdateDevAccountsForEachClient(passwordEncoder.encode(password));
  }

  @Override
  public Object createAccountForUser(Map<String, String> map) {
    Assert.assertNotNull(map.get("email"));
    Assert.assertNotNull(map.get("domain"));
    Assert.assertNotNull(map.get("name"));
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("accountsCreated", sudoRepository.createAccountForUser(map));
    jsonObject.put(
        "mailSent",
        authService
            .sendResetPasswordMailForUsers(map.get("email"))
            .getStatusCode()
            .is2xxSuccessful());
    return jsonObject;
  }

  @Override
  public Object updatePrivilegesForDevAccount() {
    return sudoRepository.updatePrivilegesForDevAccount();
  }
}
