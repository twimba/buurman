package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "aws.s3")
@SkipTestCoverage
public record AwsS3Properties(
    Optional<String> endpoint,
    String region,
    String accessKey,
    String secretKey,
    String bucketName,
    Optional<String> publicEndpoint,
    boolean usePresignedUrls) {

  public AwsS3Properties {
    endpoint = Optional.ofNullable(endpoint).flatMap(o -> o);
    publicEndpoint = Optional.ofNullable(publicEndpoint).flatMap(o -> o);
  }
}
