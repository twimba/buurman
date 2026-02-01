package com.buurman.repository;

import com.buurman.domain.TeamInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TeamInvitation domain objects.
 * Implementation uses JOOQ for database access.
 */
public interface TeamInvitationRepository {

    /**
     * Find team invitation by ID.
     */
    Optional<TeamInvitation> findById(UUID id);

    /**
     * Save team invitation (insert or update).
     */
    TeamInvitation save(TeamInvitation teamInvitation);

    /**
     * Delete team invitation by ID.
     */
    void deleteById(UUID id);

    /**
     * Find team invitation by token.
     */
    Optional<TeamInvitation> findByToken(String token);

    /**
     * Find all pending invitations for a team (not yet accepted).
     */
    List<TeamInvitation> findByTeamIdAndAcceptedAtIsNull(UUID teamId);
}
