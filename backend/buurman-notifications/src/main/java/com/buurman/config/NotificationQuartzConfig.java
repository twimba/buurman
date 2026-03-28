package com.buurman.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.job.ContactFollowUpReminderJob;
import com.buurman.job.ContractExpiryCheckJob;
import com.buurman.job.NotificationOutboxJob;
import com.buurman.job.PaymentReminderCheckJob;
import com.buurman.job.RenewalReminderJob;
import com.buurman.util.SkipTestCoverage;

@Configuration
@SkipTestCoverage
public class NotificationQuartzConfig {

  // ── Notification Outbox ──────────────────────────────────────────────────

  @Bean
  public JobDetail notificationOutboxJobDetail() {
    return JobBuilder.newJob(NotificationOutboxJob.class)
        .withIdentity("notificationOutboxJob", "notification")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger notificationOutboxTrigger(
      JobDetail notificationOutboxJobDetail,
      @Value("${scheduling.notifications.outbox-push-cron}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(notificationOutboxJobDetail)
        .withIdentity("notificationOutboxTrigger", "notification")
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
  public Trigger contractExpiryCheckTrigger(
      JobDetail contractExpiryCheckJobDetail,
      @Value("${scheduling.notification-reminders.contract-expiry-cron}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(contractExpiryCheckJobDetail)
        .withIdentity("contractExpiryCheckTrigger", "scheduling")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }

  // ── Renewal Reminder Check ─────────────────────────────────────────────

  @Bean
  public JobDetail renewalReminderJobDetail() {
    return JobBuilder.newJob(RenewalReminderJob.class)
        .withIdentity("renewalReminderJob", "scheduling")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger renewalReminderTrigger(
      JobDetail renewalReminderJobDetail,
      @Value("${scheduling.notification-reminders.renewal-reminder-cron:0 0 8 * * ?}")
          String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(renewalReminderJobDetail)
        .withIdentity("renewalReminderTrigger", "scheduling")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }

  // ── Contact Follow-Up Reminder ─────────────────────────────────────────

  @Bean
  public JobDetail contactFollowUpReminderJobDetail() {
    return JobBuilder.newJob(ContactFollowUpReminderJob.class)
        .withIdentity("contactFollowUpReminderJob", "scheduling")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger contactFollowUpReminderTrigger(
      JobDetail contactFollowUpReminderJobDetail,
      @Value("${scheduling.notification-reminders.contact-follow-up-cron:0 0 8 * * ?}")
          String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(contactFollowUpReminderJobDetail)
        .withIdentity("contactFollowUpReminderTrigger", "scheduling")
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
  public Trigger paymentReminderCheckTrigger(
      JobDetail paymentReminderCheckJobDetail,
      @Value("${scheduling.notification-reminders.payment-reminder-cron}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(paymentReminderCheckJobDetail)
        .withIdentity("paymentReminderCheckTrigger", "scheduling")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }
}
