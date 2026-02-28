package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Optional;

import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.JobExecutionHistoryResponse;
import com.buurman.dto.response.backoffice.ScheduledJobResponse;
import com.buurman.service.backoffice.BackofficeSchedulerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/scheduler")
@Tag(name = "Backoffice - Scheduler", description = "Job and scheduler management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeSchedulerController {

  private final BackofficeSchedulerService schedulerService;

  @Operation(
      summary = "List all jobs",
      description = "Get all configured Quartz jobs with trigger details")
  @GetMapping("/jobs")
  public List<ScheduledJobResponse> listJobs() throws SchedulerException {
    return schedulerService.listAllJobs();
  }

  @Operation(summary = "Pause a job", description = "Pause a scheduled job by name and group")
  @PostMapping("/jobs/{jobName}/pause")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void pauseJob(
      @Parameter(description = "Quartz job name") @PathVariable String jobName,
      @Parameter(description = "Quartz job group", example = "scheduling")
          @RequestParam(defaultValue = "scheduling")
          String group)
      throws SchedulerException {
    schedulerService.pauseJob(jobName, group);
  }

  @Operation(summary = "Resume a job", description = "Resume a paused job by name and group")
  @PostMapping("/jobs/{jobName}/resume")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resumeJob(
      @Parameter(description = "Quartz job name") @PathVariable String jobName,
      @Parameter(description = "Quartz job group", example = "scheduling")
          @RequestParam(defaultValue = "scheduling")
          String group)
      throws SchedulerException {
    schedulerService.resumeJob(jobName, group);
  }

  @Operation(summary = "Trigger a job now", description = "Immediately trigger a job execution")
  @PostMapping("/jobs/{jobName}/trigger")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void triggerJob(
      @Parameter(description = "Quartz job name") @PathVariable String jobName,
      @Parameter(description = "Quartz job group", example = "scheduling")
          @RequestParam(defaultValue = "scheduling")
          String group)
      throws SchedulerException {
    schedulerService.triggerJobNow(jobName, group);
  }

  @Operation(
      summary = "Reschedule a cron job",
      description = "Update the cron expression for a job")
  @PostMapping("/jobs/{jobName}/reschedule")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rescheduleJob(
      @Parameter(description = "Quartz job name") @PathVariable String jobName,
      @Parameter(description = "Quartz job group", example = "scheduling")
          @RequestParam(defaultValue = "scheduling")
          String group,
      @RequestBody RescheduleRequest request)
      throws SchedulerException {
    schedulerService.rescheduleJob(jobName, group, request.cronExpression());
  }

  public record RescheduleRequest(String cronExpression) {}

  @Operation(
      summary = "Get execution history",
      description = "Get paginated job execution history with optional filters")
  @GetMapping("/history")
  public PageResponse<JobExecutionHistoryResponse> getHistory(
      @Parameter(description = "Filter by job name(s)") @RequestParam
          Optional<List<String>> jobName,
      @Parameter(description = "Filter by execution status") @RequestParam Optional<String> status,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size", example = "25") @RequestParam(defaultValue = "25")
          Integer size,
      @Parameter(description = "Sort field name", example = "createdAt") @RequestParam
          Optional<String> sort,
      @Parameter(description = "Sort direction", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          SortDirection direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return schedulerService.getExecutionHistory(
        pageRequest, jobName.orElse(null), status.orElse(null));
  }
}
