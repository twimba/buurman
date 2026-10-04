package com.buurman.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.SavedContractFilter;
import com.buurman.domain.Sid;
import com.buurman.dto.request.CreateSavedContractFilterRequest;
import com.buurman.dto.response.SavedContractFilterResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.SavedContractFilterRepository;
import com.buurman.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavedContractFilterService {

  /** Landlords don't need more than a handful of saved views; this also bounds table growth. */
  private static final int MAX_SAVED_FILTERS_PER_USER = 20;

  /**
   * {@code criteria} is stored as JSONB with no column-level width limit. This caps the serialized
   * payload to a few KB — generous for any realistic filter (a handful of statuses/dates/ids) — to
   * stop an arbitrarily large blob from being accepted.
   */
  private static final int MAX_CRITERIA_JSON_LENGTH = 4096;

  private final SavedContractFilterRepository repository;
  private final ObjectMapper objectMapper;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<SavedContractFilterResponse> list(UserPrincipal principal) {
    return repository
        .findByTeamIdAndUserId(principal.requireTeamId(), principal.getUserId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public SavedContractFilterResponse create(
      CreateSavedContractFilterRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();

    if (repository.findByTeamIdAndUserId(teamId, userId).size() >= MAX_SAVED_FILTERS_PER_USER) {
      throw new BadRequestException(
          "You can save at most "
              + MAX_SAVED_FILTERS_PER_USER
              + " contract filters. Delete one before adding another.");
    }

    if (serializedLength(request.criteria()) > MAX_CRITERIA_JSON_LENGTH) {
      throw new BadRequestException(
          "Criteria is too large (max " + MAX_CRITERIA_JSON_LENGTH + " bytes of JSON)");
    }

    SavedContractFilter saved =
        repository.save(
            SavedContractFilter.builder()
                .teamId(teamId)
                .userId(userId)
                .name(request.name())
                .criteria(request.criteria())
                .build());
    return toResponse(saved);
  }

  private int serializedLength(Object criteria) {
    try {
      return objectMapper.writeValueAsBytes(criteria).length;
    } catch (JsonProcessingException e) {
      throw new BadRequestException("Criteria could not be parsed as JSON");
    }
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public void delete(Sid identifier, UserPrincipal principal) {
    repository.softDeleteByIdentifierAndTeamIdAndUserId(
        identifier, principal.requireTeamId(), principal.getUserId());
  }

  private SavedContractFilterResponse toResponse(SavedContractFilter filter) {
    return new SavedContractFilterResponse(
        filter.getIdentifier().orElseThrow(),
        filter.getName(),
        filter.getCriteria(),
        filter.getCreatedAt());
  }
}
