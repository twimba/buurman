package com.buurman.service;

import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamSettings;
import com.buurman.domain.User;
import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.request.UpdateTeamRequest;
import com.buurman.dto.request.UpdateTeamSettingsRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.mapper.TeamMapper;
import com.buurman.repository.*;
import com.buurman.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final EmailService emailService;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                      TeamInvitationRepository invitationRepository, UserRepository userRepository,
                      TeamMapper teamMapper, EmailService emailService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
        this.emailService = emailService;
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
    @PreAuthorize("hasRole('TEAM_ADMIN')")
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

        // Send invitation email
        String inviterName = principal.getName();
        emailService.sendTeamInvitation(invitation, inviterName, team.getName());

        return teamMapper.toInvitationResponse(invitation, team.getName(), inviterName);
    }

    public InvitationResponse getInvitation(String token) {
        TeamInvitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Fetch team and inviter details for the response
        Team team = teamRepository.findById(invitation.getTeamId())
            .orElseThrow(() -> new RuntimeException("Team not found"));

        String inviterName = "Team Admin";
        if (invitation.getInvitedBy() != null) {
            User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
            if (inviter != null) {
                inviterName = inviter.getFirstName() + " " + inviter.getLastName();
            }
        }

        return teamMapper.toInvitationResponse(invitation, team.getName(), inviterName);
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

        // Check user not already member of this specific team
        if (teamMemberRepository.existsByTeamIdAndUserId(invitation.getTeamId(), principal.getUserId())) {
            throw new RuntimeException("User already member of this team");
        }

        // Create team member
        User user = userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        TeamMember member = new TeamMember();
        member.setTeamId(invitation.getTeamId());
        member.setUserId(user.getId());
        member.setRole(invitation.getRole());
        member.setOwner(false); // Invited members are not owners
        member.setInvitedAt(invitation.getInvitedAt());
        member.setInvitedBy(invitation.getInvitedBy());
        member.setJoinedAt(Instant.now());
        teamMemberRepository.save(member);

        // Mark invitation as accepted
        invitation.setAcceptedAt(Instant.now());
        invitation.setAcceptedBy(principal.getUserId());
        invitationRepository.save(invitation);

        // If this is the user's first team, set it as default and active
        if (user.getDefaultTeamId() == null) {
            user.setDefaultTeamId(invitation.getTeamId());
        }
        if (user.getActiveTeamId() == null) {
            user.setActiveTeamId(invitation.getTeamId());
        }
        userRepository.save(user);

        // Notify inviter
        User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
        Team team = teamRepository.findById(invitation.getTeamId()).orElse(null);
        if (inviter != null && team != null) {
            emailService.sendInvitationAccepted(inviter, user, team);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void removeMember(UUID teamId, UUID memberId, UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        TeamMember member = teamMemberRepository.findByIdAndTeamId(memberId, teamId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        // Cannot remove self
        if (member.getUserId().equals(principal.getUserId())) {
            throw new RuntimeException("Cannot remove yourself");
        }

        teamMemberRepository.softDeleteById(member.getId());
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public TeamMemberResponse updateMemberRole(UUID teamId, UUID memberId,
                                               UpdateMemberRoleRequest request,
                                               UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        TeamMember member = teamMemberRepository.findByIdAndTeamId(memberId, teamId)
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
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request, UserPrincipal principal) {
        // Verify user is admin of this team
        if (!teamId.equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("Access denied");
        }

        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));

        team.setName(request.name());
        team.setUpdatedBy(principal.getUserId());
        team = teamRepository.save(team);

        long memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
        return teamMapper.toResponse(team, memberCount);
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
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

        // Update regional settings
        if (request.regional() != null) {
            TeamSettings.RegionalSettings regionalSettings = settings.getRegional();
            if (regionalSettings == null) {
                regionalSettings = new TeamSettings.RegionalSettings();
            }
            if (request.regional().defaultCurrency() != null) {
                regionalSettings.setDefaultCurrency(request.regional().defaultCurrency());
            }
            if (request.regional().defaultCountry() != null) {
                regionalSettings.setDefaultCountry(request.regional().defaultCountry());
            }
            if (request.regional().timezone() != null) {
                regionalSettings.setTimezone(request.regional().timezone());
            }
            if (request.regional().dateFormat() != null) {
                regionalSettings.setDateFormat(request.regional().dateFormat());
            }
            if (request.regional().fiscalYearStartMonth() != null) {
                regionalSettings.setFiscalYearStartMonth(request.regional().fiscalYearStartMonth());
            }
            settings.setRegional(regionalSettings);
        }

        team.setSettings(settings);
        team.setUpdatedBy(principal.getUserId());
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

    @Transactional
    public TeamMemberResponse transferOwnership(UUID teamId, UUID newOwnerId, UserPrincipal principal) {
        // Verify user is owner of this team
        if (!teamId.equals(principal.getTeamId()) || !principal.isOwner()) {
            throw new RuntimeException("Only team owner can transfer ownership");
        }

        // Cannot transfer to self
        if (newOwnerId.equals(principal.getUserId())) {
            throw new RuntimeException("Cannot transfer ownership to yourself");
        }

        // Find new owner's membership
        TeamMember newOwnerMember = teamMemberRepository.findByUserIdAndTeamId(newOwnerId, teamId)
            .orElseThrow(() -> new RuntimeException("User is not a member of this team"));

        // Find current owner's membership
        TeamMember currentOwnerMember = teamMemberRepository.findByUserIdAndTeamId(principal.getUserId(), teamId)
            .orElseThrow(() -> new RuntimeException("Current owner membership not found"));

        // Transfer ownership
        currentOwnerMember.setOwner(false);
        teamMemberRepository.save(currentOwnerMember);

        newOwnerMember.setOwner(true);
        newOwnerMember.setRole("TEAM_ADMIN"); // Owner must be admin
        newOwnerMember = teamMemberRepository.save(newOwnerMember);

        // Update team's created_by to new owner
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setCreatedBy(newOwnerId);
        team.setUpdatedBy(principal.getUserId());
        teamRepository.save(team);

        User newOwnerUser = userRepository.findById(newOwnerId).orElseThrow();
        return teamMapper.toMemberResponse(newOwnerMember, newOwnerUser, principal.getUserId());
    }
}
