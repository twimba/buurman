package com.buurman.repository;

import com.buurman.domain.Team;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Team domain objects.
 * Implementation uses JOOQ for database access.
 */
public interface TeamRepository {

    /**
     * Find team by ID.
     */
    Optional<Team> findById(UUID id);

    /**
     * Save team (insert or update).
     */
    Team save(Team team);

    /**
     * Delete team by ID.
     */
    void deleteById(UUID id);

    /**
     * Find team by business ID.
     */
    Optional<Team> findByBusinessId(String businessId);
}
