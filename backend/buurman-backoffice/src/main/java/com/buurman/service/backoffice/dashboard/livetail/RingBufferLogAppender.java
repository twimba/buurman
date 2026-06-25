package com.buurman.service.backoffice.dashboard.livetail;

import java.time.Instant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/** Logback appender that mirrors each log event into the {@link LogRingBuffer}. */
public class RingBufferLogAppender extends AppenderBase<ILoggingEvent> {

  // Only INFO+ events are mirrored. DEBUG/TRACE lines (e.g. geocoding/address traces) often carry
  // tenant PII and must never sit in the in-memory buffer that the backoffice live-tail exposes.
  private static final int MIN_LEVEL = Level.INFO.toInt();

  private final LogRingBuffer ringBuffer;

  public RingBufferLogAppender(LogRingBuffer ringBuffer) {
    this.ringBuffer = ringBuffer;
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (event.getLevel().toInt() < MIN_LEVEL) {
      return;
    }
    ringBuffer.add(
        new LogRingBuffer.Entry(
            Instant.ofEpochMilli(event.getTimeStamp()),
            event.getLevel().toInt(),
            event.getLevel().toString(),
            event.getLoggerName(),
            event.getFormattedMessage()));
  }
}
