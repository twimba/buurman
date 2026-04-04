import client from './client';
import {
  ContractResponse,
  ContractPartyResponse,
  CreateContractRequest,
  UpdateContractRequest,
  ChangeContractStatusRequest,
  AddContractPartyRequest,
  ChangePrimaryContactRequest,
  CountryMetadataSchema,
} from '../types/contract';
import { DocumentResponse } from '../types/property';
import { RecentActivity } from './dashboard';
import { PageResponse, PageParams } from '@/types/common';

export interface GetContractsParams {
  status?: string;
  propertyIdentifier?: string;
  contactIdentifier?: string;
}

export const getContracts = async (
  params?: GetContractsParams & PageParams
): Promise<PageResponse<ContractResponse>> => {
  const response = await client.get('/contracts', { params });
  return response.data;
};

export const getContract = async (id: string): Promise<ContractResponse> => {
  const response = await client.get(`/contracts/${id}`);
  return response.data;
};

export const createContract = async (
  data: CreateContractRequest
): Promise<ContractResponse> => {
  const response = await client.post('/contracts', data);
  return response.data;
};

export const updateContract = async (
  id: string,
  data: UpdateContractRequest
): Promise<ContractResponse> => {
  const response = await client.put(`/contracts/${id}`, data);
  return response.data;
};

export const deleteContract = async (id: string): Promise<void> => {
  await client.delete(`/contracts/${id}`);
};

export const changeContractStatus = async (
  id: string,
  data: ChangeContractStatusRequest
): Promise<ContractResponse> => {
  const response = await client.post(`/contracts/${id}/change-status`, data);
  return response.data;
};

export const reopenContract = async (id: string): Promise<ContractResponse> => {
  const response = await client.post(`/contracts/${id}/reopen`);
  return response.data;
};

export const duplicateContract = async (
  id: string
): Promise<ContractResponse> => {
  const response = await client.post(`/contracts/${id}/duplicate`);
  return response.data;
};

export const getContractDocuments = async (
  contractId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/contracts/${contractId}/documents`);
  return response.data;
};

export const uploadContractDocument = async (
  contractId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) {
    formData.append('title', title);
  }
  if (notes) {
    formData.append('notes', notes);
  }

  const response = await client.post(
    `/contracts/${contractId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const getContractDocumentDownloadUrl = async (
  documentId: string
): Promise<string> => {
  const response = await client.get(
    `/contracts/documents/${documentId}/download`
  );
  return response.data.url;
};

export const deleteContractDocument = async (
  documentId: string
): Promise<void> => {
  await client.delete(`/contracts/documents/${documentId}`);
};

// --- Country Metadata Schema ---

export const getContractMetadataSchema = async (
  countryCode: string
): Promise<CountryMetadataSchema> => {
  const response = await client.get<CountryMetadataSchema>(
    `/contracts/metadata-schema/${countryCode}`
  );
  return response.data;
};

// --- Contract Party endpoints ---

export const addContractParty = async (
  contractId: string,
  data: AddContractPartyRequest
): Promise<ContractPartyResponse> => {
  const response = await client.post(`/contracts/${contractId}/parties`, data);
  return response.data;
};

export const removeContractParty = async (
  contractId: string,
  partyIdentifier: string
): Promise<void> => {
  await client.delete(`/contracts/${contractId}/parties/${partyIdentifier}`);
};

export const changePrimaryContact = async (
  contractId: string,
  data: ChangePrimaryContactRequest
): Promise<ContractPartyResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/parties/change-primary`,
    data
  );
  return response.data;
};

export const getContractAuditLog = async (
  contractId: string
): Promise<RecentActivity[]> => {
  const response = await client.get(`/contracts/${contractId}/audit-log`);
  return response.data;
};

export interface GeneratePaymentsRequest {
  count: number;
  markAsPaid?: boolean;
  paymentDate?: string;
}

export interface GeneratePaymentsResponse {
  generated: number;
  requested: number;
  markedAsPaid?: number;
}

export const downloadContractBooklet = async (
  contractId: string
): Promise<Blob> => {
  const response = await client.get(`/booklets/contract/${contractId}`, {
    responseType: 'blob',
  });
  return new Blob([response.data], { type: 'application/pdf' });
};

export const generateContractPayments = async (
  contractId: string,
  data: GeneratePaymentsRequest
): Promise<GeneratePaymentsResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/generate-payments`,
    data
  );
  return response.data;
};
