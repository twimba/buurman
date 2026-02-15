package com.buurman.job;

import com.buurman.service.NotificationSchedulerService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
