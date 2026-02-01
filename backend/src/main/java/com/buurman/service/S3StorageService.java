package com.buurman.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String s3Endpoint;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.endpoint}") String s3Endpoint) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.s3Endpoint = s3Endpoint;
    }

    /**
     * Upload file to S3 and return the file key.
     * File key pattern: {teamId}/{entityType}/{entityId}/{uuid}_{filename}
     */
    public String uploadFile(MultipartFile file, UUID teamId, String entityType, UUID entityId) {
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

            log.info("File uploaded to S3: {}", fileKey);
            return fileKey;
        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Generate presigned URL for downloading a file.
     * URL is valid for 15 minutes.
     * For LocalStack, uses direct URLs without presigning to avoid CORS issues.
     */
    public URL generatePresignedUrl(String fileKey) {
        try {
            // For LocalStack (localhost), use direct URLs without presigning
            if (s3Endpoint.contains("localhost") || s3Endpoint.contains("127.0.0.1")) {
                String directUrl = s3Endpoint + "/" + bucketName + "/" + fileKey;
                log.debug("Generated direct URL for LocalStack: {}", directUrl);
                return URI.create(directUrl).toURL();
            }

            // For production AWS, use presigned URLs
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

            log.info("Generated presigned URL for file: {} -> {}", fileKey, url.toString());
            return url;
        } catch (Exception e) {
            log.error("Failed to generate URL for file: {}", fileKey, e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    /**
     * Delete file from S3.
     */
    public void deleteFile(String fileKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("File deleted from S3: {}", fileKey);
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
