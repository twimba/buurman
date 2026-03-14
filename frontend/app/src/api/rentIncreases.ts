import client from './client';
import type {
  RentIncreasePreviewResponse,
  ApplyRentIncreasesRequest,
  ApplyRentIncreasesResponse,
} from '../types/rentIncrease';

export const previewRentIncreases = async (
  year: number
): Promise<RentIncreasePreviewResponse> => {
  const response = await client.post('/rent-increases/preview', { year });
  return response.data;
};

export const applyRentIncreases = async (
  data: ApplyRentIncreasesRequest
): Promise<ApplyRentIncreasesResponse> => {
  const response = await client.post('/rent-increases/apply', data);
  return response.data;
};
