package com.buurman.job;

import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Clock;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.repository.EmailVerificationCodeRepository;
import com.buurman.repository.PhoneVerificationCodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class VerificationCodeCleanupJob implements Job {

  private static final long RETENTION_HOURS = 48;

  private final EmailVerificationCodeRepository emailCodeRepository;
  private final PhoneVerificationCodeRepository phoneCodeRepository;
  private final Clock clock;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      var cutoff = clock.instant().minus(RETENTION_HOURS, HOURS);

      int emailDeleted = emailCodeRepository.deleteExpiredAndUsed(cutoff);
      int phoneDeleted = phoneCodeRepository.deleteExpiredAndUsed(cutoff);

      if (emailDeleted > 0 || phoneDeleted > 0) {
        log.info(
            "Verification code cleanup: deleted {} email codes and {} phone codes older than {}h",
            emailDeleted,
            phoneDeleted,
            RETENTION_HOURS);
      }
    } catch (Exception e) {
      throw new JobExecutionException("Verification code cleanup failed", e);
    }
  }
}
