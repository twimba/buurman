package com.buurman.controller;

import com.buurman.dto.request.CreateContractPaymentInstructionRequest;
import com.buurman.dto.request.UpdateContractPaymentInstructionRequest;
import com.buurman.dto.response.ContractPaymentInstructionResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContractPaymentInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts/{contractIdentifier}/payment-instructions")
@Tag(name = "Contract Payment Instructions", description = "Per-contract payment instructions with history")
@SecurityRequirement(name = "bearer-jwt")
public class ContractPaymentInstructionController {

    private final ContractPaymentInstructionService service;

    public ContractPaymentInstructionController(ContractPaymentInstructionService service) {
        this.service = service;
    }

    @Operation(summary = "Get payment instruction history", description = "Get all payment instructions for a contract (full history)")
    @GetMapping
    public List<ContractPaymentInstructionResponse> getHistory(
            @PathVariable String contractIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.getHistory(contractIdentifier, principal);
    }

    @Operation(summary = "Get current payment instruction", description = "Get the currently active payment instruction for a contract")
    @GetMapping("/current")
    public ContractPaymentInstructionResponse getCurrent(
            @PathVariable String contractIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.getCurrent(contractIdentifier, principal);
    }

    @Operation(summary = "Create payment instruction", description = "Assign a payment instruction to a contract (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractPaymentInstructionResponse create(
            @PathVariable String contractIdentifier,
            @Valid @RequestBody CreateContractPaymentInstructionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.create(contractIdentifier, request, principal);
    }

    @Operation(summary = "Update payment instruction", description = "Change payment instruction (closes current, creates new entry) (Admin/Editor)")
    @PutMapping("/{instructionIdentifier}")
    public ContractPaymentInstructionResponse update(
            @PathVariable String contractIdentifier,
            @PathVariable String instructionIdentifier,
            @Valid @RequestBody UpdateContractPaymentInstructionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(contractIdentifier, instructionIdentifier, request, principal);
    }

    @Operation(summary = "Delete payment instruction", description = "Soft delete a payment instruction entry (Admin/Editor)")
    @DeleteMapping("/{instructionIdentifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String contractIdentifier,
            @PathVariable String instructionIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(contractIdentifier, instructionIdentifier, principal);
    }
}
