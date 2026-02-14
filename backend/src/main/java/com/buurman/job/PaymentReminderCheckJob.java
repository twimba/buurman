package com.buurman.job;

import com.buurman.service.NotificationSchedulerService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class PaymentReminderCheckJob implements Job {

    private final NotificationSchedulerService notificationSchedulerService;

    public PaymentReminderCheckJob(NotificationSchedulerService notificationSchedulerService) {
        this.notificationSchedulerService = notificationSchedulerService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            notificationSchedulerService.checkPaymentReminders();
        } catch (Exception e) {
            throw new JobExecutionException("Payment reminder check failed", e);
        }
    }
}
