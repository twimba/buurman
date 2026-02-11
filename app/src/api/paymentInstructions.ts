import client from './client';
import {
  PaymentInstructionResponse,
  CreatePaymentInstructionRequest,
  UpdatePaymentInstructionRequest,
  ContractPaymentInstructionResponse,
  CreateContractPaymentInstructionRequest,
  UpdateContractPaymentInstructionRequest,
} from '../types/paymentInstruction';

// Team-level payment instruction templates
export const getPaymentInstructions = async (): Promise<
  PaymentInstructionResponse[]
> => {
  const response = await client.get('/payment-instructions');
  return response.data;
};

export const getPaymentInstruction = async (
  id: string
): Promise<PaymentInstructionResponse> => {
  const response = await client.get(`/payment-instructions/${id}`);
  return response.data;
};

export const createPaymentInstruction = async (
  data: CreatePaymentInstructionRequest
): Promise<PaymentInstructionResponse> => {
  const response = await client.post('/payment-instructions', data);
  return response.data;
};

export const updatePaymentInstruction = async (
  id: string,
  data: UpdatePaymentInstructionRequest
): Promise<PaymentInstructionResponse> => {
  const response = await client.put(`/payment-instructions/${id}`, data);
  return response.data;
};

export const deletePaymentInstruction = async (id: string): Promise<void> => {
  await client.delete(`/payment-instructions/${id}`);
};

// Contract-level payment instructions
export const getContractPaymentInstructions = async (
  contractId: string
): Promise<ContractPaymentInstructionResponse[]> => {
  const response = await client.get(
    `/contracts/${contractId}/payment-instructions`
  );
  return response.data;
};

export const getCurrentContractPaymentInstruction = async (
  contractId: string
): Promise<ContractPaymentInstructionResponse | null> => {
  const response = await client.get(
    `/contracts/${contractId}/payment-instructions/current`
  );
  return response.data;
};

export const createContractPaymentInstruction = async (
  contractId: string,
  data: CreateContractPaymentInstructionRequest
): Promise<ContractPaymentInstructionResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/payment-instructions`,
    data
  );
  return response.data;
};

export const updateContractPaymentInstruction = async (
  contractId: string,
  instructionId: string,
  data: UpdateContractPaymentInstructionRequest
): Promise<ContractPaymentInstructionResponse> => {
  const response = await client.put(
    `/contracts/${contractId}/payment-instructions/${instructionId}`,
    data
  );
  return response.data;
};

export const deleteContractPaymentInstruction = async (
  contractId: string,
  instructionId: string
): Promise<void> => {
  await client.delete(
    `/contracts/${contractId}/payment-instructions/${instructionId}`
  );
};
