package com.buurman.service.backoffice;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.User;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.UpdateTeamNameRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.DataCounts;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.FinancialSnapshot;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.MemberInfo;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse.SettingsInfo;
import com.buurman.dto.response.backoffice.BackofficeTeamResponse;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.repository.backoffice.BackofficeTeamStatsRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeTeamService {

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final BackofficeTeamStatsRepository statsRepository;

  @Transactional(readOnly = true)
  public PageResponse<BackofficeTeamResponse> listTeams(
      PageRequest pageRequest, @Nullable String search) {
    PaginatedResult<Team> result = teamRepository.findAllPaginated(pageRequest, search);
    List<BackofficeTeamResponse> responses =
        result.items().stream().map(this::toListResponse).toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional(readOnly = true)
  public BackofficeTeamDetailResponse getTeam(String identifier) {
    Team team = teamRepository.getByIdentifierForBackoffice(identifier);

    // Members + batch user lookup
    List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
    List<UUID> userIds = members.stream().map(TeamMember::getUserId).toList();
    Map<UUID, User> usersById =
        userRepository.findByIds(userIds).stream().collect(toMap(User::getId, identity()));

    List<MemberInfo> memberInfos =
        members.stream()
            .map(
                m -> {
                  User u = usersById.get(m.getUserId());
                  return new MemberInfo(
                      Optional.ofNullable(u != null ? u.getEmail() : null),
                      Optional.ofNullable(u != null ? u.getFirstName() : null),
                      Optional.ofNullable(u != null ? u.getLastName() : null),
                      m.getRole(),
                      m.isOwner(),
                      m.getJoinedAt(),
                      u != null && u.getDisabledAt() != null);
                })
            .toList();

    // Data counts
    DataCounts dataCounts = statsRepository.countEntitiesForTeam(team.getId());

    // Financial snapshot — derive currency from actual contract data
    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(team.getId());
    var activeRent = statsRepository.sumActiveRentForTeam(team.getId());
    String currency =
        activeRent.getValue() != null ? activeRent.getValue() : prefs.getDefaultCurrency();

    FinancialSnapshot financialSnapshot =
        new FinancialSnapshot(
            Optional.ofNullable(activeRent.getKey()),
            Optional.ofNullable(currency),
            statsRepository.propertyStatusDistribution(team.getId()),
            statsRepository.propertyCategoryDistribution(team.getId()),
            statsRepository.contractStatusDistribution(team.getId()),
            statsRepository.paymentStatusDistribution(team.getId()));

    // Settings
    SettingsInfo settingsInfo =
        new SettingsInfo(
            Optional.of(prefs.getPaymentsAheadCount()),
            prefs.isAutoGenerationEnabled(),
            Optional.ofNullable(prefs.getDefaultCurrency()),
            Optional.ofNullable(prefs.getDefaultCountry()),
            Optional.ofNullable(prefs.getTimezone()),
            Optional.ofNullable(prefs.getDateFormat()),
            Optional.ofNullable(prefs.getFiscalYearStartMonth()));

    return new BackofficeTeamDetailResponse(
        team.getIdentifier(),
        team.getName(),
        team.getCreatedAt(),
        Optional.ofNullable(team.getUpdatedAt()),
        memberInfos,
        dataCounts,
        financialSnapshot,
        Optional.of(settingsInfo));
  }

  @Transactional
  public BackofficeTeamResponse updateTeamName(
      String identifier, UpdateTeamNameRequest request, BackofficePrincipal principal) {
    Team team = teamRepository.getByIdentifierForBackoffice(identifier);

    team.setName(request.name());
    teamRepository.save(team);

    log.info(
        "Backoffice user {} updated team {} name to '{}'",
        principal.getEmail(),
        identifier,
        request.name());
    return toListResponse(team);
  }

  @Transactional
  public void deleteTeam(String identifier, BackofficePrincipal principal) {
    Team team = teamRepository.getByIdentifierForBackoffice(identifier);

    teamRepository.softDeleteById(team.getId());
    log.info(
        "Backoffice user {} soft-deleted team {} ({})",
        principal.getEmail(),
        identifier,
        team.getName());
  }

  private BackofficeTeamResponse toListResponse(Team team) {
    List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
    long memberCount = members.size();

    String ownerEmail =
        members.stream()
            .filter(TeamMember::isOwner)
            .findFirst()
            .flatMap(owner -> userRepository.findById(owner.getUserId()))
            .map(User::getEmail)
            .orElse(null);

    return new BackofficeTeamResponse(
        team.getIdentifier(),
        team.getName(),
        memberCount,
        Optional.ofNullable(ownerEmail),
        team.getCreatedAt(),
        Optional.ofNullable(team.getUpdatedAt()));
  }
}
