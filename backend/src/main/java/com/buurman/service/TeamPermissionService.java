package com.buurman.service;

import com.buurman.domain.TeamMember;
import com.buurman.exception.CrossTeamAccessException;
import com.buurman.exception.InsufficientPermissionsException;
import com.buurman.exception.TeamMembershipNotFoundException;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamPermissionService {

    private final TeamMemberRepository teamMemberRepository;

    /**
     * Check if user has at least one of the specified roles in the given team.
     */
    public boolean hasRole(UUID userId, UUID teamId, String... roles) {
        return teamMemberRepository.findByUserIdAndTeamId(userId, teamId)
            .map(member -> Arrays.asList(roles).contains(member.getRole()))
            .orElse(false);
    }

    /**
     * Check if user is the owner of the given team.
     */
    public boolean isOwner(UUID userId, UUID teamId) {
        return teamMemberRepository.findByUserIdAndTeamId(userId, teamId)
            .map(TeamMember::isOwner)
            .orElse(false);
    }

    /**
     * Check if user is a member of the given team.
     */
    public boolean isMember(UUID userId, UUID teamId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    /**
     * Validate that the user is a member of the target team.
     * Throws CrossTeamAccessException if not.
     */
    public void validateTeamAccess(UserPrincipal principal, UUID teamId) {
        if (principal.getTeamId() == null) {
            throw new TeamMembershipNotFoundException("User is not a member of any team");
        }
        if (!principal.getTeamId().equals(teamId)) {
            throw new CrossTeamAccessException("Access denied: not a member of this team");
        }
    }

    /**
     * Validate that the user has at least one of the required roles.
     * Throws InsufficientPermissionsException if not.
     */
    public void validateRole(UserPrincipal principal, String... requiredRoles) {
        if (principal.getRole() == null) {
            throw new InsufficientPermissionsException("Access denied: no role assigned");
        }
        boolean hasRole = Arrays.asList(requiredRoles).contains(principal.getRole());
        if (!hasRole) {
            throw new InsufficientPermissionsException(
                "Access denied: requires one of " + Arrays.toString(requiredRoles));
        }
    }

    /**
     * Validate that the user is the owner of their active team.
     * Throws InsufficientPermissionsException if not.
     */
    public void validateOwnership(UserPrincipal principal) {
        if (!principal.isOwner()) {
            throw new InsufficientPermissionsException("Access denied: team owner required");
        }
    }

    /**
     * Validate both team access and role in one call.
     */
    public void validateTeamAndRole(UserPrincipal principal, UUID teamId, String... requiredRoles) {
        validateTeamAccess(principal, teamId);
        validateRole(principal, requiredRoles);
    }

    /**
     * Validate team access, role, and ownership in one call.
     */
    public void validateTeamRoleAndOwnership(UserPrincipal principal, UUID teamId, String... requiredRoles) {
        validateTeamAccess(principal, teamId);
        validateRole(principal, requiredRoles);
        validateOwnership(principal);
    }
}
