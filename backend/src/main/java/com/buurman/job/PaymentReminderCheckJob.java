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
public class PaymentReminderCheckJob implements Job {

  private final NotificationSchedulerService notificationSchedulerService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      notificationSchedulerService.checkPaymentReminders();
    } catch (Exception e) {
      throw new JobExecutionException("Payment reminder check failed", e);
    }
  }
}
