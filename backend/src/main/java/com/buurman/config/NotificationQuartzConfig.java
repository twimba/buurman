package com.buurman.config;

import com.buurman.job.NotificationOutboxJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
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
                                              @Value("${notification.outbox.poll-interval-ms:10000}") long intervalMs) {
        return TriggerBuilder.newTrigger()
                .forJob(notificationOutboxJobDetail)
                .withIdentity("notificationOutboxTrigger", "notification")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(intervalMs)
                        .repeatForever())
                .build();
    }
}
