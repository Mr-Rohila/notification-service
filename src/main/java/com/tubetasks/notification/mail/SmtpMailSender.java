package com.tubetasks.notification.mail;

import com.tubetasks.notification.api.exception.EmailDeliveryException;
import com.tubetasks.notification.common.ServiceRequestIdFilter;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.MDC;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailSender {

    private final JavaMailSender mailSender;

    public SmtpMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(MailComposer.ComposedMail composedMail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(composedMail.template().fromEmail(), composedMail.template().fromName());
            helper.setTo(composedMail.recipientEmail());
            helper.setSubject(composedMail.template().subject());
            helper.setText(composedMail.textBody(), composedMail.htmlBody());
            String serviceRequestId = MDC.get(ServiceRequestIdFilter.MDC_KEY);
            if (serviceRequestId != null) {
                message.setHeader(ServiceRequestIdFilter.HEADER_NAME, serviceRequestId);
            }
            mailSender.send(message);
        } catch (Exception ex) {
            throw new EmailDeliveryException("Failed to send email", ex);
        }
    }
}
