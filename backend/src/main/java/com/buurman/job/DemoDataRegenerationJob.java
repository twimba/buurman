package com.buurman.job;

import com.buurman.service.demo.DemoDataService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class DemoDataRegenerationJob implements Job {

    private final DemoDataService demoDataService;

    public DemoDataRegenerationJob(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            demoDataService.scheduledRegenerate();
        } catch (Exception e) {
            throw new JobExecutionException("Demo data regeneration failed", e);
        }
    }
}
