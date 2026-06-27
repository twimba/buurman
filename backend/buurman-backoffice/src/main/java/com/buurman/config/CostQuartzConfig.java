package com.buurman.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.job.CostSnapshotJob;
import com.buurman.util.SkipTestCoverage;

@Configuration
@SkipTestCoverage
public class CostQuartzConfig {

  @Bean
  public JobDetail costSnapshotJobDetail() {
    return JobBuilder.newJob(CostSnapshotJob.class)
        .withIdentity("costSnapshotJob", "backoffice")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger costSnapshotTrigger(
      JobDetail costSnapshotJobDetail,
      @Value("${scheduling.cost.snapshot-cron:15 3 * * * ?}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(costSnapshotJobDetail)
        .withIdentity("costSnapshotTrigger", "backoffice")
        .withSchedule(
            CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionDoNothing())
        .build();
  }
}
