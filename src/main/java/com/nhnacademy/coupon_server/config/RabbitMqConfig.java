package com.nhnacademy.coupon_server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    public static final String COUPON_WELCOME_QUEUE = "high-five-coupon-welcome-queue";
    public static final String COUPON_ISSUE_QUEUE = "high-five-coupon-issue-queue";
    public static final String COUPON_DEAD_LETTER_EXCHANGE = "high-five-coupon-dead-letter-exchange";
    public static final String COUPON_DEAD_LETTER_QUEUE = "high-five-coupon-dead-letter-queue";
    public static final String COUPON_DEAD_LETTER_ROUTING_KEY = "high-five.dead.letter";

    @Bean
    public Queue WelcomeCouponQueue() {
        return new Queue(COUPON_WELCOME_QUEUE, true);
    }

    @Bean
    public Queue issuesCouponQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", COUPON_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", COUPON_DEAD_LETTER_QUEUE);
        return new Queue(COUPON_ISSUE_QUEUE, true, false, false, args);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(COUPON_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(COUPON_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(COUPON_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}