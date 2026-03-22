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
public class AutoExtensionJob implements Job {

  private final ContractExtensionService contractExtensionService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      contractExtensionService.processAutoExtensions();
    } catch (Exception e) {
      throw new JobExecutionException("Auto-extension processing failed", e);
    }
  }
}
