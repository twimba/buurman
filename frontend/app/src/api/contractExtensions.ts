import client from './client';
import {
  ContractExtensionResponse,
  CreateContractExtensionRequest,
  DeclineContractExtensionRequest,
  UpcomingRenewalResponse,
  JurisdictionDefaultResponse,
} from '../types/contractExtension';
import { PageResponse } from '@/types/common';
import type { DocumentResponse } from '@/types/property';

export const listExtensions = async (
  contractId: string,
  page?: number,
  size?: number
): Promise<PageResponse<ContractExtensionResponse>> => {
  const response = await client.get(`/contracts/${contractId}/extensions`, {
    params: { page, size },
  });
  return response.data;
};

export const getExtension = async (
  contractId: string,
  extensionId: string
): Promise<ContractExtensionResponse> => {
  const response = await client.get(
    `/contracts/${contractId}/extensions/${extensionId}`
  );
  return response.data;
};

export const createExtension = async (
  contractId: string,
  request: CreateContractExtensionRequest
): Promise<ContractExtensionResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/extensions`,
    request
  );
  return response.data;
};

export const activateExtension = async (
  contractId: string,
  extensionId: string
): Promise<ContractExtensionResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/extensions/${extensionId}/activate`
  );
  return response.data;
};

export const confirmExtension = async (
  contractId: string,
  extensionId: string
): Promise<ContractExtensionResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/extensions/${extensionId}/confirm`
  );
  return response.data;
};

export const declineExtension = async (
  contractId: string,
  extensionId: string,
  request: DeclineContractExtensionRequest
): Promise<ContractExtensionResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/extensions/${extensionId}/decline`,
    request
  );
  return response.data;
};

export const cancelExtension = async (
  contractId: string,
  extensionId: string,
  deleteDocuments?: boolean
): Promise<void> => {
  await client.delete(`/contracts/${contractId}/extensions/${extensionId}`, {
    params: deleteDocuments ? { deleteDocuments: true } : undefined,
  });
};

export const getUpcomingRenewals = async (): Promise<
  UpcomingRenewalResponse[]
> => {
  const response = await client.get('/dashboard/upcoming-renewals');
  return response.data;
};

export const getPendingExtensions = async (): Promise<
  ContractExtensionResponse[]
> => {
  const response = await client.get('/dashboard/pending-extensions');
  return response.data;
};

export const downloadAddendum = async (
  contractId: string,
  extensionId: string,
  lang?: string
): Promise<Blob> => {
  const response = await client.get(
    `/contracts/${contractId}/extensions/${extensionId}/addendum`,
    { responseType: 'blob', params: { lang } }
  );
  return response.data;
};

export const downloadRentIncreaseLetter = async (
  contractId: string,
  extensionId: string,
  lang?: string
): Promise<Blob> => {
  const response = await client.get(
    `/contracts/${contractId}/extensions/${extensionId}/rent-increase-letter`,
    { responseType: 'blob', params: { lang } }
  );
  return response.data;
};

export interface GenerateExtensionDocumentsRequest {
  documentTypes: ('EXTENSION_ADDENDUM' | 'RENT_INCREASE_LETTER')[];
  languages?: string[];
  replaceExisting?: boolean;
}

export const generateExtensionDocuments = async (
  contractId: string,
  extensionId: string,
  request: GenerateExtensionDocumentsRequest
): Promise<DocumentResponse[]> => {
  const response = await client.post(
    `/contracts/${contractId}/extensions/${extensionId}/generate-documents`,
    request
  );
  return response.data;
};

export const getJurisdictionDefaults = async (
  countryCode: string,
  regionCode?: string,
  landlordType?: string,
  furnished?: boolean
): Promise<JurisdictionDefaultResponse> => {
  const response = await client.get(`/jurisdiction-defaults`, {
    params: { countryCode, regionCode, landlordType, furnished },
  });
  return response.data;
};
