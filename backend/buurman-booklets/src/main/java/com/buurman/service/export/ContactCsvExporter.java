package com.buurman.service.export;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactTag;
import com.buurman.exception.ExternalServiceException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContactRepository.ContactWithCount;
import com.buurman.repository.ContactTagRepository;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContactCsvExporter {

  private final ContactRepository contactRepository;
  private final ContactTagRepository contactTagRepository;

  public byte[] generate(UUID teamId) {
    List<Contact> contacts = contactRepository.findAllByTeamId(teamId);

    List<UUID> contactIds = contacts.stream().map(Contact::getId).toList();
    Map<UUID, List<ContactTag>> tagsByContactId =
        contactTagRepository.findByContactIdsGrouped(contactIds);

    // Build a map of active contract counts
    Map<UUID, Integer> activeCountsByContactId =
        contactRepository.countActiveContractsByContactIds(contactIds, teamId);

    try (StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw)) {

      writer.writeNext(new String[] {
          "Identifier", "Contact Type", "Display Name", "First Name", "Last Name",
          "Company Name", "Trade Name", "Industry", "Email", "Phone",
          "Tax Number", "ID Number", "ID Expiry Date", "Date of Birth",
          "Website", "Invoice Email", "Tags", "Active Contract Count",
          "Data Retention Status", "Created At", "Updated At"
      });

      for (Contact contact : contacts) {
        List<ContactTag> tags = tagsByContactId.getOrDefault(contact.getId(), List.of());
        String tagsStr = tags.stream()
            .map(ContactTag::name)
            .collect(Collectors.joining(","));
        int activeCount = activeCountsByContactId.getOrDefault(contact.getId(), 0);

        writer.writeNext(new String[] {
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
            String.valueOf(activeCount),
            contact.getDataRetentionStatus().name(),
            contact.getCreatedAt() != null ? contact.getCreatedAt().toString() : "",
            contact.getUpdatedAt() != null ? contact.getUpdatedAt().toString() : ""
        });
      }

      // UTF-8 BOM for Excel compatibility
      byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
      byte[] csv = sw.toString().getBytes(StandardCharsets.UTF_8);
      byte[] result = new byte[bom.length + csv.length];
      System.arraycopy(bom, 0, result, 0, bom.length);
      System.arraycopy(csv, 0, result, bom.length, csv.length);
      return result;
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate contacts CSV", e);
    }
  }
}
