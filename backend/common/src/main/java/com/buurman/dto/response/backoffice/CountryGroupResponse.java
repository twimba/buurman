package com.buurman.dto.response.backoffice;

import java.util.List;

import com.buurman.util.Generated;

@Generated
public record CountryGroupResponse(
    String groupId, String groupName, List<CountryEntry> countries) {}
