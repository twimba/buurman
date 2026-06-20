package com.buurman.service.backoffice.dashboard.livetail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LogRingBuffer")
class LogRingBufferTest {

  private static final int TRACE = 5000;
  private static final int INFO = 20000;
  private static final int WARN = 30000;
  private static final int ERROR = 40000;

  private LogRingBuffer.Entry entry(int levelInt, String level, String message) {
    return new LogRingBuffer.Entry(Instant.now(), levelInt, level, "com.buurman.Test", message);
  }

  @Test
  @DisplayName("returns entries newest-first")
  void newestFirst() {
    LogRingBuffer buffer = new LogRingBuffer();
    buffer.add(entry(INFO, "INFO", "first"));
    buffer.add(entry(INFO, "INFO", "second"));

    List<LogRingBuffer.Entry> recent = buffer.recent(TRACE, 10);

    assertThat(recent).extracting(LogRingBuffer.Entry::message).containsExactly("second", "first");
  }

  @Test
  @DisplayName("filters out entries below the minimum level")
  void filtersByLevel() {
    LogRingBuffer buffer = new LogRingBuffer();
    buffer.add(entry(INFO, "INFO", "info"));
    buffer.add(entry(WARN, "WARN", "warn"));
    buffer.add(entry(ERROR, "ERROR", "error"));

    List<LogRingBuffer.Entry> recent = buffer.recent(WARN, 10);

    assertThat(recent).extracting(LogRingBuffer.Entry::message).containsExactly("error", "warn");
  }

  @Test
  @DisplayName("respects the requested limit")
  void respectsLimit() {
    LogRingBuffer buffer = new LogRingBuffer();
    for (int i = 0; i < 5; i++) {
      buffer.add(entry(INFO, "INFO", "msg" + i));
    }

    assertThat(buffer.recent(TRACE, 2)).hasSize(2);
  }

  @Test
  @DisplayName("evicts oldest entries beyond capacity")
  void evictsBeyondCapacity() {
    LogRingBuffer buffer = new LogRingBuffer();
    for (int i = 0; i < LogRingBuffer.CAPACITY + 50; i++) {
      buffer.add(entry(INFO, "INFO", "msg" + i));
    }

    List<LogRingBuffer.Entry> all = buffer.recent(TRACE, LogRingBuffer.CAPACITY + 100);

    assertThat(all).hasSize(LogRingBuffer.CAPACITY);
    assertThat(all.get(0).message()).isEqualTo("msg" + (LogRingBuffer.CAPACITY + 49));
  }
}
