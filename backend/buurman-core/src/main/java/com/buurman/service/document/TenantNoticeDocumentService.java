package com.buurman.service.document;

import java.math.BigDecimal;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.Payment;

/**
 * Renders tenant-facing notice PDFs. Implemented by the letters module; core resolves it lazily so
 * a deployment without the PDF engine still sends plain reminders.
 */
public interface TenantNoticeDocumentService {

  /** Formal notice of overdue rent, attached to FINAL-tone reminders. */
  byte[] renderFormalNotice(FormalNoticeData data);

  record FormalNoticeData(
      Payment payment,
      Contract contract,
      Contact contact,
      BigDecimal outstanding,
      int daysOverdue,
      String languageTag) {}
}
