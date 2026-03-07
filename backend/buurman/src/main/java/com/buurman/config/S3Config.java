package com.buurman.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.config.models.AwsS3Properties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class S3Config {

  private final AwsS3Properties awsS3Properties;

  @Bean
  public S3Client s3Client() {
    var builder =
        S3Client.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .region(Region.of(awsS3Properties.region()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        awsS3Properties.accessKey(), awsS3Properties.secretKey())));

    awsS3Properties
        .endpoint()
        .filter(e -> !e.isBlank())
        .ifPresent(e -> builder.endpointOverride(URI.create(e)).forcePathStyle(true));

    return builder.build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    var builder =
        S3Presigner.builder()
            .region(Region.of(awsS3Properties.region()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        awsS3Properties.accessKey(), awsS3Properties.secretKey())));

    awsS3Properties
        .endpoint()
        .filter(e -> !e.isBlank())
        .ifPresent(
            e ->
                builder
                    .endpointOverride(URI.create(e))
                    .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build()));

    return builder.build();
  }
}
