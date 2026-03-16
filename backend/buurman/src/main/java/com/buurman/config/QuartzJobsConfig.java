package com.buurman.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.job.AutoExtensionJob;
import com.buurman.job.DatabaseMetricsRefreshJob;
import com.buurman.job.ExecutionHistoryCleanupJob;
import com.buurman.job.ImpersonationSessionCleanupJob;
import com.buurman.job.PaymentGenerationJob;
import com.buurman.job.RateLimitCleanupJob;
import com.buurman.job.ThumbnailBackfillJob;
import com.buurman.job.VerificationCodeCleanupJob;

@Configuration
public class QuartzJobsConfig {

  // ── Payment Generation ──────────────────────────────────────────────────

  @Bean
  public JobDetail paymentGenerationJobDetail() {
    return JobBuilder.newJob(PaymentGenerationJob.class)
        .withIdentity("paymentGenerationJob", "scheduling")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger paymentGenerationTrigger(
      JobDetail paymentGenerationJobDetail,
      @Value("${scheduling.payment-generation.cron}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(paymentGenerationJobDetail)
        .withIdentity("paymentGenerationTrigger", "scheduling")
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
  public Trigger databaseMetricsRefreshTrigger(
      JobDetail databaseMetricsRefreshJobDetail,
      @Value("${scheduling.metrics.metrics-push-cron}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(databaseMetricsRefreshJobDetail)
        .withIdentity("databaseMetricsRefreshTrigger", "metrics")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }

  // ── Thumbnail Backfill ──────────────────────────────────────────────────

  @Bean
  public JobDetail thumbnailBackfillJobDetail() {
    return JobBuilder.newJob(ThumbnailBackfillJob.class)
        .withIdentity("thumbnailBackfillJob", "media")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger thumbnailBackfillTrigger(
      JobDetail thumbnailBackfillJobDetail,
      @Value("${scheduling.thumbnail-backfill.cron}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(thumbnailBackfillJobDetail)
        .withIdentity("thumbnailBackfillTrigger", "media")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }

  // ── Verification Code Cleanup ───────────────────────────────────────

  @Bean
  public JobDetail verificationCodeCleanupJobDetail() {
    return JobBuilder.newJob(VerificationCodeCleanupJob.class)
        .withIdentity("verificationCodeCleanupJob", "system")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger verificationCodeCleanupTrigger(JobDetail verificationCodeCleanupJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(verificationCodeCleanupJobDetail)
        .withIdentity("verificationCodeCleanupTrigger", "system")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 * * ?"))
        .build();
  }

  // ── Rate Limit Bucket Cleanup ──────────────────────────────────────────

  @Bean
  public JobDetail rateLimitCleanupJobDetail() {
    return JobBuilder.newJob(RateLimitCleanupJob.class)
        .withIdentity("rateLimitCleanupJob", "system")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger rateLimitCleanupTrigger(JobDetail rateLimitCleanupJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(rateLimitCleanupJobDetail)
        .withIdentity("rateLimitCleanupTrigger", "system")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 */6 * * ?"))
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

  // ── Auto Extension (Contract Renewals) ─────────────────────────────────

  @Bean
  public JobDetail autoExtensionJobDetail() {
    return JobBuilder.newJob(AutoExtensionJob.class)
        .withIdentity("autoExtensionJob", "scheduling")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger autoExtensionTrigger(
      JobDetail autoExtensionJobDetail,
      @Value("${scheduling.auto-extension.cron:0 0 2 * * ?}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(autoExtensionJobDetail)
        .withIdentity("autoExtensionTrigger", "scheduling")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }

  // ── Impersonation Session Cleanup ───────────────────────────────────────

  @Bean
  public JobDetail impersonationSessionCleanupJobDetail() {
    return JobBuilder.newJob(ImpersonationSessionCleanupJob.class)
        .withIdentity("impersonationSessionCleanupJob", "system")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger impersonationSessionCleanupTrigger(
      JobDetail impersonationSessionCleanupJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(impersonationSessionCleanupJobDetail)
        .withIdentity("impersonationSessionCleanupTrigger", "system")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 */5 * * * ?"))
        .build();
  }
}
