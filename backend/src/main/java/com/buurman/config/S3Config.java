package com.buurman.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {

    private final AwsS3Properties awsS3Properties;

    public S3Config(AwsS3Properties awsS3Properties) {
        this.awsS3Properties = awsS3Properties;
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .endpointOverride(URI.create(awsS3Properties.endpoint()))
                .region(Region.of(awsS3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsS3Properties.accessKey(), awsS3Properties.secretKey())))
                .forcePathStyle(true) // Required for S3-compatible storage (SeaweedFS, etc.)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(awsS3Properties.endpoint()))
                .region(Region.of(awsS3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsS3Properties.accessKey(), awsS3Properties.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // Required for S3-compatible storage (SeaweedFS, etc.)
                        .build())
                .build();
    }

}
