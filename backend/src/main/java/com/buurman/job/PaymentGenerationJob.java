package com.buurman.job;

import com.buurman.service.PaymentSchedulingService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
