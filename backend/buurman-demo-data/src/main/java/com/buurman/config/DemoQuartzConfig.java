package com.buurman.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.config.models.DemoDataProperties;
import com.buurman.job.DemoDataRegenerationJob;
import com.buurman.util.SkipTestCoverage;

@Configuration
@ConditionalOnProperty(name = "buurman.demo.enabled", havingValue = "true")
@SkipTestCoverage
public class DemoQuartzConfig {

  @Bean
  public JobDetail demoDataRegenerationJobDetail() {
    return JobBuilder.newJob(DemoDataRegenerationJob.class)
        .withIdentity("demoDataRegenerationJob", "demo")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger demoDataRegenerationTrigger(
      JobDetail demoDataRegenerationJobDetail, DemoDataProperties demoDataProperties) {
    return TriggerBuilder.newTrigger()
        .forJob(demoDataRegenerationJobDetail)
        .withIdentity("demoDataRegenerationTrigger", "demo")
        .withSchedule(CronScheduleBuilder.cronSchedule(demoDataProperties.cron()))
        .build();
  }
}
