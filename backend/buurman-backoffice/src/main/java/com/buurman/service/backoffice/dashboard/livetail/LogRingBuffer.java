package com.buurman.service.backoffice.dashboard.livetail;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Bounded, thread-safe in-memory buffer of recent log events that powers the dashboard "Live tail"
 * panel. Capped at {@link #CAPACITY} entries; oldest are evicted first.
 *
 * <p><strong>Per-JVM-instance caveat:</strong> this buffer lives in the heap of a single JVM. In a
 * multi-replica / clustered backoffice deployment the live-tail only reflects logs from the
 * instance that happened to serve the request — never the whole fleet. Tailing across all replicas
 * requires a centralized log sink (e.g. Loki/ELK), not this buffer.
 */
@Component
public class LogRingBuffer {

  /** Spec cap: keep at most this many entries to bound memory. */
  static final int CAPACITY = 1000;

  /** One buffered log event. {@code levelInt} is logback's numeric level for filtering. */
  public record Entry(
      Instant timestamp, int levelInt, String level, String logger, String message) {}

  private final Deque<Entry> entries = new ArrayDeque<>(CAPACITY);
  private final Object lock = new Object();

  public void add(Entry entry) {
    synchronized (lock) {
      if (entries.size() >= CAPACITY) {
        entries.pollFirst();
      }
      entries.addLast(entry);
    }
  }

  /** Newest-first, at most {@code limit} entries at or above {@code minLevelInt}. */
  public List<Entry> recent(int minLevelInt, int limit) {
    // Snapshot references under the lock, then filter/limit outside it, so a dashboard read never
    // holds the lock for longer than a fixed-size copy (the level filter runs lock-free).
    Entry[] snapshot;
    synchronized (lock) {
      snapshot = entries.toArray(new Entry[0]);
    }
    List<Entry> result = new ArrayList<>();
    for (int i = snapshot.length - 1; i >= 0 && result.size() < limit; i--) {
      if (snapshot[i].levelInt() >= minLevelInt) {
        result.add(snapshot[i]);
      }
    }
    return result;
  }
}
