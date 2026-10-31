package com.ecommerce.platform.config;

import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@TestConfiguration(proxyBeanMethods = false)
public class TestMailConfiguration {

    @Bean
    @Primary
    JavaMailSender testMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doAnswer(invocation -> {
            throw new AssertionError("Tests must not send email");
        }).when(mailSender).send(any(MimeMessage.class));
        return mailSender;
    }
}
