package com.pearrity.mantys.mail;

import com.pearrity.mantys.domain.MantysMailProperties;
import com.pearrity.mantys.domain.SendEmailData;
import com.pearrity.mantys.domain.utils.Constants;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.Properties;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

  @Autowired private AwsSecretsService awsSecretsService;

  private JavaMailSenderImpl getJavaMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setPort(Constants.GMAIL_SMTP_PORT);
    mailSender.setHost(Constants.MailHost);
    MantysMailProperties mantysMailProperties =
        awsSecretsService.getSecret(Constants.MANTYS_MAIL_KEY, MantysMailProperties.class);
    if (mantysMailProperties == null)
      throw new RuntimeException(
          "problem getting secrets from aws for " + Constants.MANTYS_MAIL_KEY);
    mailSender.setUsername(mantysMailProperties.getUsername());
    mailSender.setPassword(mantysMailProperties.getPassword());
    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    return mailSender;
  }

  @Override
  @Async
  public void sendEmail(SendEmailData sendEmailData) {
    try {
      JavaMailSenderImpl javaMailSender = getJavaMailSender();
      final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
      final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, "UTF-8");
      message.setFrom(Objects.requireNonNull(javaMailSender.getUsername()));
      message.setTo(sendEmailData.recipients());
      if (sendEmailData.cc() != null) {
        message.setCc(sendEmailData.cc());
      }
      if (sendEmailData.bcc() != null) {
        message.setBcc(sendEmailData.bcc());
      }
      message.setSubject(sendEmailData.subject());
      message.setText(sendEmailData.body(), true);
      javaMailSender.send(mimeMessage);
    } catch (Exception e) {
      log.error("error {}", e.getMessage());
    }
  }

  private String sendEmailWithAttachments(SendEmailData sendEmailData) {
    return null;
  }
}
