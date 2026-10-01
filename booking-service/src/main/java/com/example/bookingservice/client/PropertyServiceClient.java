package com.example.bookingservice.client;

import com.example.bookingservice.dto.PropertyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@FeignClient(name = "property-service", url = "${app.services.property-service-url:http://localhost:8082}")
public interface PropertyServiceClient {

    @GetMapping("/api/properties/{propertyId}")
    PropertyResponse getPropertyById(@PathVariable Long propertyId);

    @PostMapping("/api/properties/{propertyId}/availability/block-confirmed")
    String confirmReservation(@RequestHeader("X-Auth-User-Id") Long userId,
                              @PathVariable Long propertyId,
                              @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                              @RequestParam("lockToken") String lockToken);

    @PostMapping("/api/properties/{propertyId}/availability/lock")
    java.util.Map<String, Object> blockDates(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable Long propertyId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    @PostMapping("/api/properties/{propertyId}/availability/release/{lockToken}")
    String releaseDates(@RequestHeader("X-Auth-User-Id") Long userId,
                        @PathVariable Long propertyId,
                        @PathVariable String lockToken);

    @GetMapping("/api/properties/{propertyId}/availability/price")
    java.util.Map<String, Object> getPrice(@PathVariable Long propertyId,
                                           @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);
}
