package com.example.paymentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "PAYMENT_FAILED";
    public static final String PAYMENT_INITIATED_ROUTING_KEY = "PAYMENT_INITIATED";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "PAYMENT_COMPLETED";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
