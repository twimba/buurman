package com.buurman.mapper;

import com.buurman.domain.Contract;
import com.buurman.jooq.generated.tables.records.ContractsRecord;
import org.springframework.stereotype.Component;

@Component
public class ContractRecordMapper {

    public Contract toDomain(ContractsRecord record) {
        if (record == null) {
            return null;
        }

        Contract contract = new Contract();
        contract.setId(record.getId());
        contract.setIdentifier(record.getIdentifier());
        contract.setTeamId(record.getTeamId());
        contract.setPropertyId(record.getPropertyId());
        contract.setTenantId(record.getTenantId());
        contract.setContractType(Contract.ContractType.valueOf(record.getContractType()));
        contract.setStartDate(record.getStartDate());
        contract.setEndDate(record.getEndDate());
        contract.setSignedDate(record.getSignedDate());
        contract.setRentAmount(record.getRentAmount());
        contract.setDepositAmount(record.getDepositAmount());
        contract.setSecurityDeposit(record.getSecurityDeposit());
        contract.setCurrency(record.getCurrency());
        contract.setPaymentFrequency(Contract.PaymentFrequency.valueOf(record.getPaymentFrequency()));
        contract.setPaymentDueDay(record.getPaymentDueDay());
        contract.setAutoRenewal(record.getAutoRenewal());
        contract.setRenewalNoticeDays(record.getRenewalNoticeDays());
        contract.setTerminationNoticeDays(record.getTerminationNoticeDays());
        contract.setLateFeePercentage(record.getLateFeePercentage());
        contract.setStatus(Contract.ContractStatus.valueOf(record.getStatus()));
        contract.setTermsAndConditions(record.getTermsAndConditions());
        contract.setNotes(record.getNotes());
        contract.setCreatedAt(record.getCreatedAt() != null ?
                record.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : null);
        contract.setUpdatedAt(record.getUpdatedAt() != null ?
                record.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC) : null);
        contract.setCreatedBy(record.getCreatedBy());
        contract.setUpdatedBy(record.getUpdatedBy());
        contract.setDeletedAt(record.getDeletedAt() != null ?
                record.getDeletedAt().toInstant(java.time.ZoneOffset.UTC) : null);

        return contract;
    }
}
