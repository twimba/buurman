package com.buurman.service.backoffice.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.ScheduledJobResponse;
import com.buurman.dto.response.backoffice.dashboard.SchedulerHealthResponse;
import com.buurman.dto.response.backoffice.dashboard.SchedulerHealthResponse.JobTile;
import com.buurman.service.backoffice.BackofficeSchedulerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Quartz scheduler health: counts by trigger state plus a per-job tile grid. */
@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerHealthService {

  private final BackofficeSchedulerService schedulerService;

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public SchedulerHealthResponse getSchedulerHealth() {
    List<ScheduledJobResponse> jobs;
    try {
      jobs = schedulerService.listAllJobs();
    } catch (SchedulerException e) {
      log.warn("Could not read scheduler state", e);
      return new SchedulerHealthResponse(
          PanelStatus.PREVIEW,
          Optional.of("Scheduler unavailable"),
          Optional.empty(),
          0,
          0,
          0,
          0,
          0,
          List.of());
    }

    long normal = 0;
    long paused = 0;
    long error = 0;
    long blocked = 0;
    List<JobTile> tiles = new ArrayList<>();
    for (ScheduledJobResponse job : jobs) {
      switch (job.triggerState()) {
        case "NORMAL" -> normal++;
        case "PAUSED" -> paused++;
        case "ERROR" -> error++;
        case "BLOCKED" -> blocked++;
        default -> {
          // COMPLETE / NONE — not counted in the four health buckets.
        }
      }
      tiles.add(
          new JobTile(
              job.jobName(),
              job.jobGroup(),
              job.triggerState(),
              job.nextFireTime(),
              "/scheduler?jobName=" + job.jobName()));
    }

    return new SchedulerHealthResponse(
        PanelStatus.LIVE,
        Optional.empty(),
        Optional.empty(),
        jobs.size(),
        normal,
        paused,
        error,
        blocked,
        tiles);
  }
}
