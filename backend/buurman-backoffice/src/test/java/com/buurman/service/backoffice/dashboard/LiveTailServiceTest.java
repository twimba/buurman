package com.buurman.service.backoffice.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.LiveTailResponse;
import com.buurman.service.backoffice.dashboard.livetail.LogRingBuffer;

import ch.qos.logback.classic.Level;

@DisplayName("LiveTailService")
class LiveTailServiceTest {

  private final LogRingBuffer buffer = new LogRingBuffer();
  private final LiveTailService service = new LiveTailService(buffer);

  private void add(Level level, String logger, String message) {
    buffer.add(
        new LogRingBuffer.Entry(Instant.now(), level.toInt(), level.toString(), logger, message));
  }

  @Test
  @DisplayName("filters by minimum level and shortens the logger name")
  void filtersByLevel() {
    add(Level.INFO, "com.buurman.Foo", "info msg");
    add(Level.ERROR, "com.buurman.Bar", "boom");

    LiveTailResponse response = service.getLiveTail(Optional.of("WARN"));

    assertThat(response.status()).isEqualTo(PanelStatus.LIVE);
    assertThat(response.lines()).hasSize(1);
    assertThat(response.lines().get(0).level()).isEqualTo("ERROR");
    assertThat(response.lines().get(0).logger()).isEqualTo("Bar");
  }

  @Test
  @DisplayName("returns all levels when no filter is given")
  void returnsAllWhenNoFilter() {
    add(Level.DEBUG, "com.buurman.Foo", "a");
    add(Level.INFO, "com.buurman.Foo", "b");

    assertThat(service.getLiveTail(Optional.empty()).lines()).hasSize(2);
  }
}
