import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { schedulerApi } from "../api/scheduler";

export const useScheduledJobs = () => {
  return useQuery({
    queryKey: ["scheduler-jobs"],
    queryFn: () => schedulerApi.listJobs().then((res) => res.data),
    refetchInterval: 30000,
  });
};

export const usePauseJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ jobName, group }: { jobName: string; group: string }) =>
      schedulerApi.pauseJob(jobName, group),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["scheduler-jobs"] });
    },
  });
};

export const useResumeJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ jobName, group }: { jobName: string; group: string }) =>
      schedulerApi.resumeJob(jobName, group),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["scheduler-jobs"] });
    },
  });
};

export const useTriggerJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ jobName, group }: { jobName: string; group: string }) =>
      schedulerApi.triggerJob(jobName, group),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["scheduler-jobs"] });
      queryClient.invalidateQueries({ queryKey: ["scheduler-history"] });
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
    queryKey: ["scheduler-history", params],
    queryFn: () => schedulerApi.history(params).then((res) => res.data),
  });
};
