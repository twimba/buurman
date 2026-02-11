package com.buurman.service.backoffice;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.UpdateTeamNameRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamResponse;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BackofficeTeamService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeTeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public BackofficeTeamService(TeamRepository teamRepository,
                                 TeamMemberRepository teamMemberRepository,
                                 UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BackofficeTeamResponse> listTeams(PageRequest pageRequest, String search) {
        PaginatedResult<Team> result = teamRepository.findAllPaginated(pageRequest, search);
        List<BackofficeTeamResponse> responses = result.items().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    @Transactional(readOnly = true)
    public BackofficeTeamResponse getTeam(String identifier) {
        Team team = teamRepository.findByIdentifierForBackoffice(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        return toResponse(team);
    }

    @Transactional
    public BackofficeTeamResponse updateTeamName(String identifier, UpdateTeamNameRequest request, BackofficePrincipal principal) {
        Team team = teamRepository.findByIdentifierForBackoffice(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        team.setName(request.name());
        teamRepository.save(team);

        log.info("Backoffice user {} updated team {} name to '{}'", principal.getEmail(), identifier, request.name());
        return toResponse(team);
    }

    @Transactional
    public void deleteTeam(String identifier, BackofficePrincipal principal) {
        Team team = teamRepository.findByIdentifierForBackoffice(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        teamRepository.softDeleteById(team.getId());
        log.info("Backoffice user {} soft-deleted team {} ({})", principal.getEmail(), identifier, team.getName());
    }

    private BackofficeTeamResponse toResponse(Team team) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
        long memberCount = members.size();

        String ownerEmail = members.stream()
                .filter(TeamMember::isOwner)
                .findFirst()
                .flatMap(owner -> userRepository.findById(owner.getUserId()))
                .map(User::getEmail)
                .orElse(null);

        return new BackofficeTeamResponse(
                team.getIdentifier(),
                team.getName(),
                memberCount,
                ownerEmail,
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
