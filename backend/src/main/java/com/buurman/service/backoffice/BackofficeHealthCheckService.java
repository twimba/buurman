package com.buurman.service.backoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.keycloak.admin.client.Keycloak;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.buurman.config.models.AwsS3Properties;
import com.buurman.config.models.KeycloakProperties;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.ServiceHealth;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.ServiceHealth.Status;
import com.flagsmith.FlagsmithClient;
import com.sendgrid.SendGrid;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Service
@Slf4j
public class BackofficeHealthCheckService {

  private final ExecutorService healthCheckExecutor = Executors.newVirtualThreadPerTaskExecutor();

  private final DSLContext dsl;
  private final DataSource dataSource;
  private final Scheduler scheduler;
  private final S3Client s3Client;
  private final AwsS3Properties awsS3Properties;
  private final Keycloak keycloak;
  private final KeycloakProperties keycloakProperties;
  private final ObjectProvider<FlagsmithClient> flagsmithClientProvider;
  private final ObjectProvider<SendGrid> sendGridProvider;
  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  public BackofficeHealthCheckService(
      DSLContext dsl,
      DataSource dataSource,
      Scheduler scheduler,
      S3Client s3Client,
      AwsS3Properties awsS3Properties,
      Keycloak keycloak,
      KeycloakProperties keycloakProperties,
      ObjectProvider<FlagsmithClient> flagsmithClientProvider,
      ObjectProvider<SendGrid> sendGridProvider,
      ObjectProvider<JavaMailSender> mailSenderProvider) {
    this.dsl = dsl;
    this.dataSource = dataSource;
    this.scheduler = scheduler;
    this.s3Client = s3Client;
    this.awsS3Properties = awsS3Properties;
    this.keycloak = keycloak;
    this.keycloakProperties = keycloakProperties;
    this.flagsmithClientProvider = flagsmithClientProvider;
    this.sendGridProvider = sendGridProvider;
    this.mailSenderProvider = mailSenderProvider;
  }

  @PreDestroy
  void shutdown() {
    healthCheckExecutor.shutdown();
  }

  public List<ServiceHealth> checkAll() {
    List<CompletableFuture<ServiceHealth>> futures = new ArrayList<>();

    futures.add(checkAsync("PostgreSQL", this::checkPostgres));
    futures.add(checkAsync("Quartz Scheduler", this::checkQuartz));
    futures.add(checkAsync("S3 Storage", this::checkS3));
    futures.add(checkAsync("Keycloak", this::checkKeycloak));
    futures.add(checkAsync("Flagsmith", this::checkFlagsmith));
    futures.add(checkAsync("SendGrid", this::checkSendGrid));
    futures.add(checkAsync("SMTP", this::checkSmtp));

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    return futures.stream().map(CompletableFuture::join).toList();
  }

  private CompletableFuture<ServiceHealth> checkAsync(
      String name, Supplier<ServiceHealth> checker) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return checker.get();
          } catch (Exception e) {
            log.warn("Health check failed for {}: {}", name, e.getMessage());
            return new ServiceHealth(name, Status.DOWN, null, null, e.getMessage());
          }
        },
        healthCheckExecutor);
  }

  private ServiceHealth checkPostgres() {
    long start = System.currentTimeMillis();
    dsl.fetch("SELECT 1");
    long latency = System.currentTimeMillis() - start;

    String details = null;
    if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hds) {
      var pool = hds.getHikariPoolMXBean();
      if (pool != null) {
        details =
            "Pool: %d/%d active, %d idle"
                .formatted(
                    pool.getActiveConnections(),
                    pool.getTotalConnections(),
                    pool.getIdleConnections());
      }
    }
    return new ServiceHealth("PostgreSQL", Status.UP, latency, details, null);
  }

  private ServiceHealth checkQuartz() {
    try {
      long start = System.currentTimeMillis();
      boolean started = scheduler.isStarted();
      long latency = System.currentTimeMillis() - start;
      String details = started ? "Running" : "Stopped";
      return new ServiceHealth(
          "Quartz Scheduler", started ? Status.UP : Status.DOWN, latency, details, null);
    } catch (Exception e) {
      return new ServiceHealth("Quartz Scheduler", Status.DOWN, null, null, e.getMessage());
    }
  }

  private ServiceHealth checkS3() {
    long start = System.currentTimeMillis();
    s3Client.headBucket(HeadBucketRequest.builder().bucket(awsS3Properties.bucketName()).build());
    long latency = System.currentTimeMillis() - start;
    return new ServiceHealth(
        "S3 Storage", Status.UP, latency, "Bucket: " + awsS3Properties.bucketName(), null);
  }

  private ServiceHealth checkKeycloak() {
    long start = System.currentTimeMillis();
    keycloak.realm(keycloakProperties.realm()).toRepresentation();
    long latency = System.currentTimeMillis() - start;
    return new ServiceHealth(
        "Keycloak", Status.UP, latency, "Realm: " + keycloakProperties.realm(), null);
  }

  private ServiceHealth checkFlagsmith() {
    var client = flagsmithClientProvider.getIfAvailable();
    if (client == null) {
      return new ServiceHealth("Flagsmith", Status.DISABLED, null, "Client not initialized", null);
    }
    return new ServiceHealth("Flagsmith", Status.UP, null, "Client initialized", null);
  }

  private ServiceHealth checkSendGrid() {
    var sg = sendGridProvider.getIfAvailable();
    if (sg == null) {
      return new ServiceHealth(
          "SendGrid", Status.DISABLED, null, "Not active in this profile", null);
    }
    try {
      long start = System.currentTimeMillis();
      var request = new com.sendgrid.Request();
      request.setMethod(com.sendgrid.Method.GET);
      request.setEndpoint("scopes");
      var response = sg.api(request);
      long latency = System.currentTimeMillis() - start;
      boolean ok = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
      return new ServiceHealth(
          "SendGrid",
          ok ? Status.UP : Status.DOWN,
          latency,
          "HTTP " + response.getStatusCode(),
          ok ? null : "HTTP " + response.getStatusCode());
    } catch (Exception e) {
      return new ServiceHealth("SendGrid", Status.DOWN, null, null, e.getMessage());
    }
  }

  private ServiceHealth checkSmtp() {
    var sender = mailSenderProvider.getIfAvailable();
    if (sender == null) {
      return new ServiceHealth("SMTP", Status.DISABLED, null, "Not active in this profile", null);
    }
    try {
      long start = System.currentTimeMillis();
      if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
        impl.testConnection();
      }
      long latency = System.currentTimeMillis() - start;
      return new ServiceHealth("SMTP", Status.UP, latency, null, null);
    } catch (Exception e) {
      return new ServiceHealth("SMTP", Status.DOWN, null, null, e.getMessage());
    }
  }
}
