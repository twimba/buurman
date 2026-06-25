package com.buurman.service.backoffice.dashboard.livetail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Attaches the {@link RingBufferLogAppender} to the Logback root logger at startup, fronted by an
 * {@link AsyncAppender} so mirroring into the ring buffer happens on a logback worker thread and
 * never blocks application threads on the synchronous logging path.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveTailLogConfig {

  private static final String APPENDER_NAME = "dashboardRingBufferAsync";

  private final LogRingBuffer ringBuffer;

  private AsyncAppender async;

  @PostConstruct
  void attachAppender() {
    if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
      log.warn("SLF4J backend is not Logback; live-tail ring buffer disabled");
      return;
    }
    var root = context.getLogger(Logger.ROOT_LOGGER_NAME);
    if (root.getAppender(APPENDER_NAME) != null) {
      // A previous context (e.g. a test refresh) already attached one; don't stack duplicates.
      return;
    }
    RingBufferLogAppender ring = new RingBufferLogAppender(ringBuffer);
    ring.setName("dashboardRingBuffer");
    ring.setContext(context);
    ring.start();

    async = new AsyncAppender();
    async.setName(APPENDER_NAME);
    async.setContext(context);
    // Never block the app thread: drop events when the queue is full rather than apply backpressure
    // to logging, and don't drop on queue pressure for higher levels (keep all — it's a tail view).
    async.setNeverBlock(true);
    async.setDiscardingThreshold(0);
    async.addAppender(ring);
    async.start();

    root.addAppender(async);
    log.info("Live-tail ring buffer appender (async) attached to root logger");
  }

  /**
   * Detach + stop on shutdown. The Logback {@link LoggerContext} outlives the Spring context, so
   * without this a context refresh (DevTools restart, @DirtiesContext tests) would leave orphaned
   * appenders bolted onto the root logger, each pinning a dead ring buffer. Stopping the
   * AsyncAppender also stops its nested appender.
   */
  @PreDestroy
  void detachAppender() {
    if (async == null) {
      return;
    }
    if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
      Appender<ILoggingEvent> existing =
          context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(APPENDER_NAME);
      context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(async);
      if (existing != null) {
        existing.stop();
      }
    }
    async.stop();
    async = null;
  }
}
