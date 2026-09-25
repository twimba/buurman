package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.LateFeeService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class LateFeeJob implements Job {

  private final LateFeeService lateFeeService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      lateFeeService.runDailyLateFees();
    } catch (Exception e) {
      throw new JobExecutionException("Late fee run failed", e);
    }
  }
}
