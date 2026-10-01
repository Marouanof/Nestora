package com.userservice.userservice.repository;

import com.userservice.userservice.entity.TenantPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantPreferenceRepository extends JpaRepository<TenantPreference, Long> {

    Optional<TenantPreference> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
