package com.buurman.job;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.domain.Photo;
import com.buurman.repository.PhotoRepository;
import com.buurman.service.S3StorageService;
import com.buurman.service.ThumbnailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class ThumbnailBackfillJob implements Job {

  private static final int BATCH_SIZE = 50;

  private final PhotoRepository photoRepository;
  private final S3StorageService s3StorageService;
  private final ThumbnailService thumbnailService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    List<Photo> photos = photoRepository.findWithoutThumbnail(BATCH_SIZE);
    if (photos.isEmpty()) {
      return;
    }

    int success = 0;
    int failed = 0;

    for (Photo photo : photos) {
      try (InputStream is = s3StorageService.downloadFile(photo.getFileKey())) {
        var thumbData = thumbnailService.generateThumbnail(is);
        if (thumbData.isPresent()) {
          // Parse team/entity identifiers from file key
          // Key pattern: {teamIdentifier}/{entityType}/{entityIdentifier}/{uuid}_{filename}
          String[] parts = photo.getFileKey().split("/");
          String teamIdentifier = parts[0];
          String entityType = parts[1];
          String entityIdentifier = parts[2];

          String thumbnailFileKey =
              s3StorageService.uploadFile(
                  thumbData.get(),
                  "image/jpeg",
                  teamIdentifier,
                  entityType,
                  entityIdentifier,
                  "thumb_" + photo.getFileName());

          photo.setThumbnailFileKey(Optional.of(thumbnailFileKey));
          photoRepository.save(photo);
          success++;
        } else {
          failed++;
        }
      } catch (Exception e) {
        log.warn(
            "Failed to backfill thumbnail for photo {}: {}", photo.getIdentifier(), e.getMessage());
        failed++;
      }
    }

    log.info(
        "Thumbnail backfill: {} succeeded, {} failed out of {} processed",
        success,
        failed,
        photos.size());
  }
}
