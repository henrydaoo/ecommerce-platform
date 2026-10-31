package com.ecommerce.platform.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

   
                                                                        
                   
   
@Component
@Slf4j
class RetryingMailSender {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String fromAddress;

    RetryingMailSender(JavaMailSender mailSender, SpringTemplateEngine templateEngine,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    @Retryable(retryFor = MailException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void send(String recipient, String subject, String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank())
                helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, context), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Unable to construct email", ex);
        }
    }

    @Recover
    public void recover(MailException exception, String recipient, String subject, String template,
            Map<String, Object> variables) {
        log.error("Email permanently failed after retries: recipient={}, subject={}", recipient, subject, exception);
        throw exception;
    }
}
