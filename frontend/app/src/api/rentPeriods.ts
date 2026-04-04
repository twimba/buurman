import client from './client';
import {
  RentPeriodResponse,
  CreateRentPeriodRequest,
  UpdateRentPeriodRequest,
} from '../types/contract';
import type { DocumentResponse } from '@/types/property';

export const getRentPeriods = async (
  contractId: string
): Promise<RentPeriodResponse[]> => {
  const response = await client.get(`/contracts/${contractId}/rent-periods`);
  return response.data;
};

export const addRentPeriod = async (
  contractId: string,
  data: CreateRentPeriodRequest
): Promise<RentPeriodResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/rent-periods`,
    data
  );
  return response.data;
};

export const updateRentPeriod = async (
  contractId: string,
  periodIdentifier: string,
  data: UpdateRentPeriodRequest
): Promise<RentPeriodResponse> => {
  const response = await client.put(
    `/contracts/${contractId}/rent-periods/${periodIdentifier}`,
    data
  );
  return response.data;
};

export const deleteRentPeriod = async (
  contractId: string,
  periodIdentifier: string
): Promise<void> => {
  await client.delete(
    `/contracts/${contractId}/rent-periods/${periodIdentifier}`
  );
};

export interface GenerateRentChangeDocumentsRequest {
  languages?: string[];
  replaceExisting?: boolean;
}

export const generateRentChangeDocuments = async (
  contractId: string,
  periodId: string,
  request: GenerateRentChangeDocumentsRequest
): Promise<DocumentResponse[]> => {
  const response = await client.post(
    `/contracts/${contractId}/rent-periods/${periodId}/generate-documents`,
    request
  );
  return response.data;
};
