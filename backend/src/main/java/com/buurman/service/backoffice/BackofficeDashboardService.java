package com.buurman.service.backoffice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Record2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    List<Record2<String, Integer>> statusCounts = notificationRepository.countGroupedByStatus();
    List<Record2<String, Integer>> channelCounts = notificationRepository.countGroupedByChannel();

    long pendingNotifications = 0, failedNotifications = 0, deliveredNotifications = 0;
    for (Record2<String, Integer> record : statusCounts) {
      String s = record.value1();
      int count = record.value2();
      switch (s) {
        case "PENDING", "QUEUED" -> pendingNotifications += count;
        case "DELIVERED" -> deliveredNotifications = count;
        case "FAILED", "BOUNCED", "REJECTED" -> failedNotifications += count;
      }
    }

    Map<String, Long> notificationsByChannel = new HashMap<>();
    for (Record2<String, Integer> record : channelCounts) {
      notificationsByChannel.put(record.value1(), (long) record.value2());
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
