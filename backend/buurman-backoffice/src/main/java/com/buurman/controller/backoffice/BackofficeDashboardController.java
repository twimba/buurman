package com.buurman.controller.backoffice;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.backoffice.dashboard.SaveDashboardLayoutRequest;
import com.buurman.dto.request.backoffice.dashboard.SnoozeActionItemRequest;
import com.buurman.dto.response.backoffice.BackofficeDashboardResponse;
import com.buurman.dto.response.backoffice.dashboard.ActionQueueResponse;
import com.buurman.dto.response.backoffice.dashboard.DashboardLayoutResponse;
import com.buurman.dto.response.backoffice.dashboard.FunnelResponse;
import com.buurman.dto.response.backoffice.dashboard.GeoResponse;
import com.buurman.dto.response.backoffice.dashboard.LatencyHeatmapResponse;
import com.buurman.dto.response.backoffice.dashboard.LiveTailResponse;
import com.buurman.dto.response.backoffice.dashboard.PreviewPanelResponse;
import com.buurman.dto.response.backoffice.dashboard.ProductEntitiesResponse;
import com.buurman.dto.response.backoffice.dashboard.PropertyLocationsResponse;
import com.buurman.dto.response.backoffice.dashboard.SchedulerHealthResponse;
import com.buurman.dto.response.backoffice.dashboard.StatusStripResponse;
import com.buurman.dto.response.backoffice.dashboard.TopTeamsResponse;
import com.buurman.generated.backoffice.api.BackofficeDashboardApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeDashboardService;
import com.buurman.service.backoffice.dashboard.ActionQueueService;
import com.buurman.service.backoffice.dashboard.ActivationFunnelService;
import com.buurman.service.backoffice.dashboard.DashboardLayoutService;
import com.buurman.service.backoffice.dashboard.DashboardPreviewService;
import com.buurman.service.backoffice.dashboard.GeoService;
import com.buurman.service.backoffice.dashboard.LatencyHeatmapService;
import com.buurman.service.backoffice.dashboard.LiveTailService;
import com.buurman.service.backoffice.dashboard.ProductEntitiesService;
import com.buurman.service.backoffice.dashboard.SchedulerHealthService;
import com.buurman.service.backoffice.dashboard.StatusStripService;
import com.buurman.service.backoffice.dashboard.TopTeamsService;

import lombok.RequiredArgsConstructor;

/**
 * Backoffice Dashboard (mission-control) endpoints. Always available to {@code BACKOFFICE_ADMIN};
 * unlike the tenant app, backoffice has no per-feature flags. The legacy {@code /stats} endpoint is
 * retained for backwards compatibility.
 */
@RestController
@RequiredArgsConstructor
public class BackofficeDashboardController implements BackofficeDashboardApi {

  private final BackofficeDashboardService backofficeDashboardService;
  private final StatusStripService statusStripService;
  private final ActionQueueService actionQueueService;
  private final ProductEntitiesService productEntitiesService;
  private final ActivationFunnelService activationFunnelService;
  private final TopTeamsService topTeamsService;
  private final SchedulerHealthService schedulerHealthService;
  private final DashboardPreviewService previewService;
  private final GeoService geoService;
  private final LatencyHeatmapService latencyHeatmapService;
  private final LiveTailService liveTailService;
  private final DashboardLayoutService layoutService;

  @Override
  public BackofficeDashboardResponse getDashboardStats() {
    return backofficeDashboardService.getStats();
  }

  @Override
  public StatusStripResponse getDashboardStatusStrip() {
    return statusStripService.getStatusStrip();
  }

  @Override
  public ActionQueueResponse getDashboardActionQueue() {
    return actionQueueService.getActionQueue(SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public ActionQueueResponse snoozeDashboardActionItem(
      SnoozeActionItemRequest snoozeActionItemRequest) {
    return actionQueueService.snooze(
        SecurityUtils.getBackofficePrincipal(),
        snoozeActionItemRequest.itemKey(),
        snoozeActionItemRequest.hours());
  }

  @Override
  public ProductEntitiesResponse getDashboardProductEntities() {
    return productEntitiesService.getProductEntities();
  }

  @Override
  public FunnelResponse getDashboardFunnel() {
    return activationFunnelService.getFunnel();
  }

  @Override
  public TopTeamsResponse getDashboardTopTeams() {
    return topTeamsService.getTopTeams();
  }

  @Override
  public SchedulerHealthResponse getDashboardSchedulerHealth() {
    return schedulerHealthService.getSchedulerHealth();
  }

  @Override
  public PreviewPanelResponse getDashboardBusiness() {
    return previewService.business();
  }

  @Override
  public PreviewPanelResponse getDashboardCostWatch() {
    return previewService.costWatch();
  }

  @Override
  public LatencyHeatmapResponse getDashboardLatencyHeatmap() {
    return latencyHeatmapService.getLatencyHeatmap();
  }

  @Override
  public GeoResponse getDashboardGeo() {
    return geoService.getGeo();
  }

  @Override
  public PropertyLocationsResponse getDashboardPropertyLocations() {
    return geoService.getPropertyLocations();
  }

  @Override
  public LiveTailResponse getDashboardLiveTail(Optional<String> level) {
    return liveTailService.getLiveTail(level);
  }

  @Override
  public DashboardLayoutResponse getDashboardLayout() {
    return layoutService.getLayout(SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public DashboardLayoutResponse saveDashboardLayout(
      SaveDashboardLayoutRequest saveDashboardLayoutRequest) {
    return layoutService.saveLayout(
        SecurityUtils.getBackofficePrincipal(), saveDashboardLayoutRequest);
  }
}
