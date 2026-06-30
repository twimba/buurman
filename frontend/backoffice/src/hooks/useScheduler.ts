import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listJobs,
  pauseJob,
  resumeJob,
  triggerJob,
  rescheduleJob,
  getHistory,
} from '../generated/api/backoffice-scheduler/backoffice-scheduler';
import type { GetHistoryParams } from '../generated/models';

export const useScheduledJobs = () => {
  return useQuery({
    queryKey: ['scheduler-jobs'],
    queryFn: () => listJobs(),
    refetchInterval: 30000,
  });
};

export const usePauseJob = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({ jobName, group }: { jobName: string; group: string }) =>
      pauseJob(jobName, { group }),
    errorTitle: "Couldn't pause job",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduler-jobs'] });
    },
  });
};

export const useResumeJob = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({ jobName, group }: { jobName: string; group: string }) =>
      resumeJob(jobName, { group }),
    errorTitle: "Couldn't resume job",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduler-jobs'] });
    },
  });
};

export const useTriggerJob = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({ jobName, group }: { jobName: string; group: string }) =>
      triggerJob(jobName, { group }),
    errorTitle: "Couldn't trigger job",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduler-jobs'] });
      queryClient.invalidateQueries({ queryKey: ['scheduler-history'] });
    },
  });
};

export const useRescheduleJob = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      jobName,
      group,
      cronExpression,
    }: {
      jobName: string;
      group: string;
      cronExpression: string;
    }) => rescheduleJob(jobName, { cronExpression }, { group }),
    errorTitle: "Couldn't reschedule job",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduler-jobs'] });
    },
  });
};

interface HistoryParams {
  jobName?: string[];
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}

export const useJobExecutionHistory = (params?: HistoryParams) => {
  return useQuery({
    queryKey: ['scheduler-history', params],
    queryFn: () => getHistory(params as GetHistoryParams),
  });
};
