package com.buurman.controller.backoffice;

import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
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
  public void pauseJob(String jobName, String group) {
    try {
      schedulerService.pauseJob(jobName, group);
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to pause job", e);
    }
  }

  @Override
  public void resumeJob(String jobName, String group) {
    try {
      schedulerService.resumeJob(jobName, group);
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to resume job", e);
    }
  }

  @Override
  public void triggerJob(String jobName, String group) {
    try {
      schedulerService.triggerJobNow(jobName, group);
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to trigger job", e);
    }
  }

  @Override
  public void rescheduleJob(
      String jobName, RescheduleRequest rescheduleRequest, String group) {
    try {
      schedulerService.rescheduleJob(jobName, group, rescheduleRequest.cronExpression());
    } catch (SchedulerException e) {
      throw new IllegalStateException("Failed to reschedule job", e);
    }
  }

  @Override
  public PageResponse<JobExecutionHistoryResponse> getHistory(
      List<String> jobName,
      String status,
      Integer page,
      Integer size,
      String sort,
      String direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, SortDirection.valueOf(direction));
    return schedulerService.getExecutionHistory(pageRequest, jobName, status);
  }
}
