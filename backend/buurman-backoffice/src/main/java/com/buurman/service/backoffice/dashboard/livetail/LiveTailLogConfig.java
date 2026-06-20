package com.buurman.service.backoffice.dashboard.livetail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Attaches the {@link RingBufferLogAppender} to the Logback root logger at startup. */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveTailLogConfig {

  private final LogRingBuffer ringBuffer;

  @PostConstruct
  void attachAppender() {
    if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
      log.warn("SLF4J backend is not Logback; live-tail ring buffer disabled");
      return;
    }
    RingBufferLogAppender appender = new RingBufferLogAppender(ringBuffer);
    appender.setName("dashboardRingBuffer");
    appender.setContext(context);
    appender.start();
    context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
    log.info("Live-tail ring buffer appender attached to root logger");
  }
}
