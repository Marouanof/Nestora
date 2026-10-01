package com.userservice.userservice.repository;

import com.userservice.userservice.entity.OwnerListingPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerListingPreferenceRepository extends JpaRepository<OwnerListingPreference, Long> {

    Optional<OwnerListingPreference> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
