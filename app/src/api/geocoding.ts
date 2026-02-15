import client from './client';

export interface GeocodeRequest {
  street: string;
  city: string;
  postalCode: string;
  country: string;
}

export interface GeocodeResponse {
  latitude: number;
  longitude: number;
}

export const geocodeAddress = async (
  data: GeocodeRequest
): Promise<GeocodeResponse | null> => {
  const response = await client.post('/geocode', data, {
    validateStatus: (status) => status === 200 || status === 204,
  });
  return response.status === 200 ? response.data : null;
};
