package com.buurman.service.backoffice.dashboard;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.dto.request.backoffice.dashboard.SaveDashboardLayoutRequest;
import com.buurman.dto.response.backoffice.dashboard.DashboardLayoutResponse;
import com.buurman.dto.response.backoffice.dashboard.PanelPlacement;
import com.buurman.repository.backoffice.DashboardLayoutRepository;
import com.buurman.security.BackofficePrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Per-user persisted bento layout. Empty placement list = "use frontend defaults". */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardLayoutService {

  private static final TypeReference<List<PanelPlacement>> LAYOUT_TYPE = new TypeReference<>() {};

  private final DashboardLayoutRepository layoutRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public DashboardLayoutResponse getLayout(BackofficePrincipal principal) {
    List<PanelPlacement> panels =
        layoutRepository
            .findLayoutJson(principal.userId())
            .map(this::deserialize)
            .orElse(List.of());
    return new DashboardLayoutResponse(panels);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public DashboardLayoutResponse saveLayout(
      BackofficePrincipal principal, SaveDashboardLayoutRequest request) {
    List<PanelPlacement> panels = request.panels() != null ? request.panels() : List.of();
    layoutRepository.upsertLayout(principal.userId(), serialize(panels));
    return new DashboardLayoutResponse(panels);
  }

  /** Layout is non-essential UI state: a corrupt row degrades to defaults rather than 500-ing. */
  private List<PanelPlacement> deserialize(String json) {
    try {
      return objectMapper.readValue(json, LAYOUT_TYPE);
    } catch (JsonProcessingException e) {
      log.warn("Discarding corrupt dashboard layout JSON; falling back to defaults", e);
      return List.of();
    }
  }

  private String serialize(List<PanelPlacement> panels) {
    try {
      return objectMapper.writeValueAsString(panels);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not serialize dashboard layout", e);
    }
  }
}
