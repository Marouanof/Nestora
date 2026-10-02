package com.example.bookingservice.client;

import com.example.bookingservice.dto.UserProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", url = "${app.services.user-service-url:http://localhost:8081}")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserProfileDTO getUserProfile(@PathVariable("userId") Long userId);

    /**
     * Reviews reçues par un utilisateur (notées 1-5 par d'autres utilisateurs,
     * 1 review par séjour). Sert le scoring de risque tenant (bad_reviews).
     */
    @GetMapping("/api/users/{userId}/reviews")
    List<Map<String, Object>> getUserReviews(@PathVariable("userId") Long userId);
}
