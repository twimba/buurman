import client from './client';
import type { ScheduledJob, JobExecutionHistory, PageResponse } from '../types';

interface HistoryParams {
  jobName?: string[];
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}

export const schedulerApi = {
  listJobs: () => client.get<ScheduledJob[]>('/scheduler/jobs'),
  pauseJob: (jobName: string, group: string) =>
    client.post(`/scheduler/jobs/${jobName}/pause`, null, {
      params: { group },
    }),
  resumeJob: (jobName: string, group: string) =>
    client.post(`/scheduler/jobs/${jobName}/resume`, null, {
      params: { group },
    }),
  triggerJob: (jobName: string, group: string) =>
    client.post(`/scheduler/jobs/${jobName}/trigger`, null, {
      params: { group },
    }),
  rescheduleJob: (jobName: string, group: string, cronExpression: string) =>
    client.post(
      `/scheduler/jobs/${jobName}/reschedule`,
      { cronExpression },
      { params: { group } }
    ),
  history: (params?: HistoryParams) =>
    client.get<PageResponse<JobExecutionHistory>>('/scheduler/history', {
      params,
      paramsSerializer: {
        indexes: null, // Serialize arrays as jobName=a&jobName=b (Spring format)
      },
    }),
};
