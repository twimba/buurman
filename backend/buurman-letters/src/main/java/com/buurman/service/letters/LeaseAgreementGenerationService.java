package com.buurman.service.letters;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.S3StorageService;
import com.buurman.util.DocumentLanguages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseAgreementGenerationService {

  private static final String ENTITY_TYPE = "CONTRACT";

  private final LeaseAgreementExporter leaseAgreementExporter;
  private final ContractRepository contractRepository;
  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final DocumentMapper documentMapper;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public DocumentResponse generateAndPersist(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    String lang = DocumentLanguages.firstSupportedOrDefault(contract.getDocumentLanguages());
    byte[] pdf = leaseAgreementExporter.generate(contractIdentifier, teamId, lang);

    String filename = "lease-agreement-" + contractIdentifier.value() + "-" + lang + ".pdf";
    String title = "Lease Agreement (" + lang + ")";

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

    log.info("Generated lease agreement document for contract {}", contractIdentifier.value());

    return new DocumentResponse(
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
        Optional.of(downloadUrl));
  }
}
