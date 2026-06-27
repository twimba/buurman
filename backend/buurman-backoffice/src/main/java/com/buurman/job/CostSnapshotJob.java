package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.buurman.service.backoffice.cost.CostService;
import com.buurman.service.backoffice.cost.FxRateService;
import com.buurman.util.SkipTestCoverage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Daily snapshot of every cost provider, for trend/MoM history. */
@Component
@Slf4j
@SkipTestCoverage
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class CostSnapshotJob implements Job {

  private final FxRateService fxRateService;
  private final CostService costService;

  @Override
  public void execute(JobExecutionContext context) {
    // Refresh + store live FX rates first so the snapshot normalizes at the current market rate.
    log.info("Refreshing FX rates before cost snapshot");
    fxRateService.refresh();
    log.info("Capturing cost snapshot");
    costService.snapshotNow();
  }
}
