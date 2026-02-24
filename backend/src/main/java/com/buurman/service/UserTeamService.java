package com.buurman.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.UpdateUserProfileRequest;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.dto.response.UserTeamResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTeamService {

  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final PhoneVerificationService phoneVerificationService;
  private final PhoneNumberPolicyService phoneNumberPolicyService;

  public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
    User user = userRepository.getById(principal.getUserId());

    return toProfileResponse(user);
  }

  @Transactional
  public UserProfileResponse updateUserProfile(
      UpdateUserProfileRequest request, UserPrincipal principal) {
    User user = userRepository.getById(principal.getUserId());

    @Nullable String oldPhone = user.getPhone().orElse(null);
    @Nullable String newPhone = request.phone().orElse(null);

    // Validate phone against policy before saving
    if (newPhone != null && !newPhone.isBlank()) {
      phoneNumberPolicyService.validate(newPhone);
    }

    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setPhone(request.phone());

    // If phone changed or removed, clear verification
    boolean phoneChanged = !java.util.Objects.equals(oldPhone, newPhone);
    if (phoneChanged) {
      user.setPhoneVerifiedAt(Optional.empty());
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
        java.util.Objects.requireNonNull(user.getIdentifier()),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        user.getPhoneVerifiedAt().isPresent());
  }

  public List<UserTeamResponse> getUserTeams(UserPrincipal principal) {
    User user = userRepository.getById(principal.getUserId());

    List<TeamMember> memberships = teamMemberRepository.findAllByUserId(principal.getUserId());

    return memberships.stream()
        .map(
            membership -> {
              Team team = teamRepository.getById(membership.getTeamId());
              int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

              return new UserTeamResponse(
                  team.getIdentifier(),
                  team.getName(),
                  membership.getRole(),
                  membership.isOwner(),
                  user.getDefaultTeamId().map(id -> id.equals(team.getId())).orElse(false),
                  user.getActiveTeamId().map(id -> id.equals(team.getId())).orElse(false),
                  memberCount,
                  membership.getJoinedAt());
            })
        .toList();
  }

  @Transactional
  public UserTeamResponse switchTeam(String teamIdentifier, UserPrincipal principal) {
    User user = userRepository.getById(principal.getUserId());

    Team team = resolveTeam(teamIdentifier);

    // Verify user is member of target team
    TeamMember membership =
        teamMemberRepository.getByUserIdAndTeamId(principal.getUserId(), team.getId());

    // Update active team
    userRepository.updateActiveTeamId(principal.getUserId(), team.getId());

    int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

    return new UserTeamResponse(
        team.getIdentifier(),
        team.getName(),
        membership.getRole(),
        membership.isOwner(),
        user.getDefaultTeamId().map(id -> id.equals(team.getId())).orElse(false),
        true, // now active
        memberCount,
        membership.getJoinedAt());
  }

  @Transactional
  public UserTeamResponse setDefaultTeam(String teamIdentifier, UserPrincipal principal) {
    User user = userRepository.getById(principal.getUserId());

    Team team = resolveTeam(teamIdentifier);

    // Verify user is member of target team
    TeamMember membership =
        teamMemberRepository.getByUserIdAndTeamId(principal.getUserId(), team.getId());

    // Update default team
    userRepository.updateDefaultTeamId(principal.getUserId(), team.getId());

    int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

    return new UserTeamResponse(
        team.getIdentifier(),
        team.getName(),
        membership.getRole(),
        membership.isOwner(),
        true, // now default
        user.getActiveTeamId().map(id -> id.equals(team.getId())).orElse(false),
        memberCount,
        membership.getJoinedAt());
  }

  @Transactional
  public void leaveTeam(String teamIdentifier, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is member of target team
    TeamMember membership =
        teamMemberRepository.getByUserIdAndTeamId(principal.getUserId(), team.getId());

    // Cannot leave if owner
    if (membership.isOwner()) {
      throw new BusinessRuleException("Cannot leave team you own. Transfer ownership first.");
    }

    // Delete membership
    teamMemberRepository.softDeleteById(membership.getId());

    // If this was active or default team, update user
    User user = userRepository.getById(principal.getUserId());

    List<TeamMember> remainingMemberships =
        teamMemberRepository.findAllByUserId(principal.getUserId());

    if (user.getActiveTeamId().map(id -> id.equals(team.getId())).orElse(false)) {
      @Nullable UUID newActiveTeamId =
          remainingMemberships.isEmpty() ? null : remainingMemberships.get(0).getTeamId();
      userRepository.updateActiveTeamId(principal.getUserId(), newActiveTeamId);
    }

    if (user.getDefaultTeamId().map(id -> id.equals(team.getId())).orElse(false)) {
      @Nullable UUID newDefaultTeamId =
          remainingMemberships.isEmpty() ? null : remainingMemberships.get(0).getTeamId();
      userRepository.updateDefaultTeamId(principal.getUserId(), newDefaultTeamId);
    }
  }

  private Team resolveTeam(String teamIdentifier) {
    return teamRepository.getByIdentifier(teamIdentifier);
  }
}
