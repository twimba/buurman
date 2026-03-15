package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.ImpersonationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class ImpersonationSessionCleanupJob implements Job {

  private final ImpersonationService impersonationService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      int expired = impersonationService.expireOverdueSessions();
      if (expired > 0) {
        log.info("Impersonation session cleanup: expired {} overdue sessions", expired);
      }
    } catch (Exception e) {
      throw new JobExecutionException("Impersonation session cleanup failed", e);
    }
  }
}
