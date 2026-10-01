package com.userservice.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // toUri() génère automatiquement le bon format : file:/// sur Linux, file:/ sur Windows
        String location = uploadPath.toUri().toString();

        // Assure un slash final (toUri() le fait déjà, mais sécurité)
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(0); // optionnel : désactive le cache en dev pour voir les changements immédiats

        log.info("Fichiers statiques servis depuis : {}", location);
    }
}
