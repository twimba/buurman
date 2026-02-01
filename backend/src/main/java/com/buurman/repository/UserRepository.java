package com.buurman.repository;

import com.buurman.domain.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User domain objects.
 * Implementation uses JOOQ for database access.
 */
public interface UserRepository {

    /**
     * Find user by ID.
     */
    Optional<User> findById(UUID id);

    /**
     * Save user (insert or update).
     */
    User save(User user);

    /**
     * Delete user by ID.
     */
    void deleteById(UUID id);

    /**
     * Find user by Keycloak ID.
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email.
     */
    boolean existsByEmail(String email);
}
