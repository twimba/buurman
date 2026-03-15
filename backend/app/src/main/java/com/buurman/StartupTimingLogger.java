package com.buurman;

import java.time.Duration;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.event.EventListener;
import org.springframework.core.metrics.StartupStep;
import org.springframework.stereotype.Component;

@Component
public class StartupTimingLogger {

  private static final Logger log = LoggerFactory.getLogger(StartupTimingLogger.class);
  private static final int TOP_N = 30;

  @EventListener(ApplicationReadyEvent.class)
  public void logStartupTimings(ApplicationReadyEvent event) {
    if (!(event.getSpringApplication().getApplicationStartup()
        instanceof BufferingApplicationStartup startup)) {
      return;
    }

    log.info("=== Top {} slowest startup steps ===", TOP_N);

    startup.getBufferedTimeline().getEvents().stream()
        .sorted(
            Comparator.comparing(
                    (org.springframework.boot.context.metrics.buffering.StartupTimeline
                                .TimelineEvent
                            e) -> e.getDuration())
                .reversed())
        .limit(TOP_N)
        .forEach(
            e -> {
              StartupStep step = e.getStartupStep();
              long ms = e.getDuration().toMillis();
              String tags = formatTags(step);
              String line =
                  String.format(
                      "  %7dms | %s%s",
                      ms, step.getName(), tags.isEmpty() ? "" : " (" + tags + ")");
              log.info(line);
            });

    log.info("=== End startup timing ===");
  }

  private String formatTags(StartupStep step) {
    StringBuilder sb = new StringBuilder();
    step.getTags()
        .forEach(
            tag -> {
              if (!sb.isEmpty()) {
                sb.append(", ");
              }
              sb.append(tag.getKey()).append("=").append(tag.getValue());
            });
    return sb.toString();
  }
}
