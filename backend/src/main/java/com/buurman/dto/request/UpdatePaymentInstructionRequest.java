package com.buurman.dto.request;

import com.buurman.domain.PaymentInstruction;
import jakarta.validation.constraints.Size;

public record UpdatePaymentInstructionRequest(
        String name,
        String description,
        PaymentInstruction.PaymentMethod paymentMethod,
        String bankName,
        String accountHolderName,

        @Size(max = 34)
        String iban,

        @Size(max = 11)
        String bicSwift,

        String accountNumber,
        String routingNumber,
        String paymentReference,
        String additionalDetails,
        Boolean isDefault
) {
}
