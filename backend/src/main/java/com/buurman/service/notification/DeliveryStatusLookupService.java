package com.buurman.service.notification;

import com.buurman.domain.Notification;

public interface DeliveryStatusLookupService {

    /**
     * Refreshes the delivery status of a notification by querying the provider API.
     * Returns the updated notification.
     */
    Notification refreshStatus(Notification notification);
}
