package com.buurman.repository;

import com.buurman.domain.TeamMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TeamMember domain objects.
 * Implementation uses JOOQ for database access.
 */
public interface TeamMemberRepository {

    /**
     * Find team member by ID.
     */
    Optional<TeamMember> findById(UUID id);

    /**
     * Save team member (insert or update).
     */
    TeamMember save(TeamMember teamMember);

    /**
     * Delete team member by ID.
     */
    void deleteById(UUID id);

    /**
     * Find team member by user ID.
     */
    Optional<TeamMember> findByUserId(UUID userId);

    /**
     * Find all team members by team ID.
     */
    List<TeamMember> findByTeamId(UUID teamId);

    /**
     * Check if team member exists by team ID and user ID.
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
}
