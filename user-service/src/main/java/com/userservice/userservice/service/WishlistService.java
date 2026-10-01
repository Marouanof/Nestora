package com.userservice.userservice.service;

import com.userservice.userservice.client.PropertyServiceClient;
import com.userservice.userservice.dto.PropertyResponseDTO;
import com.userservice.userservice.dto.WishlistResponse;
import com.userservice.userservice.entity.WishlistItem;
import com.userservice.userservice.exception.InvalidActionException;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserService userService;
    private final PropertyServiceClient propertyServiceClient;

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        requireUser(userId);
        List<WishlistItem> items = wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return toResponse(items);
    }

    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long propertyId) {
        requireUser(userId);
        if (wishlistItemRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            throw new InvalidActionException("Cette propriété est déjà dans vos favoris");
        }
        wishlistItemRepository.save(new WishlistItem(userId, propertyId));
        return getWishlist(userId);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long propertyId) {
        requireUser(userId);
        if (!wishlistItemRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            throw new InvalidActionException("Cette propriété n'est pas dans vos favoris");
        }
        wishlistItemRepository.deleteByUserIdAndPropertyId(userId, propertyId);
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long propertyId) {
        return wishlistItemRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }

    private void requireUser(Long userId) {
        userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private WishlistResponse toResponse(List<WishlistItem> items) {
        List<Long> propertyIds = items.stream().map(WishlistItem::getPropertyId).toList();
        // Enrichissement best-effort via property-service : si indisponible, on renvoie au moins les IDs
        List<PropertyResponseDTO> properties = propertyIds.stream()
                .map(propertyServiceClient::getPropertyById)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new WishlistResponse(propertyIds, properties);
    }
}
