import client from './client';
import {
  ContractExtensionResponse,
  CreateContractExtensionRequest,
  DeclineContractExtensionRequest,
  UpcomingRenewalResponse,
  JurisdictionDefaultResponse,
} from '../types/contractExtension';
import { PageResponse } from '@/types/common';

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
  extensionId: string
): Promise<ContractExtensionResponse> => {
  const response = await client.post(
    `/contracts/${contractId}/extensions/${extensionId}/cancel`
  );
  return response.data;
};

export const getUpcomingRenewals = async (): Promise<
  UpcomingRenewalResponse[]
> => {
  const response = await client.get('/contracts/upcoming-renewals');
  return response.data;
};

export const getJurisdictionDefaults = async (
  countryCode: string,
  regionCode?: string,
  landlordType?: string,
  furnished?: boolean
): Promise<JurisdictionDefaultResponse> => {
  const response = await client.get(
    `/contracts/extensions/jurisdiction-defaults/${countryCode}`,
    {
      params: { regionCode, landlordType, furnished },
    }
  );
  return response.data;
};
