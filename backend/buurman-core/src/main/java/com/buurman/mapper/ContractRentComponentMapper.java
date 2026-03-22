package com.buurman.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.buurman.domain.ContractRentComponent;
import com.buurman.dto.response.RentComponentResponse;

@Component
public class ContractRentComponentMapper {

  public RentComponentResponse toResponse(ContractRentComponent component) {
    return new RentComponentResponse(
        component.getIdentifier().orElseThrow(),
        component.getComponentType(),
        component.getComponentType().getDisplayName(),
        component.getAmount().value(),
        component.getAmount().currency(),
        component.getDescription(),
        component.getSortOrder());
  }

  public List<RentComponentResponse> toResponses(List<ContractRentComponent> components) {
    return components.stream().map(this::toResponse).toList();
  }
}
