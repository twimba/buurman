package com.buurman.config.models;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(
    @Nullable String endpoint,
    String region,
    String accessKey,
    String secretKey,
    String bucketName,
    @Nullable String publicEndpoint,
    boolean usePresignedUrls) {}
