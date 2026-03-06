import client from './client';
import type {
  WwsCalculationRequest,
  WwsCalculationResponse,
  WwsPreFillResponse,
} from '../types/wws';

export const calculateWws = async (
  data: WwsCalculationRequest
): Promise<WwsCalculationResponse> => {
  const response = await client.post('/wws/calculate', data);
  return response.data;
};

export const calculateAndSaveWws = async (
  data: WwsCalculationRequest
): Promise<WwsCalculationResponse> => {
  const response = await client.post('/wws/calculate-and-save', data);
  return response.data;
};

export const getWwsPreFill = async (
  propertyIdentifier: string
): Promise<WwsPreFillResponse> => {
  const response = await client.get(
    `/wws/properties/${propertyIdentifier}/pre-fill`
  );
  return response.data;
};

export const getWwsCalculations = async (
  propertyIdentifier: string
): Promise<WwsCalculationResponse[]> => {
  const response = await client.get(
    `/wws/properties/${propertyIdentifier}/calculations`
  );
  return response.data;
};

export const getLatestWwsCalculation = async (
  propertyIdentifier: string
): Promise<WwsCalculationResponse> => {
  const response = await client.get(
    `/wws/properties/${propertyIdentifier}/latest`
  );
  return response.data;
};
