package com.buurman.config;

import com.buurman.config.models.DemoDataProperties;
import com.buurman.config.models.NotificationOutboxProperties;
import com.buurman.job.ContractExpiryCheckJob;
import com.buurman.job.DatabaseMetricsRefreshJob;
import com.buurman.job.DemoDataRegenerationJob;
import com.buurman.job.ExecutionHistoryCleanupJob;
import com.buurman.job.NotificationOutboxJob;
import com.buurman.job.PaymentGenerationJob;
import com.buurman.job.PaymentReminderCheckJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzJobsConfig {

    // ── Notification Outbox (existing) ──────────────────────────────────────

    @Bean
    public JobDetail notificationOutboxJobDetail() {
        return JobBuilder.newJob(NotificationOutboxJob.class)
                .withIdentity("notificationOutboxJob", "notification")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger notificationOutboxTrigger(JobDetail notificationOutboxJobDetail,
                                              NotificationOutboxProperties notificationOutboxProperties) {
        return TriggerBuilder.newTrigger()
                .forJob(notificationOutboxJobDetail)
                .withIdentity("notificationOutboxTrigger", "notification")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(notificationOutboxProperties.pollIntervalMs())
                        .repeatForever())
                .build();
    }

    // ── Payment Generation ──────────────────────────────────────────────────

    @Bean
    public JobDetail paymentGenerationJobDetail() {
        return JobBuilder.newJob(PaymentGenerationJob.class)
                .withIdentity("paymentGenerationJob", "scheduling")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger paymentGenerationTrigger(JobDetail paymentGenerationJobDetail,
                                             @Value("${scheduling.payment-generation.cron}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(paymentGenerationJobDetail)
                .withIdentity("paymentGenerationTrigger", "scheduling")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    // ── Contract Expiry Check ───────────────────────────────────────────────

    @Bean
    public JobDetail contractExpiryCheckJobDetail() {
        return JobBuilder.newJob(ContractExpiryCheckJob.class)
                .withIdentity("contractExpiryCheckJob", "scheduling")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger contractExpiryCheckTrigger(JobDetail contractExpiryCheckJobDetail,
                                               @Value("${scheduling.notification-reminders.contract-expiry-cron}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(contractExpiryCheckJobDetail)
                .withIdentity("contractExpiryCheckTrigger", "scheduling")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    // ── Payment Reminder Check ──────────────────────────────────────────────

    @Bean
    public JobDetail paymentReminderCheckJobDetail() {
        return JobBuilder.newJob(PaymentReminderCheckJob.class)
                .withIdentity("paymentReminderCheckJob", "scheduling")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger paymentReminderCheckTrigger(JobDetail paymentReminderCheckJobDetail,
                                                @Value("${scheduling.notification-reminders.payment-reminder-cron}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(paymentReminderCheckJobDetail)
                .withIdentity("paymentReminderCheckTrigger", "scheduling")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    // ── Database Metrics Refresh ────────────────────────────────────────────

    @Bean
    public JobDetail databaseMetricsRefreshJobDetail() {
        return JobBuilder.newJob(DatabaseMetricsRefreshJob.class)
                .withIdentity("databaseMetricsRefreshJob", "metrics")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger databaseMetricsRefreshTrigger(JobDetail databaseMetricsRefreshJobDetail,
                                                  @Value("${scheduling.metrics.fixed-rate-ms}") long intervalMs) {
        return TriggerBuilder.newTrigger()
                .forJob(databaseMetricsRefreshJobDetail)
                .withIdentity("databaseMetricsRefreshTrigger", "metrics")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(intervalMs)
                        .repeatForever())
                .build();
    }

    // ── Demo Data Regeneration ──────────────────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "buurman.demo.enabled", havingValue = "true")
    public JobDetail demoDataRegenerationJobDetail() {
        return JobBuilder.newJob(DemoDataRegenerationJob.class)
                .withIdentity("demoDataRegenerationJob", "demo")
                .storeDurably()
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "buurman.demo.enabled", havingValue = "true")
    public Trigger demoDataRegenerationTrigger(JobDetail demoDataRegenerationJobDetail,
                                                DemoDataProperties demoDataProperties) {
        return TriggerBuilder.newTrigger()
                .forJob(demoDataRegenerationJobDetail)
                .withIdentity("demoDataRegenerationTrigger", "demo")
                .withSchedule(CronScheduleBuilder.cronSchedule(demoDataProperties.cron()))
                .build();
    }

    // ── Execution History Cleanup ───────────────────────────────────────────

    @Bean
    public JobDetail executionHistoryCleanupJobDetail() {
        return JobBuilder.newJob(ExecutionHistoryCleanupJob.class)
                .withIdentity("executionHistoryCleanupJob", "system")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger executionHistoryCleanupTrigger(JobDetail executionHistoryCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(executionHistoryCleanupJobDetail)
                .withIdentity("executionHistoryCleanupTrigger", "system")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))
                .build();
    }
}
