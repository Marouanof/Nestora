package com.example.notificationservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Propage le secret gateway sur les appels inter-services (user, booking, property).
// Sans lui, les routes /internal/** répondent 401 et les emails ne partent jamais.
@Configuration
public class FeignSecretConfig {

    @Value("${app.gateway.shared-secret}")
    private String gatewaySharedSecret;

    @Bean
    public RequestInterceptor gatewaySecretInterceptor() {
        return (RequestTemplate template) -> template.header("X-Gateway-Secret", gatewaySharedSecret);
    }
}
