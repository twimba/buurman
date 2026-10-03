package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID ORIGINAL_ID = UUID.randomUUID();
  private static final UUID SIGNED_ID = UUID.randomUUID();
  private static final Sid ORIGINAL_SID = Sid.of("DOC00000000000000000000001");

  @Mock private DocumentRepository documentRepository;
  @Mock private S3StorageService s3StorageService;
  @Mock private DocumentMapper documentMapper;
  @Mock private AuditService auditService;
  @Mock private MetricsService metricsService;
  @Mock private AppProperties appProperties;

  private DocumentService service;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() throws Exception {
    when(appProperties.documents()).thenReturn(new AppProperties.Documents(10_000_000L, List.of()));
    service =
        new DocumentService(
            documentRepository,
            s3StorageService,
            documentMapper,
            auditService,
            metricsService,
            appProperties);
    principal =
        new UserPrincipal(
            UUID.randomUUID(),
            "usr_test",
            "kc-id",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN);
    when(s3StorageService.generatePresignedUrl(any())).thenReturn(new URL("https://example.com/f"));
  }

  private Document signedDocument() {
    return Document.builder()
        .id(SIGNED_ID)
        .teamId(TEAM_ID)
        .entityType("CONTRACT")
        .entityId(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("DOC00000000000000000000002")))
        .fileKey("k")
        .fileName("signed-addendum.pdf")
        .uploadedBy(UUID.randomUUID())
        .sourceDocumentId(Optional.of(ORIGINAL_ID))
        .build();
  }

  private DocumentResponse barebonesResponse(Document document) {
    return new DocumentResponse(
        document.getIdentifier().orElseThrow(),
        document.getEntityType(),
        Sid.of("CON00000000000000000000001"),
        document.getFileKey(),
        document.getFileName(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Instant.now(),
        Optional.empty());
  }

  @Test
  @DisplayName(
      "searchDocuments resolves sourceDocumentIdentifier even though the original isn't itself in"
          + " the result set")
  void searchDocumentsResolvesSourceDocumentIdentifierAcrossTheWholeResultSet() {
    Document signed = signedDocument();
    when(documentRepository.searchDocuments("addendum", null, TEAM_ID)).thenReturn(List.of(signed));
    when(documentMapper.toResponse(signed)).thenReturn(barebonesResponse(signed));
    when(documentRepository.findByIdsAndTeamId(List.of(ORIGINAL_ID), TEAM_ID))
        .thenReturn(
            List.of(
                Document.builder()
                    .id(ORIGINAL_ID)
                    .teamId(TEAM_ID)
                    .identifier(Optional.of(ORIGINAL_SID))
                    .fileKey("k2")
                    .fileName("addendum.pdf")
                    .build()));

    List<DocumentResponse> responses = service.searchDocuments("addendum", null, principal);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).sourceDocumentIdentifier()).contains(ORIGINAL_SID);
  }

  @Test
  @DisplayName("getDocument resolves sourceDocumentIdentifier for a single document")
  void getDocumentResolvesSourceDocumentIdentifier() {
    Document signed = signedDocument();
    when(documentRepository.getByIdentifierAndTeamId(any(), eq(TEAM_ID))).thenReturn(signed);
    when(documentMapper.toResponse(signed)).thenReturn(barebonesResponse(signed));
    when(documentRepository.findByIdsAndTeamId(List.of(ORIGINAL_ID), TEAM_ID))
        .thenReturn(
            List.of(
                Document.builder()
                    .id(ORIGINAL_ID)
                    .teamId(TEAM_ID)
                    .identifier(Optional.of(ORIGINAL_SID))
                    .fileKey("k2")
                    .fileName("addendum.pdf")
                    .build()));

    DocumentResponse response =
        service.getDocument(
            com.buurman.domain.identifier.DocumentIdentifier.of(
                signed.getIdentifier().orElseThrow().value()),
            principal);

    assertThat(response.sourceDocumentIdentifier()).contains(ORIGINAL_SID);
  }
}
