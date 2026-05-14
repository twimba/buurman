package com.buurman.service.export.tabular;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactTag;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContactTagRepository;

import lombok.RequiredArgsConstructor;

/**
 * Loads contacts (+ tags + active-contract counts) for a team and produces a single-sheet {@link
 * TabularExport}. Consumed by every Contact export format: CSV, Excel, Google Sheets.
 */
@Component
@RequiredArgsConstructor
public class ContactTabularExportBuilder {

  private final ContactRepository contactRepository;
  private final ContactTagRepository contactTagRepository;

  public TabularExport build(UUID teamId) {
    List<Contact> contacts = contactRepository.findAllByTeamId(teamId);
    List<UUID> contactIds = contacts.stream().map(Contact::getId).toList();
    Map<UUID, List<ContactTag>> tagsByContactId =
        contactTagRepository.findByContactIdsGrouped(contactIds, teamId);
    Map<UUID, Integer> activeCountsByContactId =
        contactRepository.countActiveContractsByContactIds(contactIds, teamId);

    TabularExport export = new TabularExport("contacts");
    TabularSheet sheet =
        export.addSheet(
            "Contacts",
            List.of(
                TabularColumn.text("Identifier"),
                TabularColumn.text("Contact Type"),
                TabularColumn.text("Display Name"),
                TabularColumn.text("First Name"),
                TabularColumn.text("Last Name"),
                TabularColumn.text("Company Name"),
                TabularColumn.text("Trade Name"),
                TabularColumn.text("Industry"),
                TabularColumn.text("Email"),
                TabularColumn.text("Phone"),
                TabularColumn.text("Tax Number"),
                TabularColumn.text("ID Number"),
                TabularColumn.of("ID Expiry Date", TabularColumnFormat.DATE),
                TabularColumn.of("Date of Birth", TabularColumnFormat.DATE),
                TabularColumn.text("Website"),
                TabularColumn.text("Invoice Email"),
                TabularColumn.text("Tags"),
                TabularColumn.of("Active Contract Count", TabularColumnFormat.INTEGER),
                TabularColumn.text("Data Retention Status"),
                TabularColumn.text("Created At"),
                TabularColumn.text("Updated At")));

    for (Contact contact : contacts) {
      List<ContactTag> tags = tagsByContactId.getOrDefault(contact.getId(), List.of());
      String tagsStr = tags.stream().map(ContactTag::name).collect(Collectors.joining(", "));
      int activeCount = activeCountsByContactId.getOrDefault(contact.getId(), 0);

      sheet.addRow(
          contact.getIdentifier().map(Object::toString).orElse(""),
          contact.getContactType().name(),
          contact.getDisplayName(),
          contact.getFirstName().orElse(""),
          contact.getLastName().orElse(""),
          contact.getCompanyName().orElse(""),
          contact.getTradeName().orElse(""),
          contact.getIndustry().orElse(""),
          contact.getEmail().orElse(""),
          contact.getPhone().orElse(""),
          contact.getTaxNumber().orElse(""),
          contact.getIdNumber().orElse(""),
          contact.getIdExpiryDate().map(Object::toString).orElse(""),
          contact.getDateOfBirth().map(Object::toString).orElse(""),
          contact.getWebsite().orElse(""),
          contact.getInvoiceEmail().orElse(""),
          tagsStr,
          activeCount,
          contact.getDataRetentionStatus().name(),
          contact.getCreatedAt() != null ? contact.getCreatedAt().toString() : "",
          contact.getUpdatedAt() != null ? contact.getUpdatedAt().toString() : "");
    }
    return export;
  }
}
