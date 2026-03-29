package com.buurman.service.export;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactTag;
import com.buurman.exception.ExternalServiceException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContactTagRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContactExcelExporter {

  private final ContactRepository contactRepository;
  private final ContactTagRepository contactTagRepository;

  public byte[] generate(UUID teamId) {
    List<Contact> contacts = contactRepository.findAllByTeamId(teamId);

    List<UUID> contactIds = contacts.stream().map(Contact::getId).toList();
    Map<UUID, List<ContactTag>> tagsByContactId =
        contactTagRepository.findByContactIdsGrouped(contactIds, teamId);
    Map<UUID, Integer> activeCountsByContactId =
        contactRepository.countActiveContractsByContactIds(contactIds, teamId);

    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Contacts");

      Font boldFont = workbook.createFont();
      boldFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(boldFont);

      String[] headers = {
        "Identifier",
        "Contact Type",
        "Display Name",
        "First Name",
        "Last Name",
        "Company Name",
        "Trade Name",
        "Industry",
        "Email",
        "Phone",
        "Tax Number",
        "ID Number",
        "ID Expiry Date",
        "Date of Birth",
        "Website",
        "Invoice Email",
        "Tags",
        "Active Contract Count",
        "Data Retention Status",
        "Created At",
        "Updated At"
      };

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowNum = 1;
      for (Contact contact : contacts) {
        List<ContactTag> tags = tagsByContactId.getOrDefault(contact.getId(), List.of());
        String tagsStr = tags.stream().map(ContactTag::name).collect(Collectors.joining(", "));
        int activeCount = activeCountsByContactId.getOrDefault(contact.getId(), 0);

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(contact.getIdentifier().map(Object::toString).orElse(""));
        row.createCell(1).setCellValue(contact.getContactType().name());
        row.createCell(2).setCellValue(contact.getDisplayName());
        row.createCell(3).setCellValue(contact.getFirstName().orElse(""));
        row.createCell(4).setCellValue(contact.getLastName().orElse(""));
        row.createCell(5).setCellValue(contact.getCompanyName().orElse(""));
        row.createCell(6).setCellValue(contact.getTradeName().orElse(""));
        row.createCell(7).setCellValue(contact.getIndustry().orElse(""));
        row.createCell(8).setCellValue(contact.getEmail().orElse(""));
        row.createCell(9).setCellValue(contact.getPhone().orElse(""));
        row.createCell(10).setCellValue(contact.getTaxNumber().orElse(""));
        row.createCell(11).setCellValue(contact.getIdNumber().orElse(""));
        row.createCell(12).setCellValue(contact.getIdExpiryDate().map(Object::toString).orElse(""));
        row.createCell(13).setCellValue(contact.getDateOfBirth().map(Object::toString).orElse(""));
        row.createCell(14).setCellValue(contact.getWebsite().orElse(""));
        row.createCell(15).setCellValue(contact.getInvoiceEmail().orElse(""));
        row.createCell(16).setCellValue(tagsStr);
        row.createCell(17).setCellValue(activeCount);
        row.createCell(18).setCellValue(contact.getDataRetentionStatus().name());
        row.createCell(19)
            .setCellValue(contact.getCreatedAt() != null ? contact.getCreatedAt().toString() : "");
        row.createCell(20)
            .setCellValue(contact.getUpdatedAt() != null ? contact.getUpdatedAt().toString() : "");
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(os);
      return os.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate contacts Excel", e);
    }
  }
}
