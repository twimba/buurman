package com.buurman.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AwsS3Properties;
import com.buurman.domain.Sid;
import com.buurman.exception.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@Slf4j
public class S3StorageService {

  private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String bucketName;
  private final String s3PublicEndpoint;
  private final boolean usePresignedUrls;
  private final MetricsService metricsService;
  private final Clock clock;

  public S3StorageService(
      S3Client s3Client,
      S3Presigner s3Presigner,
      AwsS3Properties s3Properties,
      MetricsService metricsService,
      Clock clock) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.bucketName = s3Properties.bucketName();
    this.s3PublicEndpoint = s3Properties.publicEndpoint().orElse("");
    this.usePresignedUrls = s3Properties.usePresignedUrls();
    this.metricsService = metricsService;
    this.clock = clock;
  }

  /**
   * Upload file to S3 and return the file key. File key pattern:
   * {teamIdentifier}/{entityType}/{entityIdentifier}/{uuid}_{filename}
   */
  public String uploadFile(
      MultipartFile file, Sid teamIdentifier, String entityType, Sid entityIdentifier) {
    Instant start = clock.instant();
    try {
      String originalFilename = file.getOriginalFilename();
      if (originalFilename == null) {
        throw new RuntimeException("Unable to upload file. No filename provided");
      }
      String fileKey =
          generateFileKey(teamIdentifier, entityType, entityIdentifier, originalFilename);

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(fileKey)
              .contentType(file.getContentType())
              .contentLength(file.getSize())
              .build();

      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "upload",
          "result",
          "success");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "upload", "result", "success");

      log.info("File uploaded to S3: {}", fileKey);
      return fileKey;
    } catch (IOException e) {
      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "upload",
          "result",
          "failure");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "upload", "result", "failure");
      log.error("Failed to upload file to S3", e);
      throw new ExternalServiceException("Failed to upload file", e);
    }
  }

  /** Upload raw bytes to S3 with a generated key. Same pattern as the MultipartFile variant. */
  public String uploadFile(
      byte[] data,
      String contentType,
      Sid teamIdentifier,
      String entityType,
      Sid entityIdentifier,
      String filename) {
    String fileKey = generateFileKey(teamIdentifier, entityType, entityIdentifier, filename);
    uploadFile(data, contentType, fileKey);

    return fileKey;
  }

  /** Upload raw bytes to S3 using an explicit (pre-computed) file key. */
  public void uploadFile(byte[] data, String contentType, String fileKey) {
    Instant start = clock.instant();

    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(fileKey).contentType(contentType).build();

    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));

    metricsService.recordTimer(
        "s3.operation.seconds",
        Duration.between(start, clock.instant()),
        "operation",
        "upload",
        "result",
        "success");
    metricsService.incrementCounter(
        "s3.operation.total", "operation", "upload", "result", "success");

    log.info("File uploaded to S3: {}", fileKey);
  }

  /**
   * Derives the thumbnail key from an existing photo file key by stripping the extension and
   * appending {@code _thumbnail.jpeg}. E.g.: {@code team/property/prop-123/uuid.jpg →
   * team/property/prop-123/uuid_thumbnail.jpeg}
   */
  public static String deriveThumbnailKey(String fileKey) {
    int lastDot = fileKey.lastIndexOf('.');
    int lastSlash = fileKey.lastIndexOf('/');
    if (lastDot > lastSlash) {
      return fileKey.substring(0, lastDot) + "_thumbnail.jpeg";
    }
    return fileKey + "_thumbnail.jpeg";
  }

  /**
   * Generate presigned URL for downloading a file. URL is valid for 15 minutes. For local dev
   * (SeaweedFS), uses direct URLs without presigning to avoid CORS issues.
   */
  public URL generatePresignedUrl(String fileKey) {
    Instant start = clock.instant();
    try {
      // For local/docker dev, use direct URLs via the public endpoint
      if (!usePresignedUrls) {
        String directUrl = s3PublicEndpoint + "/" + bucketName + "/" + fileKey;
        log.debug("Generated direct URL: {}", directUrl);
        metricsService.recordTimer(
            "s3.operation.seconds",
            Duration.between(start, clock.instant()),
            "operation",
            "presign",
            "result",
            "success");
        metricsService.incrementCounter(
            "s3.operation.total", "operation", "presign", "result", "success");
        return URI.create(directUrl).toURL();
      }

      // For production, use presigned URLs
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucketName).key(fileKey).build();

      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(PRESIGNED_URL_DURATION)
              .getObjectRequest(getObjectRequest)
              .build();

      PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
      URL url = presignedRequest.url();

      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "presign",
          "result",
          "success");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "presign", "result", "success");

      log.info("Generated presigned URL for file: {} -> {}", fileKey, url.toString());
      return url;
    } catch (Exception e) {
      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "presign",
          "result",
          "failure");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "presign", "result", "failure");
      log.error("Failed to generate URL for file: {}", fileKey, e);
      throw new ExternalServiceException("Failed to generate download URL", e);
    }
  }

  /** Download file from S3 and return as InputStream. */
  public InputStream downloadFile(String fileKey) {
    Instant start = clock.instant();
    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucketName).key(fileKey).build();

      ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "download",
          "result",
          "success");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "download", "result", "success");

      log.debug("Downloaded file from S3: {}", fileKey);
      return s3Object;
    } catch (Exception e) {
      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "download",
          "result",
          "failure");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "download", "result", "failure");
      log.error("Failed to download file from S3: {}", fileKey, e);
      throw new ExternalServiceException("Failed to download file", e);
    }
  }

  /** Delete file from S3. */
  public void deleteFile(String fileKey) {
    Instant start = clock.instant();
    try {
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(bucketName).key(fileKey).build();

      s3Client.deleteObject(deleteObjectRequest);

      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "delete",
          "result",
          "success");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "delete", "result", "success");

      log.info("File deleted from S3: {}", fileKey);
    } catch (Exception e) {
      metricsService.recordTimer(
          "s3.operation.seconds",
          Duration.between(start, clock.instant()),
          "operation",
          "delete",
          "result",
          "failure");
      metricsService.incrementCounter(
          "s3.operation.total", "operation", "delete", "result", "failure");
      log.error("Failed to delete file from S3: {}", fileKey, e);
      throw e;
    }
  }

  /**
   * Generate file key with pattern: {teamIdentifier}/{entityType}/{entityIdentifier}/{uuid}.{ext}
   */
  private String generateFileKey(
      Sid teamIdentifier, String entityType, Sid entityIdentifier, String filename) {
    String ext = extractExtension(filename);
    String uniqueId = UUID.randomUUID().toString();
    return String.format(
        "%s/%s/%s/%s.%s",
        teamIdentifier.value(), entityType, entityIdentifier.value(), uniqueId, ext);
  }

  private static String extractExtension(String filename) {
    int lastDot = filename.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < filename.length() - 1) {
      String ext =
          filename.substring(lastDot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
      return ext.isEmpty() ? "dat" : ext;
    }
    return "dat";
  }
}
