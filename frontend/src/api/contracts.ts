import client from './client';
import {
  ContractResponse,
  CreateContractRequest,
  UpdateContractRequest,
  ChangeContractStatusRequest,
} from '../types/contract';
import { DocumentResponse } from '../types/property';
import { RecentActivity } from './dashboard';

export interface GetContractsParams {
  status?: string;
  propertyId?: string;
  tenantId?: string;
}

export const getContracts = async (
  params?: GetContractsParams
): Promise<ContractResponse[]> => {
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
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

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

export const getContractAuditLog = async (
  contractId: string
): Promise<RecentActivity[]> => {
  const response = await client.get(`/contracts/${contractId}/audit-log`);
  return response.data;
};
