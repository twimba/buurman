package com.buurman.service.backoffice.dashboard;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.PreviewPanelResponse;

/**
 * Panels whose data source is not wired yet. Each returns a PREVIEW envelope with a CTA — never
 * fabricated numbers. They graduate to dedicated services as their sources land (billing,
 * Prometheus, PostHog, geo, log ring buffer).
 */
@Service
public class DashboardPreviewService {

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public PreviewPanelResponse business() {
    return preview("business", "Connect billing to enable MRR & churn");
  }

  private static PreviewPanelResponse preview(String panel, String cta) {
    return new PreviewPanelResponse(PanelStatus.PREVIEW, panel, Optional.of(cta), Optional.empty());
  }
}
