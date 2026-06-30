import {} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import { geocode } from '../generated/api/geocoding/geocoding';
import type { GeocodeRequest } from '../generated/models';

export const useGeocode = () => {
  return useMutationWithToast({
    mutationFn: (data: GeocodeRequest) => geocode(data),
  });
};
