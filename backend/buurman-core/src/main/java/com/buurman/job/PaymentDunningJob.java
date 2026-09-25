package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.PaymentDunningService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PaymentDunningJob implements Job {

  private final PaymentDunningService paymentDunningService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      paymentDunningService.runDailyLadder();
    } catch (Exception e) {
      throw new JobExecutionException("Payment dunning ladder failed", e);
    }
  }
}
