package com.nhnacademy.coupon_server.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue WelcomeCouponQueue() {
        return new Queue("coupon-welcome-queue", true);
    }

    @Bean
    public Queue issuesCouponQueue() {
        return new Queue("issues-coupon-queue", true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}