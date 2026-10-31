package com.ecommerce.platform.messaging.email;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailMessagingConfiguration {

    @Bean
    DirectExchange emailExchange(@Value("${app.messaging.email-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    DirectExchange emailDeadLetterExchange(
            @Value("${app.messaging.email-dead-letter-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Queue emailNotificationQueue(
            @Value("${app.messaging.email-queue}") String name,
            @Value("${app.messaging.email-dead-letter-exchange}") String deadLetterExchange) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey("email.dead")
                .build();
    }

    @Bean
    Queue emailDeadLetterQueue(
            @Value("${app.messaging.email-dead-letter-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    Binding emailNotificationBinding(
            @Qualifier("emailNotificationQueue") Queue emailNotificationQueue,
            @Qualifier("emailExchange") DirectExchange emailExchange,
            @Value("${app.messaging.email-routing-key}") String routingKey) {
        return BindingBuilder.bind(emailNotificationQueue).to(emailExchange).with(routingKey);
    }

    @Bean
    Binding emailDeadLetterBinding(
            @Qualifier("emailDeadLetterQueue") Queue emailDeadLetterQueue,
            @Qualifier("emailDeadLetterExchange") DirectExchange emailDeadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue).to(emailDeadLetterExchange).with("email.dead");
    }

    @Bean
    Jackson2JsonMessageConverter emailMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory emailDeadLetterListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter emailMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(emailMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean
    RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter emailMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(emailMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
