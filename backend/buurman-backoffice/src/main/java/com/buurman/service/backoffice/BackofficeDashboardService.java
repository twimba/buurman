package com.buurman.service.backoffice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.LabelCount;
import com.buurman.dto.response.backoffice.BackofficeDashboardResponse;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeDashboardService {

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;

  @Transactional(readOnly = true)
  public BackofficeDashboardResponse getStats() {
    long totalTeams = teamRepository.countAll();
    long totalUsers = userRepository.countAll();
    long disabledUsers = userRepository.countDisabled();
    long totalNotifications = notificationRepository.countAll();

    List<LabelCount> statusCounts = notificationRepository.countGroupedByStatus();
    List<LabelCount> channelCounts = notificationRepository.countGroupedByChannel();

    long pendingNotifications = 0, failedNotifications = 0, deliveredNotifications = 0;
    for (LabelCount record : statusCounts) {
      String s = record.label();
      int count = record.count();
      switch (s) {
        case "PENDING", "QUEUED" -> pendingNotifications += count;
        case "DELIVERED" -> deliveredNotifications = count;
        case "FAILED", "BOUNCED", "REJECTED" -> failedNotifications += count;
      }
    }

    Map<String, Long> notificationsByChannel = new HashMap<>();
    for (LabelCount record : channelCounts) {
      notificationsByChannel.put(record.label(), (long) record.count());
    }

    return new BackofficeDashboardResponse(
        totalTeams,
        totalUsers,
        disabledUsers,
        totalNotifications,
        pendingNotifications,
        failedNotifications,
        deliveredNotifications,
        notificationsByChannel);
  }
}
