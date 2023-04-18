package com.pearrity.mantys.auth;

import com.pearrity.mantys.domain.auth.LoginForm;
import com.pearrity.mantys.domain.auth.ResetPasswordData;
import java.security.InvalidAlgorithmParameterException;
import org.springframework.http.ResponseEntity;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface AuthService {

  ResponseEntity<Object> loginViaPasswordAndEmail(LoginForm dto)
	  throws NoSuchPaddingException,
	  IllegalBlockSizeException,
	  NoSuchAlgorithmException,
	  BadPaddingException,
	  InvalidKeyException, InvalidAlgorithmParameterException;

  ResponseEntity<Object> resetPasswordAndLogin(ResetPasswordData dto)
	  throws NoSuchPaddingException,
	  IllegalBlockSizeException,
	  NoSuchAlgorithmException,
	  BadPaddingException,
	  InvalidKeyException, InvalidAlgorithmParameterException;

  ResponseEntity<Object> sendResetPasswordMailForUsers(String email);

  ResponseEntity<Object> refreshJwt(String request)
	  throws NoSuchPaddingException,
	  IllegalBlockSizeException,
	  NoSuchAlgorithmException,
	  BadPaddingException,
	  InvalidKeyException, InvalidAlgorithmParameterException;

  ResponseEntity<Object> logoutCurrentUser();

  ResponseEntity<Object> checkResetLinkStatus(ResetPasswordData data);
}
