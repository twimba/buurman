package com.buurman.controller;

import com.buurman.domain.Expense;
import com.buurman.dto.request.CreateExpenseRequest;
import com.buurman.dto.request.UpdateExpenseRequest;
import com.buurman.dto.response.ExpenseResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expenses", description = "Property expense tracking and management")
@SecurityRequirement(name = "bearer-jwt")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Operation(summary = "Create expense", description = "Record a new property expense (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ExpenseResponse createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return expenseService.createExpense(request, principal);
    }

    @Operation(summary = "List expenses", description = "Get all expenses with optional filters")
    @GetMapping
    public List<ExpenseResponse> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID propertyId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (propertyId != null) {
            return expenseService.getExpensesByProperty(propertyId, principal);
        }

        if (category != null) {
            try {
                Expense.ExpenseCategory expenseCategory = Expense.ExpenseCategory.valueOf(category.toUpperCase());
                return expenseService.getExpensesByCategory(expenseCategory, principal);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid category: " + category);
            }
        }

        return expenseService.getAllExpenses(principal);
    }

    @Operation(summary = "Get expense details", description = "Get details of a specific expense")
    @GetMapping("/{id}")
    public ExpenseResponse getExpense(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return expenseService.getExpense(id, principal);
    }

    @Operation(summary = "Update expense", description = "Update expense information (Admin/Editor)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ExpenseResponse updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return expenseService.updateExpense(id, request, principal);
    }

    @Operation(summary = "Delete expense", description = "Soft delete an expense (Admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteExpense(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        expenseService.deleteExpense(id, principal);
    }
}
