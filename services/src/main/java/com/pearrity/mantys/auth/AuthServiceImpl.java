package com.pearrity.mantys.auth;

import com.pearrity.mantys.DataSourceContextHolder;
import com.pearrity.mantys.domain.SendEmailData;
import com.pearrity.mantys.domain.User;
import com.pearrity.mantys.domain.auth.LoginForm;
import com.pearrity.mantys.domain.auth.ResetPasswordData;
import com.pearrity.mantys.domain.enums.Role;
import com.pearrity.mantys.domain.utils.UtilFunctions;
import com.pearrity.mantys.mail.EmailService;
import com.pearrity.mantys.repository.UserRepository;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import com.pearrity.mantys.mail.EmailService;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.pearrity.mantys.domain.utils.UtilFunctions.getDomainFromRefreshToken;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

  @Autowired TemplateEngine templateEngine;
  @Autowired private EmailService emailService;
  @Autowired private UserRepository userRepository;

  @Value("${mantys.sudo.mail}")
  private String mantysSudoMail;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenService tokenService;

  @Value("${mantys.invitation.expiryTime.in.days}")
  private String invitationExpiryTimeInDays;

  @Override
  public ResponseEntity<Object> loginViaPasswordAndEmail(LoginForm dto)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    DataSourceContextHolder.setTenant(UtilFunctions.getDomainFromEmail(dto.email()));
    User user =
        userRepository
            .findByEmail(dto.email(), null)
            .orElseThrow(() -> new Error("User with this email does not exist"));
    Assert.assertTrue(
        "Invalid credentials", passwordEncoder.matches(dto.password(), user.getPassword()));
    return tokenService.getJwtToken(null, user);
  }

  @Override
  public ResponseEntity<Object> resetPasswordAndLogin(ResetPasswordData dto)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    DataSourceContextHolder.setTenant(UtilFunctions.getDomainFromEmail(dto.email()));
    User user =
        userRepository
            .findByUserIdAndPrimaryAndSecondaryTokenAndNotExpired(
                dto.email(),
                dto.primaryToken(),
                dto.secondaryToken(),
                Timestamp.from(ZonedDateTime.now().toInstant()))
            .orElseThrow(() -> new Error("Invalid link used for reset"));
    user.setResetTokenExpiryTime(Timestamp.from(ZonedDateTime.now().toInstant()));
    user.setPassword(passwordEncoder.encode(dto.password()));
    return tokenService.getJwtToken(null, user);
  }

  @Override
  public ResponseEntity<Object> sendResetPasswordMailForUsers(String email) {
    return sendPasswordResetMail(email == null ? mantysSudoMail : email, email == null);
  }

  private ResponseEntity<Object> sendPasswordResetMail(String mail, boolean sudo) {
    DataSourceContextHolder.setTenant(UtilFunctions.getDomainFromEmail(mail));
    User user = verifyEmailAndGetUser(mail, sudo);
    Assert.assertTrue(
        "Cannot send another password reset within 24 hrs",
        user.getResetTokenCreationTime() == null
            || user.getResetTokenCreationTime()
                .toInstant()
                .isBefore(ZonedDateTime.now().minusHours(24).toInstant()));
    user.setPrimaryResetToken(getUniquePrimaryToken());
    user.setSecondaryResetToken(getUniqueSecondaryToken());
    user.setResetTokenExpiryTime(
        Timestamp.from(
            ZonedDateTime.now().plusDays(Long.parseLong(invitationExpiryTimeInDays)).toInstant()));
    user.setResetTokenCreationTime(Timestamp.from(ZonedDateTime.now().toInstant()));
    userRepository.save(user);
    sendResetPasswordMail(user);
    return ResponseEntity.ok().build();
  }

  public void sendResetPasswordMail(User user) {
    String s =
        String.format(
            "https://mantys.in/login/reset/password?e=%s&q1=%s&q2=%s",
            user.getEmail(), user.getPrimaryResetToken(), user.getSecondaryResetToken());
    sendResetPasswordMail(user.getEmail(), s);
  }

  private void sendResetPasswordMail(String emailTo, String url) {
    Context context = new Context();
    context.setVariable("url", url);
    String process = templateEngine.process("passwordResetMail", context);
    SendEmailData sendEmailData =
        SendEmailData.builder()
            .body(process)
            .recipients(new String[] {emailTo})
            .subject("Password reset mail - Mantys.io")
            .build();
    emailService.sendEmail(sendEmailData);
  }

  private String getUniquePrimaryToken() {
    String generatedString = UUID.randomUUID().toString();
    while (userRepository.findIfUniquePrimaryTokenString(generatedString)) {
      generatedString = UUID.randomUUID().toString();
    }
    return generatedString;
  }

  private String getUniqueSecondaryToken() {
    String generatedString = UUID.randomUUID().toString();
    while (userRepository.findIfUniqueSecondaryTokenString(generatedString)) {
      generatedString = UUID.randomUUID().toString();
    }
    return generatedString;
  }

  /**
   * verifies the email and fetches corresponding user id from db
   *
   * @param mail mail to find out userId
   * @param sudo if sudo true, check if user account created for sudo , if not create and save on
   *     behalf of sudo and get userId else get userId of already created sudo user account
   * @return long userId
   */
  private User verifyEmailAndGetUser(String mail, boolean sudo) {
    Optional<User> userOptional =
        userRepository.findByEmail(mail, UtilFunctions.getDomainFromEmail(mail));
    if (userOptional.isEmpty()) {
      if (sudo) {
        return User.builder().email(mail).role(Role.SUDO).name("Mantys Sudo").build();
      } else {
        throw new Error("no account associated with this email");
      }
    } else {
      return userOptional.get();
    }
  }

  @Override
  public ResponseEntity<Object> refreshJwt(String token)
      throws NoSuchPaddingException,
      IllegalBlockSizeException,
      NoSuchAlgorithmException,
      BadPaddingException,
      InvalidKeyException, InvalidAlgorithmParameterException {
    DataSourceContextHolder.setTenant(getDomainFromRefreshToken(token));
    return tokenService.getJwtToken(token, null);
  }

  @Override
  public ResponseEntity<Object> logoutCurrentUser() {
    tokenService.setDeleted();
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<Object> checkResetLinkStatus(ResetPasswordData dto) {
    DataSourceContextHolder.setTenant(UtilFunctions.getDomainFromEmail(dto.email()));
    User user =
        userRepository
            .findByUserIdAndPrimaryAndSecondaryTokenAndNotExpired(
                dto.email(),
                dto.primaryToken(),
                dto.secondaryToken(),
                Timestamp.from(ZonedDateTime.now().toInstant()))
            .orElseThrow(() -> new Error("Invalid link used for reset"));
    JSONObject object = new JSONObject();
    object.put("name", user.getName());
    return ResponseEntity.ok(object);
  }
}
