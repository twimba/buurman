package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ContactListItemResponse(
    Sid identifier,
    ContactType contactType,
    String displayName,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> companyName,
    Optional<String> email,
    Optional<String> phone,
    Optional<String> mainPhotoThumbnailUrl,
    List<ContactTag> tags,
    int activeContractCount,
    Instant createdAt) {}
