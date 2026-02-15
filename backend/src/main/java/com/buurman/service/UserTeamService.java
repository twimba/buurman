package com.buurman.service;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.UpdateUserProfileRequest;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.dto.response.UserTeamResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserTeamService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final PhoneNumberPolicyService phoneNumberPolicyService;

    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        String oldPhone = user.getPhone();
        String newPhone = request.phone();

        // Validate phone against policy before saving
        if (newPhone != null && !newPhone.isBlank()) {
            phoneNumberPolicyService.validate(newPhone);
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(newPhone);

        // If phone changed or removed, clear verification
        boolean phoneChanged = !java.util.Objects.equals(oldPhone, newPhone);
        if (phoneChanged) {
            user.setPhoneVerifiedAt(null);
        }

        user = userRepository.save(user);

        // If phone changed to a new (non-null) value, trigger verification
        if (phoneChanged && newPhone != null && !newPhone.isBlank()) {
            phoneVerificationService.sendVerificationCode(user.getId());
        }

        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
            user.getIdentifier(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getPhoneVerifiedAt() != null
        );
    }

    public List<UserTeamResponse> getUserTeams(UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findAllByUserId(principal.getUserId());

        return memberships.stream()
            .map(membership -> {
                Team team = teamRepository.findById(membership.getTeamId())
                    .orElseThrow(() -> new NotFoundException("Team not found"));
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
            .orElseThrow(() -> new NotFoundException("User not found"));

        Team team = resolveTeam(teamIdentifier);

        // Verify user is member of target team
        TeamMember membership = teamMemberRepository.findByUserIdAndTeamId(principal.getUserId(), team.getId())
            .orElseThrow(() -> new NotFoundException("Not a member of this team"));

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
            .orElseThrow(() -> new NotFoundException("User not found"));

        Team team = resolveTeam(teamIdentifier);

        // Verify user is member of target team
        TeamMember membership = teamMemberRepository.findByUserIdAndTeamId(principal.getUserId(), team.getId())
            .orElseThrow(() -> new NotFoundException("Not a member of this team"));

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
            .orElseThrow(() -> new NotFoundException("Not a member of this team"));

        // Cannot leave if owner
        if (membership.isOwner()) {
            throw new BusinessRuleException("Cannot leave team you own. Transfer ownership first.");
        }

        // Delete membership
        teamMemberRepository.softDeleteById(membership.getId());

        // If this was active or default team, update user
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

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
            .orElseThrow(() -> new NotFoundException("Team not found"));
    }
}
