package com.buurman.service;

import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamSettings;
import com.buurman.domain.User;
import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.request.UpdateTeamSettingsRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.mapper.TeamMapper;
import com.buurman.repository.*;
import com.buurman.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                      TeamInvitationRepository invitationRepository, UserRepository userRepository,
                      TeamMapper teamMapper) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
    }

    public TeamResponse getCurrentTeam(UserPrincipal principal) {
        Team team = teamRepository.findById(principal.getTeamId())
            .orElseThrow(() -> new RuntimeException("Team not found"));

        long memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
        return teamMapper.toResponse(team, memberCount);
    }

    public List<TeamMemberResponse> getTeamMembers(UUID teamId, UserPrincipal principal) {
        // Verify user belongs to this team
        if (!teamId.equals(principal.getTeamId())) {
            throw new RuntimeException("Access denied");
        }

        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        return members.stream()
                // TODO: very inefficient, causes N+1 query problem
            .map(member -> teamMapper.toMemberResponse(member, userRepository.findById(member.getUserId()).orElseThrow(), principal.getUserId()))
            .toList();
    }

    @Transactional
    public InvitationResponse createInvitation(UUID teamId, CreateInvitationRequest request,
                                               UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        // Create invitation
        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeamId(team.getId());
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation.setInvitedBy(principal.getUserId());
        invitation.setInvitedAt(Instant.now());

        invitation = invitationRepository.save(invitation);
        return teamMapper.toInvitationResponse(invitation);
    }

    public InvitationResponse getInvitation(String token) {
        TeamInvitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (invitation.getAcceptedAt() != null) {
            throw new RuntimeException("Invitation already accepted");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Invitation expired");
        }

        return teamMapper.toInvitationResponse(invitation);
    }

    @Transactional
    public void acceptInvitation(String token, UserPrincipal principal) {
        TeamInvitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Validate
        if (invitation.getAcceptedAt() != null) {
            throw new RuntimeException("Invitation already accepted");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Invitation expired");
        }
        if (!invitation.getEmail().equalsIgnoreCase(principal.getEmail())) {
            throw new RuntimeException("Invitation email does not match");
        }

        // Check user not already in a team
        if (teamMemberRepository.findByUserId(principal.getUserId()).isPresent()) {
            throw new RuntimeException("User already belongs to a team");
        }

        // Create team member
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        TeamMember member = new TeamMember();
        member.setTeamId(invitation.getTeamId());
        member.setUserId(user.getId());
        member.setRole(invitation.getRole());
        member.setInvitedAt(invitation.getInvitedAt());
        member.setInvitedBy(invitation.getInvitedBy());
        member.setJoinedAt(Instant.now());
        teamMemberRepository.save(member);

        // Mark invitation as accepted
        invitation.setAcceptedAt(Instant.now());
        invitation.setAcceptedBy(principal.getUserId());
        invitationRepository.save(invitation);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID memberId, UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        TeamMember member = teamMemberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        // Cannot remove self
        if (member.getUserId().equals(principal.getUserId())) {
            throw new RuntimeException("Cannot remove yourself");
        }

        teamMemberRepository.deleteById(member.getId());
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(UUID teamId, UUID memberId,
                                               UpdateMemberRoleRequest request,
                                               UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        TeamMember member = teamMemberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        // Cannot change own role
        if (member.getUserId().equals(principal.getUserId())) {
            throw new RuntimeException("Cannot change your own role");
        }

        member.setRole(request.role());
        member = teamMemberRepository.save(member);

        User user = userRepository.findById(member.getUserId()).orElseThrow();

        return teamMapper.toMemberResponse(member, user, principal.getUserId());
    }

    @Transactional
    public TeamResponse updateTeamSettings(UUID teamId, UpdateTeamSettingsRequest request,
                                          UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Update settings
        TeamSettings settings = team.getSettings();
        if (settings == null) {
            settings = new TeamSettings();
        }

        // Update payment settings
        if (request.payments() != null) {
            TeamSettings.PaymentSettings paymentSettings = new TeamSettings.PaymentSettings();
            paymentSettings.setPaymentsAheadCount(request.payments().paymentsAheadCount());
            paymentSettings.setAutoGenerationEnabled(request.payments().autoGenerationEnabled());
            settings.setPayments(paymentSettings);
        }

        team.setSettings(settings);
        team = teamRepository.save(team);

        long memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
        return teamMapper.toResponse(team, memberCount);
    }

    public TeamSettings getTeamSettings(UUID teamId, UserPrincipal principal) {
        // Verify user belongs to this team
        if (!teamId.equals(principal.getTeamId())) {
            throw new RuntimeException("Access denied");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamSettings settings = team.getSettings();
        if (settings == null) {
            settings = new TeamSettings(); // Return defaults
        }

        return settings;
    }
}
