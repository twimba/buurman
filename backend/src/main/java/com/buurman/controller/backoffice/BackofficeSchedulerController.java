package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Optional;

import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.RescheduleRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.JobExecutionHistoryResponse;
import com.buurman.dto.response.backoffice.ScheduledJobResponse;
import com.buurman.generated.backoffice.api.BackofficeSchedulerApi;
import com.buurman.service.backoffice.BackofficeSchedulerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeSchedulerController implements BackofficeSchedulerApi {

  private final BackofficeSchedulerService schedulerService;

  @Override
  public List<ScheduledJobResponse> listJobs() {
    try {
      return schedulerService.listAllJobs();
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to list jobs", e);
    }
  }

  @Override
  public void pauseJob(String jobName, Optional<String> group) {
    try {
      schedulerService.pauseJob(jobName, group.orElse("scheduling"));
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to pause job", e);
    }
  }

  @Override
  public void resumeJob(String jobName, Optional<String> group) {
    try {
      schedulerService.resumeJob(jobName, group.orElse("scheduling"));
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to resume job", e);
    }
  }

  @Override
  public void triggerJob(String jobName, Optional<String> group) {
    try {
      schedulerService.triggerJobNow(jobName, group.orElse("scheduling"));
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to trigger job", e);
    }
  }

  @Override
  public void rescheduleJob(
      String jobName, RescheduleRequest rescheduleRequest, Optional<String> group) {
    try {
      schedulerService.rescheduleJob(
          jobName, group.orElse("scheduling"), rescheduleRequest.cronExpression());
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to reschedule job", e);
    }
  }

  @Override
  public PageResponse<JobExecutionHistoryResponse> getHistory(
      Optional<List<String>> jobName,
      Optional<String> status,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return schedulerService.getExecutionHistory(
        pageRequest, jobName.orElse(null), status.orElse(null));
  }
}
