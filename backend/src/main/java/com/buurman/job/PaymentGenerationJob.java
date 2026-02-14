package com.buurman.job;

import com.buurman.service.PaymentSchedulingService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class PaymentGenerationJob implements Job {

    private final PaymentSchedulingService paymentSchedulingService;

    public PaymentGenerationJob(PaymentSchedulingService paymentSchedulingService) {
        this.paymentSchedulingService = paymentSchedulingService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            paymentSchedulingService.scheduledPaymentGeneration();
        } catch (Exception e) {
            throw new JobExecutionException("Payment generation failed", e);
        }
    }
}
