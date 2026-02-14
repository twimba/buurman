package com.buurman.config;

import com.buurman.config.models.NotificationOutboxProperties;
import com.buurman.job.NotificationOutboxJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationQuartzConfig {

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
}
