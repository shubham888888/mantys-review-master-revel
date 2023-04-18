package com.pearrity.mantys.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.pearrity.mantys.auth.util.AuthConstants;
import com.pearrity.mantys.auth.util.AuthUserUtil;
import com.pearrity.mantys.EncryptionService;
import com.pearrity.mantys.domain.User;
import com.pearrity.mantys.domain.auth.LoginResponse;
import com.pearrity.mantys.repository.UserRepository;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import com.pearrity.mantys.auth.util.AuthConstants;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

  @Autowired private Algorithm algorithm;

  @Autowired private UserRepository userRepository;

  @Autowired private AuthUserUtil authUserUtil;

  @Value("${mantys.jwt.expiration.minutes}")
  private String jwtExpirationInMinutes;

  @Value(("${mantys.refresh.expiration.days}"))
  private String jwtRefreshTokenExpirationInDays;

  @Autowired private EncryptionService encryptionService;

  @Override
  public ResponseEntity<Object> getJwtToken(String refreshTokenS, User user)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    if (refreshTokenS == null) {
      user.setRefreshToken(
          UUID.randomUUID()
              + " "
              + Base64.getEncoder().encodeToString(user.getDomain().getBytes()));
      user.setRefreshTokenCreationTime(Timestamp.from(ZonedDateTime.now().toInstant()));
      userRepository.save(user);
    } else {
      user = userRepository.findByRefreshToken(refreshTokenS).orElseThrow();
      Assert.assertFalse(
          user.getRefreshTokenCreationTime() == null
              || user.getRefreshTokenCreationTime()
                  .toInstant()
                  .plus(Long.parseLong(jwtRefreshTokenExpirationInDays), ChronoUnit.DAYS)
                  .isBefore(Instant.now()));
    }
    return ResponseEntity.ok()
        .body(
            LoginResponse.builder()
                .jwtToken(encryptionService.encrypt(generateJWTToken(user)))
                .refreshToken(encryptionService.encrypt(user.getRefreshToken()))
                .email(user.getEmail())
                .expiryTime(
                    ZonedDateTime.now()
                        .minusSeconds(30)
                        .plusMinutes(Long.parseLong(jwtExpirationInMinutes)))
                .userId(user.getId())
                .build());
  }

  @Override
  public void setDeleted() {
    User user =
        userRepository
            .findByEmail(authUserUtil.getUserEmailFromSecurityContext(), null)
            .orElseThrow();
    user.setRefreshToken(null);
    userRepository.save(user);
  }

  private String generateJWTToken(User user) {
    return JWT.create()
        .withExpiresAt(getExpiryDateForTokens())
        .withIssuer(AuthConstants.MANTYS_DOMAIN)
        .withClaim(AuthConstants.EMAIL, user.getEmail())
        .withClaim(AuthConstants.USER_ID, user.getId())
        .withClaim(AuthConstants.ROLE, user.getRole().name())
        .withClaim(AuthConstants.NAME, user.getName())
        .sign(algorithm);
  }

  private Date getExpiryDateForTokens() {
    return new Date(
        ZonedDateTime.now()
            .plusMinutes(Long.parseLong(jwtExpirationInMinutes))
            .toInstant()
            .toEpochMilli());
  }
}
