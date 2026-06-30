import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listLoggers,
  setLogLevel,
  resetAll,
} from '../generated/api/backoffice-loggers/backoffice-loggers';

export const useLoggers = () => {
  return useQuery({
    queryKey: ['loggers'],
    queryFn: () => listLoggers(),
  });
};

export const useSetLogLevel = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      loggerName,
      level,
    }: {
      loggerName: string;
      level: string | null;
      // A null level resets the logger to its inherited level; the backend
      // expects an explicit null, so send it through as-is.
    }) => setLogLevel(loggerName, { level } as { level?: string }),
    errorTitle: "Couldn't set log level",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loggers'] });
    },
  });
};

export const useResetAllLogLevels = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: () => resetAll(),
    errorTitle: "Couldn't reset log levels",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loggers'] });
    },
  });
};
