package com.buurman.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.job.TakeoutCleanupJob;
import com.buurman.util.SkipTestCoverage;

@Configuration
@SkipTestCoverage
public class TakeoutQuartzConfig {

  @Bean
  public JobDetail takeoutCleanupJobDetail() {
    return JobBuilder.newJob(TakeoutCleanupJob.class)
        .withIdentity("takeoutCleanupJob", "system")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger takeoutCleanupTrigger(JobDetail takeoutCleanupJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(takeoutCleanupJobDetail)
        .withIdentity("takeoutCleanupTrigger", "system")
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 4 * * ?"))
        .build();
  }
}
