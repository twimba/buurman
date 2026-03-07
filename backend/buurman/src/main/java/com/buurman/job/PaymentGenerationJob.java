package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.PaymentSchedulingService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PaymentGenerationJob implements Job {

  private final PaymentSchedulingService paymentSchedulingService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      paymentSchedulingService.scheduledPaymentGeneration();
    } catch (Exception e) {
      throw new JobExecutionException("Payment generation failed", e);
    }
  }
}
