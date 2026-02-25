package com.buurman.config;

import java.time.Duration;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import io.github.bucket4j.postgresql.PostgreSQLSelectForUpdateBasedProxyManager;

@Configuration
public class Bucket4jConfig {

  @Bean
  public PostgreSQLSelectForUpdateBasedProxyManager<String> rateLimitProxyManagerInstance(
      DataSource dataSource) {
    return Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
        .primaryKeyMapper(PrimaryKeyMapper.STRING)
        .table("rate_limit_buckets")
        .idColumn("id")
        .stateColumn("state")
        .expiresAtColumn("expires_at")
        .expirationAfterWrite(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                Duration.ofSeconds(10)))
        .build();
  }

  @Bean
  public ProxyManager<String> rateLimitProxyManager(
      PostgreSQLSelectForUpdateBasedProxyManager<String> instance) {
    return instance;
  }

  @Bean
  public ExpiredEntriesCleaner rateLimitExpiredEntriesCleaner(
      PostgreSQLSelectForUpdateBasedProxyManager<String> instance) {
    return instance;
  }
}
