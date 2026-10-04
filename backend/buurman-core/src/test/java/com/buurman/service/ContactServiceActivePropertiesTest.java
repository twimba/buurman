package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.dto.response.ContactPropertyAssignment;
import com.buurman.dto.response.ContactResponse;
import com.buurman.mapper.ContactMapper;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactNoteRepository;
import com.buurman.repository.ContactRelationshipRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContactTagRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyContactHistoryRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.SidGenerator;

/** The contact detail's "active properties" list, built in {@code ContactService#toResponse}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContactService — active properties")
class ContactServiceActivePropertiesTest {

  private static final UUID TEAM_ID = UUID.randomUUID();

  @Mock private ContactRepository contactRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private PropertyContactHistoryRepository historyRepository;
  @Mock private ContactMapper contactMapper;
  @Mock private AuditService auditService;
  @Mock private UserRepository userRepository;
  @Mock private DocumentService documentService;
  @Mock private PhotoService photoService;
  @Mock private PhotoRepository photoRepository;
  @Mock private S3StorageService s3StorageService;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractPartyRepository contractPartyRepository;
  @Mock private ContactAddressService addressService;
  @Mock private ContactAddressRepository addressRepository;
  @Mock private DocumentRepository documentRepository;
  @Mock private ContactTagRepository contactTagRepository;
  @Mock private ContactNoteRepository contactNoteRepository;
  @Mock private ContactRelationshipRepository contactRelationshipRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private MetricsService metricsService;
  @Mock private Clock clock;

  @InjectMocks private ContactService service;

  @Test
  @DisplayName(
      "a property let under a NOTICE_GIVEN contract is still listed as the contact's active"
          + " property; a TERMINATED one is not")
  void noticeGivenContractPropertyIsListedAsActive() {
    ContactIdentifier identifier = SidGenerator.newContactId();
    Contact contact = Contact.builder().id(UUID.randomUUID()).build();
    Property letProperty =
        Property.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(SidGenerator.newPropertyId()))
            .street("Kerkstraat 14")
            .city("Amsterdam")
            .build();
    Contract underNotice =
        Contract.builder()
            .id(UUID.randomUUID())
            .propertyId(letProperty.getId())
            .status(Contract.ContractStatus.NOTICE_GIVEN)
            .build();
    Contract terminated =
        Contract.builder()
            .id(UUID.randomUUID())
            .propertyId(UUID.randomUUID())
            .status(Contract.ContractStatus.TERMINATED)
            .build();
    when(contactRepository.getByIdentifierAndTeamId(identifier, TEAM_ID)).thenReturn(contact);
    when(contactMapper.toResponse(contact)).thenReturn(mock(ContactResponse.class));
    when(contractRepository.findByContactIdViaParties(contact.getId(), TEAM_ID))
        .thenReturn(List.of(underNotice, terminated));
    when(propertyRepository.findByIdsAndTeamId(java.util.Set.of(letProperty.getId()), TEAM_ID))
        .thenReturn(List.of(letProperty));

    ContactResponse response = service.getContact(identifier, principal());

    assertThat(response.activeProperties())
        .singleElement()
        .extracting(ContactPropertyAssignment::property)
        .satisfies(p -> assertThat(p.street()).isEqualTo("Kerkstraat 14"));
  }

  private static UserPrincipal principal() {
    return new UserPrincipal(
        UUID.randomUUID(),
        "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
        "kc-123",
        "user@example.com",
        "John Doe",
        TEAM_ID,
        "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
        TeamRole.TEAM_ADMIN,
        true,
        true);
  }
}
