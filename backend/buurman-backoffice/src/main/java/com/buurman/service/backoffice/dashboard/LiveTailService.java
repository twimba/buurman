package com.buurman.service.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.LiveTailResponse;
import com.buurman.dto.response.backoffice.dashboard.LiveTailResponse.LogLine;
import com.buurman.service.backoffice.dashboard.livetail.LogRingBuffer;

import ch.qos.logback.classic.Level;
import lombok.RequiredArgsConstructor;

/** Serves recent log lines (newest first) from the in-memory ring buffer, filtered by level. */
@Service
@RequiredArgsConstructor
public class LiveTailService {

  private static final int MAX_LINES = 200;

  private final LogRingBuffer ringBuffer;

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LiveTailResponse getLiveTail(Optional<String> level) {
    int minLevel =
        level
            .filter(s -> !s.isBlank())
            .map(s -> Level.toLevel(s, Level.TRACE).toInt())
            .orElse(Level.TRACE.toInt());
    List<LogLine> lines =
        ringBuffer.recent(minLevel, MAX_LINES).stream()
            .map(
                e ->
                    new LogLine(
                        e.timestamp().toString(), e.level(), shortLogger(e.logger()), e.message()))
            .toList();
    return new LiveTailResponse(PanelStatus.LIVE, Optional.empty(), Optional.empty(), lines);
  }

  private static String shortLogger(String name) {
    int idx = name.lastIndexOf('.');
    return idx >= 0 ? name.substring(idx + 1) : name;
  }
}
