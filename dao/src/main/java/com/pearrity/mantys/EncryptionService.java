package com.pearrity.mantys;

import com.pearrity.mantys.domain.utils.Constants;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import javax.crypto.spec.GCMParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pearrity.mantys.domain.MantysSecretKeys;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class EncryptionService {

  private final SecretKey secretKey;
  private final GCMParameterSpec parameterSpec;
  private final Logger logger;
  private final Base64.Encoder encoder;
  private final Base64.Decoder decoder;

  public EncryptionService(@Autowired AwsSecretsService awsSecretsService) {
    MantysSecretKeys secretKeys =
        awsSecretsService.getSecret(Constants.mantysSecretKeys, MantysSecretKeys.class);
    if (secretKeys == null) throw new RuntimeException();
    this.secretKey =
        new SecretKeySpec(secretKeys.getEncryptKey().getBytes(StandardCharsets.UTF_8), "AES");
    this.parameterSpec = new GCMParameterSpec(128, Arrays.copyOf(secretKeys.getEncryptKey().getBytes(StandardCharsets.UTF_8), 12));
    this.logger = LoggerFactory.getLogger(getClass());
    this.encoder = Base64.getUrlEncoder();
    this.decoder = Base64.getUrlDecoder();
  }

  public String encrypt(String plainText)
      throws NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException,
      InvalidAlgorithmParameterException {
    try {
      byte[] plainTextByte = plainText.getBytes();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
      byte[] encryptedByte = cipher.doFinal(plainTextByte);
      return encoder.encodeToString(encryptedByte);
    } catch (Exception e) {
      logger.error("Failed to encrypt", e);
      throw e;
    }
  }

  public String decrypt(String encrypted)
      throws NoSuchPaddingException,
          IllegalBlockSizeException,
          NoSuchAlgorithmException,
          BadPaddingException,
          InvalidKeyException,
      InvalidAlgorithmParameterException {
    try {
      byte[] encryptedByte = decoder.decode(encrypted);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
      byte[] decryptedByte = cipher.doFinal(encryptedByte);
      return new String(decryptedByte);
    } catch (Exception e) {
      logger.error("Failed to decrypt {}", encrypted, e);
      throw e;
    }
  }
}
