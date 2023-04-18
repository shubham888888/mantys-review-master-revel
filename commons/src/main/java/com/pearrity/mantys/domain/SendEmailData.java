package com.pearrity.mantys.domain;

import lombok.Builder;

@Builder
public record SendEmailData(
    String[] recipients,
    String senderEmail,
    String body,
    String subject,
    String attachments,
    String[] cc,
    String[] bcc) {}
