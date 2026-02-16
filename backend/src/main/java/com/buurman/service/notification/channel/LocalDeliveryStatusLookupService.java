package com.buurman.service.notification.channel;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.buurman.domain.Notification;
import com.buurman.service.notification.DeliveryStatusLookupService;

import lombok.extern.slf4j.Slf4j;

@Service
@Profile("local")
@Slf4j
public class LocalDeliveryStatusLookupService implements DeliveryStatusLookupService {

  @Override
  public Notification refreshStatus(Notification notification) {
    log.debug(
        "Status refresh not available in local profile for notification {}",
        notification.getIdentifier());

    return notification;
  }
}
