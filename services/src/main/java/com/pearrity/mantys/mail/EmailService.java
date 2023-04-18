package com.pearrity.mantys.mail;

import com.pearrity.mantys.domain.SendEmailData;

public interface EmailService {

  void sendEmail(SendEmailData sendEmailData);
}
