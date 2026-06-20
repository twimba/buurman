package com.buurman.service.backoffice.dashboard.livetail;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Bounded, thread-safe in-memory buffer of recent log events that powers the dashboard "Live tail"
 * panel. Capped at {@link #CAPACITY} entries; oldest are evicted first.
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
    List<Entry> result = new ArrayList<>();
    synchronized (lock) {
      Iterator<Entry> it = entries.descendingIterator();
      while (it.hasNext() && result.size() < limit) {
        Entry entry = it.next();
        if (entry.levelInt() >= minLevelInt) {
          result.add(entry);
        }
      }
    }
    return result;
  }
}
