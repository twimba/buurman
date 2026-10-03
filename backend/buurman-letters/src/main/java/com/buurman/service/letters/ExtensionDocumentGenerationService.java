package com.buurman.service.letters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.GenerateExtensionDocumentsRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.S3StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtensionDocumentGenerationService {

  private static final String ENTITY_TYPE = "CONTRACT";

  private final ContractExtensionAddendumExporter addendumExporter;
  private final RentIncreaseLetterExporter rentIncreaseLetterExporter;
  private final ContractExtensionRepository extensionRepository;
  private final ContractRepository contractRepository;
  private final DocumentRepository documentRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final S3StorageService s3StorageService;
  private final DocumentMapper documentMapper;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public List<DocumentResponse> generateAndPersist(
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      GenerateExtensionDocumentsRequest request,
      UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    var extension = extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);
    if (!extension.getContractId().equals(contract.getId())) {
      throw new BadRequestException("Extension does not belong to this contract");
    }

    List<String> languages =
        Optional.ofNullable(request.languages())
            .flatMap(opt -> opt)
            .filter(list -> !list.isEmpty())
            .orElseGet(
                () -> {
                  List<String> contractLangs = contract.getDocumentLanguages();
                  if (contractLangs != null && !contractLangs.isEmpty()) {
                    return contractLangs;
                  }
                  return List.of(
                      teamPreferencesRepository.getByTeamId(teamId).getDefaultLanguage());
                });

    List<String> validTypes = List.of("EXTENSION_ADDENDUM", "RENT_INCREASE_LETTER");
    for (String type : request.documentTypes()) {
      if (!validTypes.contains(type)) {
        throw new BadRequestException(
            "Invalid document type: " + type + ". Valid types: " + validTypes);
      }
    }

    boolean replace = request.replaceExisting().orElse(false);
    if (replace) {
      deleteExistingExtensionDocuments(contract, extension.getExtensionNumber(), teamId);
    }

    List<DocumentResponse> results = new ArrayList<>();

    for (String documentType : request.documentTypes()) {
      for (String lang : languages) {
        byte[] pdf =
            generatePdf(documentType, contractIdentifier, extensionIdentifier, teamId, lang);

        String filename = buildFilename(documentType, extension.getExtensionNumber(), lang);
        String title = buildTitle(documentType, extension.getExtensionNumber(), lang);

        Sid contractSid = contract.getIdentifier().orElseThrow();
        String fileKey =
            s3StorageService.uploadFile(
                pdf,
                MediaType.APPLICATION_PDF_VALUE,
                Sid.of(principal.requireTeamIdentifier()),
                ENTITY_TYPE,
                contractSid,
                filename);

        Document document =
            Document.builder()
                .teamId(teamId)
                .entityType(ENTITY_TYPE)
                .entityId(contract.getId())
                .fileKey(fileKey)
                .fileName(filename)
                .fileSize((long) pdf.length)
                .mimeType(MediaType.APPLICATION_PDF_VALUE)
                .title(Optional.of(title))
                .notes(Optional.empty())
                .uploadedBy(principal.getUserId())
                .build();

        Document saved = documentRepository.save(document);

        DocumentResponse response = documentMapper.toResponse(saved);
        String downloadUrl = s3StorageService.generatePresignedUrl(saved.getFileKey()).toString();
        results.add(
            new DocumentResponse(
                response.identifier(),
                response.entityType(),
                response.entityIdentifier(),
                response.fileKey(),
                response.fileName(),
                response.fileSize(),
                response.mimeType(),
                response.title(),
                response.notes(),
                response.sourceDocumentIdentifier(),
                response.uploadedAt(),
                Optional.of(downloadUrl)));
      }
    }

    log.info(
        "Generated {} documents for extension {} on contract {}",
        results.size(),
        extensionIdentifier.value(),
        contractIdentifier.value());

    return results;
  }

  private byte[] generatePdf(
      String documentType,
      ContractIdentifier contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      UUID teamId,
      String lang) {
    return switch (documentType) {
      case "EXTENSION_ADDENDUM" ->
          addendumExporter.generate(
              Optional.of(contractIdentifier), extensionIdentifier, teamId, lang);
      case "RENT_INCREASE_LETTER" ->
          rentIncreaseLetterExporter.generate(
              Optional.of(contractIdentifier), extensionIdentifier, teamId, lang);
      default -> throw new BadRequestException("Unknown document type: " + documentType);
    };
  }

  private String buildFilename(String documentType, int extensionNumber, String lang) {
    String base =
        switch (documentType) {
          case "EXTENSION_ADDENDUM" -> "extension-addendum";
          case "RENT_INCREASE_LETTER" -> "rent-increase-letter";
          default -> "document";
        };
    return base + "-" + extensionNumber + "-" + lang + ".pdf";
  }

  private String buildTitle(String documentType, int extensionNumber, String lang) {
    String typeName =
        switch (documentType) {
          case "EXTENSION_ADDENDUM" -> "Addendum";
          case "RENT_INCREASE_LETTER" -> "Rent Increase Letter";
          default -> "Document";
        };
    return "Extension #" + extensionNumber + " - " + typeName + " (" + lang + ")";
  }

  private void deleteExistingExtensionDocuments(
      Contract contract, int extensionNumber, UUID teamId) {
    String filenamePattern = "%-" + extensionNumber + "-%.pdf";
    List<Document> existing =
        documentRepository.findByEntityAndFileNamePatternAndTeamId(
            ENTITY_TYPE, contract.getId(), filenamePattern, teamId);

    for (Document doc : existing) {
      documentRepository.softDeleteByIdAndTeamId(doc.getId(), teamId);
      s3StorageService.deleteFile(doc.getFileKey());
    }

    if (!existing.isEmpty()) {
      log.info(
          "Deleted {} existing documents for extension #{} before regeneration",
          existing.size(),
          extensionNumber);
    }
  }
}
