package com.buurman.service.backoffice;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.JobExecutionHistoryResponse;
import com.buurman.dto.response.backoffice.ScheduledJobResponse;
import com.buurman.repository.JobExecutionHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeSchedulerService {

  private final Scheduler scheduler;
  private final JobExecutionHistoryRepository executionHistoryRepository;

  public List<ScheduledJobResponse> listAllJobs() throws SchedulerException {
    List<ScheduledJobResponse> jobs = new ArrayList<>();

    for (String groupName : scheduler.getJobGroupNames()) {
      for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
        JobDetail detail = scheduler.getJobDetail(jobKey);
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

        if (triggers.isEmpty()) {
          jobs.add(
              new ScheduledJobResponse(
                  jobKey.getName(),
                  jobKey.getGroup(),
                  detail.getJobClass().getSimpleName(),
                  Optional.empty(),
                  Optional.empty(),
                  Optional.empty(),
                  Optional.empty(),
                  "NONE",
                  Optional.empty(),
                  Optional.empty()));
          continue;
        }

        for (Trigger trigger : triggers) {
          Trigger.TriggerState state = scheduler.getTriggerState(trigger.getKey());
          String triggerType;
          String scheduleExpression;

          if (trigger instanceof CronTrigger cronTrigger) {
            triggerType = "cron";
            scheduleExpression = cronTrigger.getCronExpression();
          } else if (trigger instanceof SimpleTrigger simpleTrigger) {
            triggerType = "simple";
            long intervalMs = simpleTrigger.getRepeatInterval();
            scheduleExpression = formatInterval(intervalMs);
          } else {
            triggerType = trigger.getClass().getSimpleName();
            scheduleExpression = "unknown";
          }

          jobs.add(
              new ScheduledJobResponse(
                  jobKey.getName(),
                  jobKey.getGroup(),
                  detail.getJobClass().getSimpleName(),
                  Optional.of(trigger.getKey().getName()),
                  Optional.of(trigger.getKey().getGroup()),
                  Optional.of(triggerType),
                  Optional.of(scheduleExpression),
                  state.name(),
                  Optional.ofNullable(formatDate(trigger.getNextFireTime())),
                  Optional.ofNullable(formatDate(trigger.getPreviousFireTime()))));
        }
      }
    }

    return jobs;
  }

  public void pauseJob(String jobName, String group) throws SchedulerException {
    scheduler.pauseJob(JobKey.jobKey(jobName, group));
    log.info("Paused job {}.{}", group, jobName);
  }

  public void resumeJob(String jobName, String group) throws SchedulerException {
    scheduler.resumeJob(JobKey.jobKey(jobName, group));
    log.info("Resumed job {}.{}", group, jobName);
  }

  public void triggerJobNow(String jobName, String group) throws SchedulerException {
    scheduler.triggerJob(JobKey.jobKey(jobName, group));
    log.info("Triggered job {}.{}", group, jobName);
  }

  public void rescheduleJob(String jobName, String group, String cronExpression)
      throws SchedulerException {
    JobKey jobKey = JobKey.jobKey(jobName, group);
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

    if (triggers.isEmpty()) {
      throw new IllegalStateException("Job %s.%s has no triggers".formatted(group, jobName));
    }

    Trigger existingTrigger = triggers.getFirst();
    if (!(existingTrigger instanceof CronTrigger)) {
      throw new IllegalArgumentException(
          "Job %s.%s is not a cron trigger — cannot reschedule with a cron expression"
              .formatted(group, jobName));
    }

    Trigger newTrigger =
        TriggerBuilder.newTrigger()
            .withIdentity(existingTrigger.getKey())
            .forJob(jobKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build();

    scheduler.rescheduleJob(existingTrigger.getKey(), newTrigger);
    log.info("Rescheduled job {}.{} with cron '{}'", group, jobName, cronExpression);
  }

  public PageResponse<JobExecutionHistoryResponse> getExecutionHistory(
      PageRequest pageRequest,
      @Nullable List<String> jobNameFilter,
      @Nullable String statusFilter) {
    return executionHistoryRepository.findAll(pageRequest, jobNameFilter, statusFilter);
  }

  private @Nullable String formatDate(@Nullable Date date) {
    if (date == null) {
      return null;
    }
    return Instant.ofEpochMilli(date.getTime()).atOffset(UTC).format(ISO_OFFSET_DATE_TIME);
  }

  private String formatInterval(long intervalMs) {
    if (intervalMs >= 3600000) {
      return "every " + (intervalMs / 3600000) + "h";
    } else if (intervalMs >= 60000) {
      return "every " + (intervalMs / 60000) + "m";
    } else if (intervalMs >= 1000) {
      return "every " + (intervalMs / 1000) + "s";
    }
    return "every " + intervalMs + "ms";
  }
}
