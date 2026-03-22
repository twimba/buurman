package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.NotificationSchedulerService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ContactFollowUpReminderJob implements Job {

  private final NotificationSchedulerService notificationSchedulerService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      notificationSchedulerService.checkContactFollowUpReminders();
    } catch (Exception e) {
      throw new JobExecutionException("Contact follow-up reminder check failed", e);
    }
  }
}
