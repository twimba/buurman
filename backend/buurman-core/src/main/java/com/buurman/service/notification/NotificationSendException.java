package com.buurman.service.notification;

public class NotificationSendException extends Exception {

  public NotificationSendException(String message) {
    super(message);
  }

  public NotificationSendException(String message, Throwable cause) {
    super(message, cause);
  }
}
