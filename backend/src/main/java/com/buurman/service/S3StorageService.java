package com.buurman.service;

import com.buurman.config.AwsS3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String s3PublicEndpoint;
    private final boolean usePresignedUrls;
    private final MetricsService metricsService;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AwsS3Properties s3Properties,
            MetricsService metricsService) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = s3Properties.bucketName();
        this.s3PublicEndpoint = s3Properties.publicEndpoint();
        this.usePresignedUrls = s3Properties.usePresignedUrls();
        this.metricsService = metricsService;
    }

    /**
     * Upload file to S3 and return the file key.
     * File key pattern: {teamId}/{entityType}/{entityId}/{uuid}_{filename}
     */
    public String uploadFile(MultipartFile file, UUID teamId, String entityType, UUID entityId) {
        Instant start = Instant.now();
        try {
            String originalFilename = file.getOriginalFilename();
            String fileKey = generateFileKey(teamId, entityType, entityId, originalFilename);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "upload", "result", "success");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "upload", "result", "success");

            log.info("File uploaded to S3: {}", fileKey);
            return fileKey;
        } catch (IOException e) {
            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "upload", "result", "failure");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "upload", "result", "failure");
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Generate presigned URL for downloading a file.
     * URL is valid for 15 minutes.
     * For local dev (SeaweedFS), uses direct URLs without presigning to avoid CORS issues.
     */
    public URL generatePresignedUrl(String fileKey) {
        Instant start = Instant.now();
        try {
            // For local/docker dev, use direct URLs via the public endpoint
            if (!usePresignedUrls) {
                String directUrl = s3PublicEndpoint + "/" + bucketName + "/" + fileKey;
                log.debug("Generated direct URL: {}", directUrl);
                metricsService.recordTimer("s3.operation.seconds",
                        Duration.between(start, Instant.now()),
                        "operation", "presign", "result", "success");
                metricsService.incrementCounter("s3.operation.total",
                        "operation", "presign", "result", "success");
                return URI.create(directUrl).toURL();
            }

            // For production, use presigned URLs
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_DURATION)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            URL url = presignedRequest.url();

            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "presign", "result", "success");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "presign", "result", "success");

            log.info("Generated presigned URL for file: {} -> {}", fileKey, url.toString());
            return url;
        } catch (Exception e) {
            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "presign", "result", "failure");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "presign", "result", "failure");
            log.error("Failed to generate URL for file: {}", fileKey, e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    /**
     * Download file from S3 and return as InputStream.
     */
    public InputStream downloadFile(String fileKey) {
        Instant start = Instant.now();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "download", "result", "success");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "download", "result", "success");

            log.debug("Downloaded file from S3: {}", fileKey);
            return s3Object;
        } catch (Exception e) {
            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "download", "result", "failure");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "download", "result", "failure");
            log.error("Failed to download file from S3: {}", fileKey, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    /**
     * Delete file from S3.
     */
    public void deleteFile(String fileKey) {
        Instant start = Instant.now();
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "delete", "result", "success");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "delete", "result", "success");

            log.info("File deleted from S3: {}", fileKey);
        } catch (Exception e) {
            metricsService.recordTimer("s3.operation.seconds",
                    Duration.between(start, Instant.now()),
                    "operation", "delete", "result", "failure");
            metricsService.incrementCounter("s3.operation.total",
                    "operation", "delete", "result", "failure");
            log.error("Failed to delete file from S3: {}", fileKey, e);
            throw e;
        }
    }

    /**
     * Generate file key with pattern: {teamId}/{entityType}/{entityId}/{uuid}_{filename}
     */
    private String generateFileKey(UUID teamId, String entityType, UUID entityId, String filename) {
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueId = UUID.randomUUID().toString();
        return String.format("%s/%s/%s/%s_%s",
                teamId, entityType, entityId, uniqueId, sanitizedFilename);
    }
}
