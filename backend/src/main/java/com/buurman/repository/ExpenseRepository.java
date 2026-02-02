package com.buurman.repository;

import com.buurman.domain.Expense;
import com.buurman.mapper.ExpenseRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.EXPENSES;

@Repository
public class ExpenseRepository {

    private final DSLContext dsl;
    private final ExpenseRecordMapper mapper;

    public ExpenseRepository(DSLContext dsl, ExpenseRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<Expense> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(EXPENSES)
                .where(EXPENSES.ID.eq(id)
                        .and(EXPENSES.TEAM_ID.eq(teamId))
                        .and(EXPENSES.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Expense> findAllByTeamId(UUID teamId) {
        return dsl.selectFrom(EXPENSES)
                .where(EXPENSES.TEAM_ID.eq(teamId)
                        .and(EXPENSES.DELETED_AT.isNull()))
                .orderBy(EXPENSES.EXPENSE_DATE.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Expense> findByPropertyId(UUID propertyId, UUID teamId) {
        return dsl.selectFrom(EXPENSES)
                .where(EXPENSES.PROPERTY_ID.eq(propertyId)
                        .and(EXPENSES.TEAM_ID.eq(teamId))
                        .and(EXPENSES.DELETED_AT.isNull()))
                .orderBy(EXPENSES.EXPENSE_DATE.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Expense> findByCategory(Expense.ExpenseCategory category, UUID teamId) {
        return dsl.selectFrom(EXPENSES)
                .where(EXPENSES.CATEGORY.eq(category.name())
                        .and(EXPENSES.TEAM_ID.eq(teamId))
                        .and(EXPENSES.DELETED_AT.isNull()))
                .orderBy(EXPENSES.EXPENSE_DATE.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate, UUID teamId) {
        return dsl.selectFrom(EXPENSES)
                .where(EXPENSES.TEAM_ID.eq(teamId)
                        .and(EXPENSES.EXPENSE_DATE.between(startDate, endDate))
                        .and(EXPENSES.DELETED_AT.isNull()))
                .orderBy(EXPENSES.EXPENSE_DATE.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Expense save(Expense expense) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (expense.getId() == null) {
            // Insert
            UUID id = UUID.randomUUID();
            LocalDateTime createdAt = expense.getCreatedAt() != null
                    ? LocalDateTime.ofInstant(expense.getCreatedAt(), ZoneOffset.UTC)
                    : now;
            LocalDateTime updatedAt = expense.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(expense.getUpdatedAt(), ZoneOffset.UTC)
                    : now;

            dsl.insertInto(EXPENSES)
                    .set(EXPENSES.ID, id)
                    .set(EXPENSES.IDENTIFIER, expense.getIdentifier())
                    .set(EXPENSES.TEAM_ID, expense.getTeamId())
                    .set(EXPENSES.PROPERTY_ID, expense.getPropertyId())
                    .set(EXPENSES.CATEGORY, expense.getCategory().name())
                    .set(EXPENSES.AMOUNT, expense.getAmount())
                    .set(EXPENSES.CURRENCY, expense.getCurrency())
                    .set(EXPENSES.EXPENSE_DATE, expense.getExpenseDate())
                    .set(EXPENSES.DESCRIPTION, expense.getDescription())
                    .set(EXPENSES.NOTES, expense.getNotes())
                    .set(EXPENSES.CREATED_AT, createdAt)
                    .set(EXPENSES.UPDATED_AT, updatedAt)
                    .set(EXPENSES.CREATED_BY, expense.getCreatedBy())
                    .set(EXPENSES.UPDATED_BY, expense.getUpdatedBy())
                    .execute();

            expense.setId(id);
            expense.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
            expense.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));
        } else {
            // Update
            LocalDateTime updatedAt = expense.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(expense.getUpdatedAt(), ZoneOffset.UTC)
                    : now;

            dsl.update(EXPENSES)
                    .set(EXPENSES.CATEGORY, expense.getCategory().name())
                    .set(EXPENSES.AMOUNT, expense.getAmount())
                    .set(EXPENSES.CURRENCY, expense.getCurrency())
                    .set(EXPENSES.EXPENSE_DATE, expense.getExpenseDate())
                    .set(EXPENSES.DESCRIPTION, expense.getDescription())
                    .set(EXPENSES.NOTES, expense.getNotes())
                    .set(EXPENSES.UPDATED_AT, updatedAt)
                    .set(EXPENSES.UPDATED_BY, expense.getUpdatedBy())
                    .where(EXPENSES.ID.eq(expense.getId())
                            .and(EXPENSES.TEAM_ID.eq(expense.getTeamId())))
                    .execute();

            expense.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));
        }

        return expense;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(EXPENSES)
                .set(EXPENSES.DELETED_AT, now)
                .where(EXPENSES.ID.eq(id)
                        .and(EXPENSES.TEAM_ID.eq(teamId)))
                .execute();
    }
}
