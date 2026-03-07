import client from './client';
import type {
  RentRegulationCountryResponse,
  RentRegulationCountryDetailResponse,
  RentRegulationRuleResponse,
} from '../types/rentRegulation';

export const getCountries = async (): Promise<
  RentRegulationCountryResponse[]
> => {
  const response = await client.get('/rent-regulations/countries');
  return response.data;
};

export const getCountryDetail = async (
  code: string
): Promise<RentRegulationCountryDetailResponse> => {
  const response = await client.get(`/rent-regulations/countries/${code}`);
  return response.data;
};

export const getCurrentRules = async (
  code: string
): Promise<RentRegulationRuleResponse[]> => {
  const response = await client.get(
    `/rent-regulations/countries/${code}/current`
  );
  return response.data;
};

export const getRulesByYear = async (
  code: string,
  year: number
): Promise<RentRegulationRuleResponse[]> => {
  const response = await client.get(
    `/rent-regulations/countries/${code}/rules`,
    { params: { year } }
  );
  return response.data;
};

export const getRegionCurrentRules = async (
  code: string,
  regionCode: string
): Promise<RentRegulationRuleResponse[]> => {
  const response = await client.get(
    `/rent-regulations/countries/${code}/regions/${regionCode}/current`
  );
  return response.data;
};
