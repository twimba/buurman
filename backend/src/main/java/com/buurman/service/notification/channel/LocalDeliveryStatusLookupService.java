package com.buurman.service.notification.channel;

import com.buurman.domain.Notification;
import com.buurman.service.notification.DeliveryStatusLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class LocalDeliveryStatusLookupService implements DeliveryStatusLookupService {

    private static final Logger log = LoggerFactory.getLogger(LocalDeliveryStatusLookupService.class);

    @Override
    public Notification refreshStatus(Notification notification) {
        log.debug("Status refresh not available in local profile for notification {}",
                notification.getIdentifier());
        
        return notification;
    }
}
