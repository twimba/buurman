import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { loggersApi } from '../api/loggers';

export const useLoggers = () => {
  return useQuery({
    queryKey: ['loggers'],
    queryFn: () => loggersApi.list().then((res) => res.data),
  });
};

export const useSetLogLevel = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      loggerName,
      level,
    }: {
      loggerName: string;
      level: string | null;
    }) => loggersApi.setLevel(loggerName, level),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loggers'] });
    },
  });
};

export const useResetAllLogLevels = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => loggersApi.resetAll(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loggers'] });
    },
  });
};
