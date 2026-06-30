import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listOccupancyPeriods,
  get as getOccupancyPeriod,
  create as createOccupancyPeriod,
  update as updateOccupancyPeriod,
  end as endOccupancyPeriod,
  _delete as deleteOccupancyPeriod,
  getTimeline as getPropertyTimeline,
} from '../generated/api/occupancy-periods/occupancy-periods';
import {
  CreateOccupancyPeriodRequest,
  UpdateOccupancyPeriodRequest,
  EndOccupancyPeriodRequest,
} from '../types/occupancyPeriod';
import { queryKeys } from '../lib/queryKeys';

export const useOccupancyPeriods = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.occupancyPeriods.all(propertyIdentifier),
    queryFn: () => listOccupancyPeriods(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};

export const useOccupancyPeriod = (
  propertyIdentifier: string | undefined,
  periodIdentifier: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.occupancyPeriods.detail(
      propertyIdentifier,
      periodIdentifier
    ),
    queryFn: () =>
      getOccupancyPeriod(propertyIdentifier ?? '', periodIdentifier ?? ''),
    enabled: !!propertyIdentifier && !!periodIdentifier,
  });
};

export const useCreateOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Self-occupancy period created',
    mutationFn: (data: CreateOccupancyPeriodRequest) =>
      createOccupancyPeriod(propertyIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.all(propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyIdentifier),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Self-occupancy period updated',
    mutationFn: ({
      periodIdentifier,
      data,
    }: {
      periodIdentifier: string;
      data: UpdateOccupancyPeriodRequest;
    }) => updateOccupancyPeriod(propertyIdentifier, periodIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.all(propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyIdentifier),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.timeline(propertyIdentifier),
      });
    },
  });
};

export const useEndOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Self-occupancy period ended',
    mutationFn: ({
      periodIdentifier,
      data,
    }: {
      periodIdentifier: string;
      data: EndOccupancyPeriodRequest;
    }) => endOccupancyPeriod(propertyIdentifier, periodIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.all(propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyIdentifier),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.timeline(propertyIdentifier),
      });
    },
  });
};

export const useDeleteOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Self-occupancy period deleted',
    mutationFn: (periodIdentifier: string) =>
      deleteOccupancyPeriod(propertyIdentifier, periodIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.all(propertyIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyIdentifier),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.timeline(propertyIdentifier),
      });
    },
  });
};

export const usePropertyTimeline = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.occupancyPeriods.timeline(propertyIdentifier),
    queryFn: () => getPropertyTimeline(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};
