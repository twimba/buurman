package com.buurman.service.backoffice.dashboard.livetail;

import java.time.Instant;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/** Logback appender that mirrors each log event into the {@link LogRingBuffer}. */
public class RingBufferLogAppender extends AppenderBase<ILoggingEvent> {

  private final LogRingBuffer ringBuffer;

  public RingBufferLogAppender(LogRingBuffer ringBuffer) {
    this.ringBuffer = ringBuffer;
  }

  @Override
  protected void append(ILoggingEvent event) {
    ringBuffer.add(
        new LogRingBuffer.Entry(
            Instant.ofEpochMilli(event.getTimeStamp()),
            event.getLevel().toInt(),
            event.getLevel().toString(),
            event.getLoggerName(),
            event.getFormattedMessage()));
  }
}
