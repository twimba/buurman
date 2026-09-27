package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.ManualAllocationRequest;
import com.buurman.dto.request.UpdateAllocationRequest;
import com.buurman.dto.response.ExpenseAllocationResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.generated.api.ExpenseAllocationsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExpenseService;
import com.buurman.service.PropertyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ExpenseAllocationController implements ExpenseAllocationsApi {

  private final ExpenseService expenseService;
  private final PropertyService propertyService;

  @Override
  public List<ExpenseAllocationResponse> getExpenseAllocations(ExpenseIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.getExpenseAllocations(identifier, principal);
  }

  @Override
  public List<ExpenseAllocationResponse> updateExpenseAllocations(
      ExpenseIdentifier identifier, ManualAllocationRequest manualAllocationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.overrideExpenseAllocations(
        identifier, manualAllocationRequest, principal);
  }

  @Override
  public List<ExpenseAllocationResponse> recomputeExpenseAllocations(ExpenseIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return expenseService.recomputeExpenseAllocations(identifier, principal);
  }

  @Override
  public PropertyResponse updatePropertyAllocation(
      PropertyIdentifier identifier, UpdateAllocationRequest updateAllocationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.updateAllocation(identifier, updateAllocationRequest, principal);
  }
}
