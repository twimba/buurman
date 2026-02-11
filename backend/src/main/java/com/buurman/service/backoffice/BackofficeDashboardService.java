package com.buurman.service.backoffice;

import com.buurman.dto.response.backoffice.BackofficeDashboardResponse;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import org.jooq.Record2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackofficeDashboardService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeDashboardService.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public BackofficeDashboardService(TeamRepository teamRepository,
                                       UserRepository userRepository,
                                       NotificationRepository notificationRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

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
                notificationsByChannel
        );
    }
}
