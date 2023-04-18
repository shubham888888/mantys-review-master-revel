package com.pearrity.mantys.auth.controller;

import com.pearrity.mantys.auth.AuthService;
import com.pearrity.mantys.EncryptionService;
import com.pearrity.mantys.domain.auth.LoginForm;
import com.pearrity.mantys.domain.auth.ResetPasswordData;

import java.security.InvalidAlgorithmParameterException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/services/auth")
public class AuthController {

  @Autowired private EncryptionService encryptionService;

  @Autowired private AuthService authService;

  @PostMapping()
  public ResponseEntity<Object> loginViaPasswordAndEmail(@RequestBody @Valid LoginForm dto)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    return authService.loginViaPasswordAndEmail(dto);
  }

  @PostMapping("/reset")
  public ResponseEntity<Object> resetPasswordAndLogin(@RequestBody @Valid ResetPasswordData dto)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    return authService.resetPasswordAndLogin(dto);
  }

  @GetMapping("/for-sudo")
  public ResponseEntity<Object> sendResetPasswordForSudo() {
    return authService.sendResetPasswordMailForUsers(null);
  }

  @PostMapping("/reset/mail")
  public ResponseEntity<Object> sendResetPasswordMailForUsers(@RequestBody @Valid LoginForm dto) {
    return authService.sendResetPasswordMailForUsers(dto.email());
  }

  @PostMapping("/refresh/jwt")
  public ResponseEntity<Object> refreshJwt(@RequestHeader String token)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    return authService.refreshJwt(encryptionService.decrypt(token));
  }

  @Operation(
      description = "checks the validity of reset mail link",
      responses = {
        @ApiResponse(responseCode = "200", description = "valid reset token"),
        @ApiResponse(responseCode = "400", description = "invalid reset token")
      })
  @PostMapping(value = "/reset/check", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> checkResetLinkStatus(@RequestBody ResetPasswordData data) {
    return authService.checkResetLinkStatus(data);
  }
}
