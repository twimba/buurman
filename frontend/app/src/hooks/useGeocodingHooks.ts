import { useMutation } from '@tanstack/react-query';
import { geocodeAddress, GeocodeRequest } from '../api/geocoding';

export const useGeocode = () => {
  return useMutation({
    mutationFn: (data: GeocodeRequest) => geocodeAddress(data),
  });
};
