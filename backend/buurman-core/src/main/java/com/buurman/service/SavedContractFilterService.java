package com.buurman.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.SavedContractFilter;
import com.buurman.domain.Sid;
import com.buurman.dto.request.CreateSavedContractFilterRequest;
import com.buurman.dto.response.SavedContractFilterResponse;
import com.buurman.repository.SavedContractFilterRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavedContractFilterService {

  private final SavedContractFilterRepository repository;

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
    SavedContractFilter saved =
        repository.save(
            SavedContractFilter.builder()
                .teamId(principal.requireTeamId())
                .userId(principal.getUserId())
                .name(request.name())
                .criteria(request.criteria())
                .build());
    return toResponse(saved);
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
