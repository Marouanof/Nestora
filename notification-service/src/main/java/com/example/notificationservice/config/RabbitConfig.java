package com.example.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String NOTIFICATION_QUEUE = "q.notifications";
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    
    // Web2 Routing Keys
    public static final String PROPERTY_APPROVED = "PROPERTY_APPROVED";
    public static final String PROPERTY_REJECTED = "PROPERTY_REJECTED";
    public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with("PAYMENT_SUCCESS");
    }

    @Bean
    public Binding paymentFailedBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with("PAYMENT_FAILED");
    }

    @Bean
    public Binding paymentCompletedBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with("PAYMENT_COMPLETED");
    }

    @Bean
    public Binding propertyApprovedBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with(PROPERTY_APPROVED);
    }

    @Bean
    public Binding propertyRejectedBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with(PROPERTY_REJECTED);
    }

    @Bean
    public Binding bookingCancelledBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bookingExchange).with(BOOKING_CANCELLED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        // ID Neutre pour les tests et la prod
        idClassMapping.put("BookingPaymentEvent", com.example.notificationservice.dto.BookingPaymentEvent.class);
        idClassMapping.put("PropertyApprovalEvent", com.example.notificationservice.dto.PropertyApprovalEvent.class);
        idClassMapping.put("BookingCancellationEvent", com.example.notificationservice.dto.BookingCancellationEvent.class);

        // Compatibilité avec les classes existantes
        idClassMapping.put("com.example.bookingservice.messaging.BookingPaymentEvent", com.example.notificationservice.dto.BookingPaymentEvent.class);
        idClassMapping.put("com.example.notificationservice.dto.BookingPaymentEvent", com.example.notificationservice.dto.BookingPaymentEvent.class);
        idClassMapping.put("com.example.notificationservice.dto.PropertyApprovalEvent", com.example.notificationservice.dto.PropertyApprovalEvent.class);
        idClassMapping.put("com.example.notificationservice.dto.BookingCancellationEvent", com.example.notificationservice.dto.BookingCancellationEvent.class);
        idClassMapping.put("com.example.bookingservice.messaging.BookingCancellationEvent", com.example.notificationservice.dto.BookingCancellationEvent.class);
        idClassMapping.put("com.propertyservice.propertyservice.dto.PropertyApprovalEvent", com.example.notificationservice.dto.PropertyApprovalEvent.class);

        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("*"); // Plus prudent pour les tests
        
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
