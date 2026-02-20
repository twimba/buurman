import client from './client';

export interface CurrencyInfo {
  code: string;
  name: string;
  symbol: string;
  fractionalDigits: number;
}

export const getCurrencies = async (): Promise<CurrencyInfo[]> => {
  const response = await client.get('/reference/currencies');
  return response.data;
};
