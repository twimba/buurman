package com.buurman.dto.response.backoffice;

import java.util.List;

public record CountryGroupResponse(
    String groupId,
    String groupName,
    List<CountryEntry> countries
) {}
