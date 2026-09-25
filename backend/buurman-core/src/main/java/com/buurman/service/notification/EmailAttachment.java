package com.buurman.service.notification;

/** A file stored in object storage to attach to an outgoing email. */
public record EmailAttachment(String fileName, String contentType, String fileKey) {}
