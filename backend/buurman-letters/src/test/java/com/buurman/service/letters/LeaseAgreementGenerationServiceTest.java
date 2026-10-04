package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class LeaseAgreementGenerationServiceTest {

  private static final ContractIdentifier CONTRACT =
      ContractIdentifier.of("CON00000000000000000000001");
  private static final UUID TEAM_ID = UUID.randomUUID();

  private final LeaseAgreementExporter exporter = mock(LeaseAgreementExporter.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final S3StorageService s3StorageService = mock(S3StorageService.class);
  private final DocumentMapper documentMapper = mock(DocumentMapper.class);
  private final LeaseAgreementGenerationService service =
      new LeaseAgreementGenerationService(
          exporter, contractRepository, documentRepository, s3StorageService, documentMapper);
  private final UserPrincipal principal =
      new UserPrincipal(
          UUID.randomUUID(), "USR1", "kc-1", "l@example.com", "L", TEAM_ID, "TEA1", null);

  @BeforeEach
  void setUp() throws MalformedURLException {
    Contract contract =
        Contract.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(Sid.of(CONTRACT.value())))
            .documentLanguages(List.of("de", "en"))
            .build();
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT, TEAM_ID)).thenReturn(contract);
    when(s3StorageService.uploadFile(
            any(byte[].class), anyString(), any(), anyString(), any(), anyString()))
        .thenReturn("file-key");
    when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(documentMapper.toResponse(any()))
        .thenReturn(
            new DocumentResponse(
                Sid.of("DOC00000000000000000000001"),
                "CONTRACT",
                Sid.of(CONTRACT.value()),
                "file-key",
                "lease.pdf",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Instant.now(),
                Optional.empty()));
    when(s3StorageService.generatePresignedUrl(anyString()))
        .thenReturn(URI.create("https://s3.example/file-key").toURL());
  }

  @Test
  void rendersInTheRequestedLanguage() {
    when(exporter.generateWithLanguage(CONTRACT, TEAM_ID, "nl"))
        .thenReturn(new LeaseAgreementExporter.RenderedLease(new byte[] {1}, "nl"));

    service.generateAndPersist(CONTRACT, Optional.of("nl"), principal);

    verify(exporter).generateWithLanguage(CONTRACT, TEAM_ID, "nl");
    assertThat(savedDocument().getFileName())
        .isEqualTo("lease-agreement-CON00000000000000000000001-nl.pdf");
  }

  @Test
  void withoutALanguageUsesTheContractsFirstDocumentLanguage() {
    when(exporter.generateWithLanguage(CONTRACT, TEAM_ID, "de"))
        .thenReturn(new LeaseAgreementExporter.RenderedLease(new byte[] {1}, "de"));

    service.generateAndPersist(CONTRACT, Optional.empty(), principal);

    verify(exporter).generateWithLanguage(CONTRACT, TEAM_ID, "de");
    assertThat(savedDocument().getFileName())
        .isEqualTo("lease-agreement-CON00000000000000000000001-de.pdf");
  }

  @Test
  void namesTheDocumentAfterTheLanguageActuallyRendered() {
    // The country has no French document: the exporter falls back to the national language.
    when(exporter.generateWithLanguage(CONTRACT, TEAM_ID, "fr"))
        .thenReturn(new LeaseAgreementExporter.RenderedLease(new byte[] {1}, "nl"));

    service.generateAndPersist(CONTRACT, Optional.of("fr"), principal);

    Document saved = savedDocument();
    assertThat(saved.getFileName()).isEqualTo("lease-agreement-CON00000000000000000000001-nl.pdf");
    assertThat(saved.getTitle()).contains("Lease Agreement (nl)");
  }

  private Document savedDocument() {
    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository).save(captor.capture());
    verify(s3StorageService)
        .uploadFile(
            any(byte[].class),
            anyString(),
            any(),
            eq("CONTRACT"),
            any(),
            eq(captor.getValue().getFileName()));
    return captor.getValue();
  }
}
