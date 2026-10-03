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
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.dto.request.GenerateRentChangeDocumentsRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.ContractRentPeriodRepository;
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
public class RentChangeDocumentGenerationService {

  private static final String ENTITY_TYPE = "CONTRACT";

  private final RentChangeDocumentExporter rentChangeDocumentExporter;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final ContractRepository contractRepository;
  private final DocumentRepository documentRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final S3StorageService s3StorageService;
  private final DocumentMapper documentMapper;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public List<DocumentResponse> generateAndPersist(
      ContractIdentifier contractIdentifier,
      ContractRentPeriodIdentifier periodIdentifier,
      GenerateRentChangeDocumentsRequest request,
      UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    ContractRentPeriod period =
        rentPeriodRepository
            .findByIdentifierAndTeamId(periodIdentifier, teamId)
            .orElseThrow(() -> new BadRequestException("Rent period not found"));
    if (!period.getContractId().equals(contract.getId())) {
      throw new BadRequestException("Rent period does not belong to this contract");
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

    boolean replace = request.replaceExisting().orElse(false);
    if (replace) {
      deleteExistingRentChangeDocuments(contract, periodIdentifier, teamId);
    }

    List<DocumentResponse> results = new ArrayList<>();

    for (String lang : languages) {
      byte[] pdf =
          rentChangeDocumentExporter.generate(contractIdentifier, periodIdentifier, teamId, lang);

      String filename = "rent-change-" + periodIdentifier.value() + "-" + lang + ".pdf";
      String title = "Rent Change - " + periodIdentifier.value() + " (" + lang + ")";

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

    log.info(
        "Generated {} rent change documents for period {} on contract {}",
        results.size(),
        periodIdentifier.value(),
        contractIdentifier.value());

    return results;
  }

  private void deleteExistingRentChangeDocuments(
      Contract contract, ContractRentPeriodIdentifier periodIdentifier, UUID teamId) {
    String filenamePattern = "rent-change-" + periodIdentifier.value() + "-%.pdf";
    List<Document> existing =
        documentRepository.findByEntityAndFileNamePatternAndTeamId(
            ENTITY_TYPE, contract.getId(), filenamePattern, teamId);

    for (Document doc : existing) {
      documentRepository.softDeleteByIdAndTeamId(doc.getId(), teamId);
      s3StorageService.deleteFile(doc.getFileKey());
    }

    if (!existing.isEmpty()) {
      log.info(
          "Deleted {} existing documents for rent period {} before regeneration",
          existing.size(),
          periodIdentifier.value());
    }
  }
}
