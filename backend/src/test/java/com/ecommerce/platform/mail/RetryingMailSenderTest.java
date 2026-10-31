package com.ecommerce.platform.mail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.EnableRetry;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

                                                                                                    
class RetryingMailSenderTest {

    @Test
    void mockedSmtpFailureIsRetriedAndPropagated() {
        Logger logger = (Logger) LoggerFactory.getLogger(RetryingMailSender.class);
        Level previousLevel = logger.getLevel();
        boolean previousAdditivity = logger.isAdditive();
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        logger.setLevel(Level.ERROR);
        logger.setAdditive(false);
        logger.addAppender(logAppender);

        try {
            try (AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext(TestConfig.class)) {
                RetryingMailSender sender = context.getBean(RetryingMailSender.class);
                assertThatThrownBy(
                        () -> sender.send("customer@example.com", "subject", "welcome", Map.of()))
                        .isInstanceOf(MailSendException.class);
                verify(context.getBean(JavaMailSender.class), times(3)).send(any(MimeMessage.class));
            }
            assertThat(logAppender.list)
                    .singleElement()
                    .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.ERROR));
        } finally {
            logger.detachAppender(logAppender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditivity);
            logAppender.stop();
        }
    }

    @Configuration
    @EnableRetry
    static class TestConfig {
        @Bean
        JavaMailSender mailSender() {
            JavaMailSender sender = mock(JavaMailSender.class);
            when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
            doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));
            return sender;
        }

        @Bean
        SpringTemplateEngine templateEngine() {
            SpringTemplateEngine engine = new SpringTemplateEngine();
            StringTemplateResolver resolver = new StringTemplateResolver();
            resolver.setTemplateMode("HTML");
            engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean
        RetryingMailSender retryingMailSender(JavaMailSender sender, SpringTemplateEngine engine) {
            return new RetryingMailSender(sender, engine, "");
        }
    }
}
