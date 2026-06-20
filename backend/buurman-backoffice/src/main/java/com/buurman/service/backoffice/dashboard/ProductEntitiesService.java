package com.buurman.service.backoffice.dashboard;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.ProductEntitiesResponse;
import com.buurman.repository.backoffice.DashboardAggregateRepository;

import lombok.RequiredArgsConstructor;

/** Product entities created today (and totals) across all teams. */
@Service
@RequiredArgsConstructor
public class ProductEntitiesService {

  private final DashboardAggregateRepository aggregateRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public ProductEntitiesResponse getProductEntities() {
    return new ProductEntitiesResponse(
        PanelStatus.LIVE,
        Optional.empty(),
        Optional.empty(),
        aggregateRepository.productEntityCounts());
  }
}
