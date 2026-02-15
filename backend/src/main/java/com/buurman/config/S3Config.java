package com.buurman.config;

import com.buurman.config.models.AwsS3Properties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class S3Config {

    private final AwsS3Properties awsS3Properties;


    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .region(Region.of(awsS3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsS3Properties.accessKey(), awsS3Properties.secretKey())));

        if (awsS3Properties.endpoint() != null && !awsS3Properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(awsS3Properties.endpoint()))
                    .forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(awsS3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsS3Properties.accessKey(), awsS3Properties.secretKey())));

        if (awsS3Properties.endpoint() != null && !awsS3Properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(awsS3Properties.endpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }

}
