package com.pearrity.mantys.auth;

import com.pearrity.mantys.domain.User;
import java.security.InvalidAlgorithmParameterException;
import org.springframework.http.ResponseEntity;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface JwtTokenService {

  ResponseEntity<Object> getJwtToken(String refreshToken, User user)
	  throws NoSuchPaddingException,
	  IllegalBlockSizeException,
	  NoSuchAlgorithmException,
	  BadPaddingException,
	  InvalidKeyException, InvalidAlgorithmParameterException;

  void setDeleted();
}
