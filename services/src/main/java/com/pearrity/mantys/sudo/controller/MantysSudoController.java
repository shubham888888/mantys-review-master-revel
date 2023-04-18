package com.pearrity.mantys.sudo.controller;

import com.pearrity.mantys.EncryptionService;
import com.pearrity.mantys.auth.util.AuthUserUtil;
import com.pearrity.mantys.domain.auth.LoginForm;
import com.pearrity.mantys.domain.enums.Role;
import com.pearrity.mantys.sudo.service.MantysSudoService;
import com.pearrity.mantys.auth.util.AuthUserUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.security.InvalidAlgorithmParameterException;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@RestController
@RequestMapping("/services/sudo")
@SecurityRequirement(name = "token")
public class MantysSudoController {

  @Autowired private MantysSudoService mantysSudoService;
  @Autowired private EncryptionService encryptionService;

  @Autowired private AuthUserUtil authUserUtil;

  @PostMapping("/createUpdate/devAccounts")
  public ResponseEntity<Object> createUpdateDevAccountsForEachClient(
      @RequestBody LoginForm password) {
    Assert.assertEquals(Role.SUDO, authUserUtil.getRoleFromSecurityContext());
    return ResponseEntity.ok(
        mantysSudoService.createUpdateDevAccountsForEachClient(password.password()));
  }

  @PostMapping("/create/userAccount")
  public ResponseEntity<Object> createAccountForUser(@RequestBody Map<String, String> map) {
    Assert.assertEquals(Role.SUDO, authUserUtil.getRoleFromSecurityContext());
    return ResponseEntity.ok(mantysSudoService.createAccountForUser(map).toString());
  }

  @PostMapping("/update/devPrivileges")
  public ResponseEntity<Object> updatePrivilegesForDevAccount() {
    Assert.assertEquals(Role.SUDO, authUserUtil.getRoleFromSecurityContext());
    return ResponseEntity.ok(mantysSudoService.updatePrivilegesForDevAccount());
  }

  @GetMapping("getEncrypt")
  public ResponseEntity<String> getEncrypt(@RequestParam String key)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    Assert.assertEquals(Role.SUDO, authUserUtil.getRoleFromSecurityContext());
    return ResponseEntity.ok(encryptionService.encrypt(key));
  }

  @GetMapping("getDecrypt")
  public ResponseEntity<String> getDecrypt(@RequestParam String key)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    Assert.assertEquals(Role.SUDO, authUserUtil.getRoleFromSecurityContext());
    return ResponseEntity.ok(encryptionService.decrypt(key));
  }
}
