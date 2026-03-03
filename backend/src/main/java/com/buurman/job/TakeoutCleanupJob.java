package com.buurman.job;

import java.time.Clock;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.domain.DataTakeout;
import com.buurman.repository.DataTakeoutRepository;
import com.buurman.service.S3StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class TakeoutCleanupJob implements Job {

  private final DataTakeoutRepository takeoutRepository;
  private final S3StorageService s3StorageService;
  private final Clock clock;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      List<DataTakeout> expired = takeoutRepository.findExpired(clock.instant());
      int cleaned = 0;
      for (DataTakeout takeout : expired) {
        takeout
            .getFileKey()
            .ifPresent(
                key -> {
                  try {
                    s3StorageService.deleteFile(key);
                  } catch (Exception e) {
                    log.warn("Failed to delete S3 file for expired takeout: {}", key, e);
                  }
                });
        takeoutRepository.softDelete(takeout.getId());
        cleaned++;
      }
      if (cleaned > 0) {
        log.info("Cleaned up {} expired data takeouts", cleaned);
      }
    } catch (Exception e) {
      throw new JobExecutionException("Takeout cleanup failed", e);
    }
  }
}
