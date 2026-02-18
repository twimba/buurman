package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CreateRentPeriodRequest;
import com.buurman.dto.request.UpdateRentPeriodRequest;
import com.buurman.dto.response.RentPeriodResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContractRentPeriodService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contracts/{contractIdentifier}/rent-periods")
@Tag(name = "Contract Rent Periods", description = "Rent timeline management for contracts")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ContractRentPeriodController {

  private final ContractRentPeriodService rentPeriodService;

  @Operation(summary = "Get rent timeline", description = "Get all rent periods for a contract")
  @GetMapping
  public List<RentPeriodResponse> getRentTimeline(
      @PathVariable String contractIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return rentPeriodService.getRentTimeline(contractIdentifier, principal);
  }

  @Operation(
      summary = "Add rent period",
      description = "Add a new rent period to a contract (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public RentPeriodResponse addRentPeriod(
      @PathVariable String contractIdentifier,
      @Valid @RequestBody CreateRentPeriodRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return rentPeriodService.addRentPeriod(contractIdentifier, request, principal);
  }

  @Operation(
      summary = "Update rent period",
      description = "Update a future rent period (Admin/Editor)")
  @PutMapping("/{periodIdentifier}")
  public RentPeriodResponse updateRentPeriod(
      @PathVariable String contractIdentifier,
      @PathVariable String periodIdentifier,
      @Valid @RequestBody UpdateRentPeriodRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return rentPeriodService.updateRentPeriod(
        contractIdentifier, periodIdentifier, request, principal);
  }

  @Operation(
      summary = "Delete rent period",
      description = "Soft-delete a future rent period (Admin/Editor)")
  @DeleteMapping("/{periodIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteRentPeriod(
      @PathVariable String contractIdentifier,
      @PathVariable String periodIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    rentPeriodService.deleteRentPeriod(contractIdentifier, periodIdentifier, principal);
  }
}
