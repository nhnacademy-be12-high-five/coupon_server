package com.nhnacademy.coupon_server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    // 이 아래 부분은 user에서 작성되어야 함. user는 message provider이고, 쿠폰은 consumer 이기 때문에, json을 변환하면 됨.
//    public static final String COUPON_EXCHANGE = "coupon_exchange";
//    public static final String COUPON_QUEUE = "coupon_queue";
//    public static final String COUPON_ROUTING_KEY = "coupon_key";
//
//    @Bean
//    public TopicExchange exchange() {
//        return new TopicExchange(COUPON_EXCHANGE);
//    }
//
//    @Bean
//    public Queue queue() {
//        return new Queue(COUPON_QUEUE, false);
//    }
//
//    @Bean
//    public Binding binding(Queue queue, TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(COUPON_ROUTING_KEY);
//    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        // json -> java 객체로 변환
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory); // yml을 통해 rabbitMQ 서버에 연결
        factory.setMessageConverter(jackson2JsonMessageConverter()); // json으로 들어왔을 때 java 객체로 변환함.
        factory.setAdviceChain(RetryInterceptorBuilder
                .stateless() // 실패 이유는 알려주지 않음
                .maxAttempts(100) // 100번까지 재시도
                .backOffOptions(3_600_000L, 1.0, 3_600_000L) // 1시간 간격으로
                .build());
        return factory;
    }
}
