package com.buurman.config;

import com.buurman.job.PaymentGenerationJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz Scheduler configuration for automatic payment generation.
 * Uses Spring Boot's auto-configuration for SchedulerFactoryBean.
 * Database configuration is handled via application.yml.
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail paymentGenerationJobDetail() {
        return JobBuilder.newJob(PaymentGenerationJob.class)
                .withIdentity("paymentGenerationJob", "payment-scheduling")
                .withDescription("Generates future rent payments for active contracts")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger paymentGenerationTrigger(JobDetail paymentGenerationJobDetail) {
        // Every 4 hours: 0 0 */4 * * ?
        // Seconds Minutes Hours DayOfMonth Month DayOfWeek
        return TriggerBuilder.newTrigger()
                .forJob(paymentGenerationJobDetail)
                .withIdentity("paymentGenerationTrigger", "payment-scheduling")
                .withDescription("Trigger for payment generation job (every 4 hours)")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                .build();
    }
}
