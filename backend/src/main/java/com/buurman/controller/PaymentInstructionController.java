package com.buurman.controller;

import com.buurman.dto.request.CreatePaymentInstructionRequest;
import com.buurman.dto.request.UpdatePaymentInstructionRequest;
import com.buurman.dto.response.PaymentInstructionResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PaymentInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/payment-instructions")
@Tag(name = "Payment Instructions", description = "Team-level payment instruction templates")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PaymentInstructionController {

    private final PaymentInstructionService paymentInstructionService;


    @Operation(summary = "List payment instructions", description = "Get all payment instruction templates for the team")
    @GetMapping
    public List<PaymentInstructionResponse> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentInstructionService.getAll(principal);
    }

    @Operation(summary = "Get payment instruction", description = "Get a payment instruction template by identifier")
    @GetMapping("/{identifier}")
    public PaymentInstructionResponse getByIdentifier(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentInstructionService.getByIdentifier(identifier, principal);
    }

    @Operation(summary = "Create payment instruction", description = "Create a payment instruction template (Admin/Editor)")
    @PostMapping
    @ResponseStatus(CREATED)
    public PaymentInstructionResponse create(
            @Valid @RequestBody CreatePaymentInstructionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentInstructionService.create(request, principal);
    }

    @Operation(summary = "Update payment instruction", description = "Update a payment instruction template (Admin/Editor)")
    @PutMapping("/{identifier}")
    public PaymentInstructionResponse update(
            @PathVariable String identifier,
            @Valid @RequestBody UpdatePaymentInstructionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentInstructionService.update(identifier, request, principal);
    }

    @Operation(summary = "Delete payment instruction", description = "Soft delete a payment instruction template (Admin/Editor)")
    @DeleteMapping("/{identifier}")
    @ResponseStatus(NO_CONTENT)
    public void delete(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        paymentInstructionService.delete(identifier, principal);
    }
}
