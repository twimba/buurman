package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(
    Optional<String> endpoint,
    String region,
    String accessKey,
    String secretKey,
    String bucketName,
    Optional<String> publicEndpoint,
    boolean usePresignedUrls) {

  public AwsS3Properties {
    endpoint = Optional.of(endpoint).flatMap(o -> o);
    publicEndpoint = Optional.of(publicEndpoint).flatMap(o -> o);
  }
}
