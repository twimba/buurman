package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.PAID;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.Expense;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

/** Loads and merges payment + expense data into a unified transaction list. */
@Component
@RequiredArgsConstructor
public class TransactionDataLoader {

  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;

  public List<TransactionRecord> load(
      Optional<LocalDate> startDate, Optional<LocalDate> endDate, UUID teamId) {
    List<TransactionRecord> transactions = new ArrayList<>();

    addPaymentsAsIncome(transactions, startDate, endDate, teamId);
    addExpenses(transactions, startDate, endDate, teamId);

    transactions.sort((a, b) -> b.date().compareTo(a.date()));
    return transactions;
  }

  private void addPaymentsAsIncome(
      List<TransactionRecord> transactions,
      Optional<LocalDate> startDate,
      Optional<LocalDate> endDate,
      UUID teamId) {
    List<Payment> payments =
        (startDate.isPresent() && endDate.isPresent())
            ? paymentRepository.findByDateRange(startDate.get(), endDate.get(), teamId)
            : paymentRepository.findAllByTeamId(teamId);

    for (Payment payment : payments) {
      if (payment.getStatus() == PAID && payment.getPaymentDate().isPresent()) {
        String propertyName = resolvePropertyNameForPayment(payment, teamId);

        transactions.add(
            new TransactionRecord(
                payment.getId().toString(),
                payment.getPaymentDate().get(),
                "INCOME",
                "Rent payment - " + propertyName,
                propertyName,
                Optional.empty(),
                payment.getAmount().value(),
                payment.getAmount().currency()));
      }
    }
  }

  private void addExpenses(
      List<TransactionRecord> transactions,
      Optional<LocalDate> startDate,
      Optional<LocalDate> endDate,
      UUID teamId) {
    List<Expense> expenses =
        (startDate.isPresent() && endDate.isPresent())
            ? expenseRepository.findByDateRange(startDate.get(), endDate.get(), teamId)
            : expenseRepository.findAllByTeamId(teamId);

    for (Expense expense : expenses) {
      Property property =
          propertyRepository.findByIdAndTeamId(expense.getPropertyId(), teamId).orElse(null);
      String propertyName =
          property != null ? property.getStreet() + ", " + property.getCity() : "Unknown Property";

      transactions.add(
          new TransactionRecord(
              expense.getId().toString(),
              expense.getExpenseDate(),
              "EXPENSE",
              expense.getDescription(),
              propertyName,
              Optional.of(expense.getCategory()).map(Enum::name),
              expense.getAmount().value(),
              expense.getAmount().currency()));
    }
  }

  private String resolvePropertyNameForPayment(Payment payment, UUID teamId) {
    Contract contract =
        contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
    Property property =
        contract != null
            ? propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null)
            : null;
    return property != null ? property.getStreet() + ", " + property.getCity() : "Unknown Property";
  }
}
