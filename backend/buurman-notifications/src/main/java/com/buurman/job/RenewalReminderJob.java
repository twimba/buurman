package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.ContractExtensionService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class RenewalReminderJob implements Job {

  private final ContractExtensionService contractExtensionService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      contractExtensionService.processRenewalReminders();
    } catch (Exception e) {
      throw new JobExecutionException("Renewal reminder processing failed", e);
    }
  }
}
