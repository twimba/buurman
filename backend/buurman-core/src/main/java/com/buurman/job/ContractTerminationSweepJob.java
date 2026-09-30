package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.ContractTerminationService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ContractTerminationSweepJob implements Job {

  private final ContractTerminationService contractTerminationService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      contractTerminationService.sweepDueTerminations();
    } catch (Exception e) {
      throw new JobExecutionException("Contract termination sweep failed", e);
    }
  }
}
