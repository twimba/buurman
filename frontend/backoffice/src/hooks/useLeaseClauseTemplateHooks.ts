import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listLeaseClauseTemplates,
  createLeaseClauseTemplate,
  updateLeaseClauseTemplate,
  deleteLeaseClauseTemplate,
} from '../generated/api/backoffice-lease-clause-templates/backoffice-lease-clause-templates';
import type { UpsertLeaseClauseTemplateRequest } from '../generated/models';

const queryKey = (countryCode: string) => [
  'lease-clause-templates',
  countryCode,
];

export const useLeaseClauseTemplates = (countryCode: string) => {
  return useQuery({
    queryKey: queryKey(countryCode),
    queryFn: () => listLeaseClauseTemplates({ countryCode }),
    enabled: !!countryCode,
  });
};

export const useCreateLeaseClauseTemplate = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpsertLeaseClauseTemplateRequest) =>
      createLeaseClauseTemplate(data),
    errorTitle: "Couldn't create clause template",
    successMessage: 'Clause template created',
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKey(countryCode) });
    },
  });
};

export const useUpdateLeaseClauseTemplate = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpsertLeaseClauseTemplateRequest;
    }) => updateLeaseClauseTemplate(identifier, data),
    errorTitle: "Couldn't update clause template",
    successMessage: 'Clause template updated',
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKey(countryCode) });
    },
  });
};

export const useDeleteLeaseClauseTemplate = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (identifier: string) => deleteLeaseClauseTemplate(identifier),
    errorTitle: "Couldn't delete clause template",
    successMessage: 'Clause template deleted',
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKey(countryCode) });
    },
  });
};
