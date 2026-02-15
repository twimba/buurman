package com.buurman.job;

import com.buurman.repository.EmailVerificationCodeRepository;
import com.buurman.repository.PhoneVerificationCodeRepository;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.HOURS;

@Component
@DisallowConcurrentExecution
public class VerificationCodeCleanupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeCleanupJob.class);
    private static final long RETENTION_HOURS = 48;

    private final EmailVerificationCodeRepository emailCodeRepository;
    private final PhoneVerificationCodeRepository phoneCodeRepository;
    private final Clock clock;

    public VerificationCodeCleanupJob(EmailVerificationCodeRepository emailCodeRepository,
                                       PhoneVerificationCodeRepository phoneCodeRepository,
                                       Clock clock) {
        this.emailCodeRepository = emailCodeRepository;
        this.phoneCodeRepository = phoneCodeRepository;
        this.clock = clock;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            var cutoff = clock.instant().minus(RETENTION_HOURS, HOURS);

            int emailDeleted = emailCodeRepository.deleteExpiredAndUsed(cutoff);
            int phoneDeleted = phoneCodeRepository.deleteExpiredAndUsed(cutoff);

            if (emailDeleted > 0 || phoneDeleted > 0) {
                log.info("Verification code cleanup: deleted {} email codes and {} phone codes older than {}h",
                        emailDeleted, phoneDeleted, RETENTION_HOURS);
            }
        } catch (Exception e) {
            throw new JobExecutionException("Verification code cleanup failed", e);
        }
    }
}
