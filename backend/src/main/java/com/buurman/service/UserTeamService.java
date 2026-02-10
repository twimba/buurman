package com.buurman.service;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.UpdateUserProfileRequest;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.dto.response.UserTeamResponse;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserTeamService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public UserTeamService(UserRepository userRepository, TeamRepository teamRepository,
                          TeamMemberRepository teamMemberRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileResponse(
            user.getIdentifier(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone()
        );
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user = userRepository.save(user);

        return new UserProfileResponse(
            user.getIdentifier(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone()
        );
    }

    public List<UserTeamResponse> getUserTeams(UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findAllByUserId(principal.getUserId());

        return memberships.stream()
            .map(membership -> {
                Team team = teamRepository.findById(membership.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found"));
                int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

                return new UserTeamResponse(
                    team.getIdentifier(),
                    team.getName(),
                    membership.getRole(),
                    membership.isOwner(),
                    team.getId().equals(user.getDefaultTeamId()),
                    team.getId().equals(user.getActiveTeamId()),
                    memberCount,
                    membership.getJoinedAt()
                );
            })
            .toList();
    }

    @Transactional
    public UserTeamResponse switchTeam(String teamIdentifier, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = resolveTeam(teamIdentifier);

        // Verify user is member of target team
        TeamMember membership = teamMemberRepository.findByUserIdAndTeamId(principal.getUserId(), team.getId())
            .orElseThrow(() -> new RuntimeException("Not a member of this team"));

        // Update active team
        userRepository.updateActiveTeamId(principal.getUserId(), team.getId());

        int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

        return new UserTeamResponse(
            team.getIdentifier(),
            team.getName(),
            membership.getRole(),
            membership.isOwner(),
            team.getId().equals(user.getDefaultTeamId()),
            true, // now active
            memberCount,
            membership.getJoinedAt()
        );
    }

    @Transactional
    public UserTeamResponse setDefaultTeam(String teamIdentifier, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = resolveTeam(teamIdentifier);

        // Verify user is member of target team
        TeamMember membership = teamMemberRepository.findByUserIdAndTeamId(principal.getUserId(), team.getId())
            .orElseThrow(() -> new RuntimeException("Not a member of this team"));

        // Update default team
        userRepository.updateDefaultTeamId(principal.getUserId(), team.getId());

        int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

        return new UserTeamResponse(
            team.getIdentifier(),
            team.getName(),
            membership.getRole(),
            membership.isOwner(),
            true, // now default
            team.getId().equals(user.getActiveTeamId()),
            memberCount,
            membership.getJoinedAt()
        );
    }

    @Transactional
    public void leaveTeam(String teamIdentifier, UserPrincipal principal) {
        Team team = resolveTeam(teamIdentifier);

        // Verify user is member of target team
        TeamMember membership = teamMemberRepository.findByUserIdAndTeamId(principal.getUserId(), team.getId())
            .orElseThrow(() -> new RuntimeException("Not a member of this team"));

        // Cannot leave if owner
        if (membership.isOwner()) {
            throw new RuntimeException("Cannot leave team you own. Transfer ownership first.");
        }

        // Delete membership
        teamMemberRepository.softDeleteById(membership.getId());

        // If this was active or default team, update user
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamMember> remainingMemberships = teamMemberRepository.findAllByUserId(principal.getUserId());

        if (team.getId().equals(user.getActiveTeamId())) {
            UUID newActiveTeamId = remainingMemberships.isEmpty() ? null : remainingMemberships.get(0).getTeamId();
            userRepository.updateActiveTeamId(principal.getUserId(), newActiveTeamId);
        }

        if (team.getId().equals(user.getDefaultTeamId())) {
            UUID newDefaultTeamId = remainingMemberships.isEmpty() ? null : remainingMemberships.get(0).getTeamId();
            userRepository.updateDefaultTeamId(principal.getUserId(), newDefaultTeamId);
        }
    }

    private Team resolveTeam(String teamIdentifier) {
        return teamRepository.findByIdentifier(teamIdentifier)
            .orElseThrow(() -> new RuntimeException("Team not found"));
    }
}
