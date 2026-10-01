package com.userservice.userservice.controller;

import com.userservice.userservice.dto.WishlistResponse;
import com.userservice.userservice.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist(
            @RequestHeader("X-Auth-User-Id") String userIdHeader) {

        return ResponseEntity.ok(wishlistService.getWishlist(Long.valueOf(userIdHeader)));
    }

    @PostMapping("/{propertyId}")
    public ResponseEntity<WishlistResponse> addToWishlist(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @PathVariable Long propertyId) {

        WishlistResponse response = wishlistService.addToWishlist(Long.valueOf(userIdHeader), propertyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void> removeFromWishlist(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @PathVariable Long propertyId) {

        wishlistService.removeFromWishlist(Long.valueOf(userIdHeader), propertyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{propertyId}")
    public ResponseEntity<Boolean> isWishlisted(
            @RequestHeader("X-Auth-User-Id") String userIdHeader,
            @PathVariable Long propertyId) {

        return ResponseEntity.ok(wishlistService.isWishlisted(Long.valueOf(userIdHeader), propertyId));
    }
}
