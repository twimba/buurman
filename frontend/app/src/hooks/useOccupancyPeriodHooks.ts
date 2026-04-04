import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as occupancyApi from '../api/occupancyPeriods';
import {
  CreateOccupancyPeriodRequest,
  UpdateOccupancyPeriodRequest,
  EndOccupancyPeriodRequest,
} from '../types/occupancyPeriod';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useOccupancyPeriods = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.occupancyPeriods.all(propertyIdentifier),
    queryFn: () => occupancyApi.getOccupancyPeriods(propertyIdentifier ?? ''),
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
      occupancyApi.getOccupancyPeriod(
        propertyIdentifier ?? '',
        periodIdentifier ?? ''
      ),
    enabled: !!propertyIdentifier && !!periodIdentifier,
  });
};

export const useCreateOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateOccupancyPeriodRequest) =>
      occupancyApi.createOccupancyPeriod(propertyIdentifier, data),
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
      showToast('Self-occupancy period created', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      periodIdentifier,
      data,
    }: {
      periodIdentifier: string;
      data: UpdateOccupancyPeriodRequest;
    }) =>
      occupancyApi.updateOccupancyPeriod(
        propertyIdentifier,
        periodIdentifier,
        data
      ),
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
      showToast('Self-occupancy period updated', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useEndOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      periodIdentifier,
      data,
    }: {
      periodIdentifier: string;
      data: EndOccupancyPeriodRequest;
    }) =>
      occupancyApi.endOccupancyPeriod(
        propertyIdentifier,
        periodIdentifier,
        data
      ),
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
      showToast('Self-occupancy period ended', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteOccupancyPeriod = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (periodIdentifier: string) =>
      occupancyApi.deleteOccupancyPeriod(propertyIdentifier, periodIdentifier),
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
      showToast('Self-occupancy period deleted', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePropertyTimeline = (propertyIdentifier: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.occupancyPeriods.timeline(propertyIdentifier),
    queryFn: () => occupancyApi.getPropertyTimeline(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });
};
