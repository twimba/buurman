import {
  keepPreviousData,
  useQueries,
  useQueryClient,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listLeaseClauseTemplates,
  createLeaseClauseTemplate,
  updateLeaseClauseTemplate,
  deleteLeaseClauseTemplate,
} from '../generated/api/backoffice-lease-clause-templates/backoffice-lease-clause-templates';
import { combineTemplateQueries } from '../lib/combineTemplateQueries';
import type {
  DocumentLanguage,
  UpsertLeaseClauseTemplateRequest,
} from '../generated/models';

const BASE_KEY = ['lease-clause-templates'];

/**
 * Loads the templates of several countries in parallel (one request each) with titles and
 * bodies resolved in `language`. Mutations invalidate the whole prefix, so every language
 * and country view refreshes.
 */
export const useLeaseClauseTemplates = (
  countryCodes: string[],
  language: DocumentLanguage
) => {
  return useQueries({
    queries: countryCodes.map((countryCode) => ({
      queryKey: [...BASE_KEY, countryCode, language],
      queryFn: () => listLeaseClauseTemplates({ countryCode, language }),
      placeholderData: keepPreviousData,
    })),
    combine: combineTemplateQueries,
  });
};

export const useCreateLeaseClauseTemplate = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpsertLeaseClauseTemplateRequest) =>
      createLeaseClauseTemplate(data),
    errorTitle: "Couldn't create clause template",
    successMessage: 'Clause template created',
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BASE_KEY });
    },
  });
};

export const useUpdateLeaseClauseTemplate = () => {
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
      queryClient.invalidateQueries({ queryKey: BASE_KEY });
    },
  });
};

export const useDeleteLeaseClauseTemplate = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (identifier: string) => deleteLeaseClauseTemplate(identifier),
    errorTitle: "Couldn't delete clause template",
    successMessage: 'Clause template deleted',
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BASE_KEY });
    },
  });
};
