package com.buurman.service;

import java.util.UUID;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;

/**
 * SPI for generating the contract termination notice letter. {@code buurman-core} cannot depend on
 * {@code buurman-letters} (dependency runs the other way — letters depends on core), so this
 * interface lives here and {@code ContractTerminationLetterExporter} (in {@code buurman-letters})
 * implements it; Spring wires the implementation in at runtime via the assembled app module. Same
 * pattern as {@link com.buurman.service.notification.NotificationService}.
 */
public interface ContractTerminationLetterGenerator {

  byte[] generate(Contract contract, ContractTermination termination, UUID teamId);
}
