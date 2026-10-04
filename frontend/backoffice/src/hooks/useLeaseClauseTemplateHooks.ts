import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listLeaseClauseTemplates,
  createLeaseClauseTemplate,
  updateLeaseClauseTemplate,
  deleteLeaseClauseTemplate,
  previewLeaseAgreement,
} from '../generated/api/backoffice-lease-clause-templates/backoffice-lease-clause-templates';
import {
  LEASE_TEMPLATES_KEY_PREFIX,
  leaseTemplatesKey,
  mergeSettled,
} from '../lib/leaseTemplateQuery';
import type {
  DocumentLanguage,
  LeaseAgreementPreviewRequest,
  UpsertLeaseClauseTemplateRequest,
} from '../generated/models';

const EMPTY: never[] = [];
const NO_FAILURES: string[] = [];
const BASE_KEY = [LEASE_TEMPLATES_KEY_PREFIX];

/**
 * Loads the templates of several countries (one request each, settled independently) with
 * titles and bodies resolved in `language`. A single query keyed by countries + language keeps
 * the previous rows while a new key loads (`isPlaceholderData`). Mutations invalidate the
 * whole prefix.
 */
export const useLeaseClauseTemplates = (
  countryCodes: string[],
  language: DocumentLanguage
) => {
  const query = useQuery({
    queryKey: leaseTemplatesKey(countryCodes, language),
    queryFn: async () =>
      mergeSettled(
        countryCodes,
        await Promise.allSettled(
          countryCodes.map((countryCode) =>
            listLeaseClauseTemplates({ countryCode, language })
          )
        )
      ),
    placeholderData: keepPreviousData,
  });
  return {
    data: query.data?.templates ?? EMPTY,
    failedCountries: query.data?.failedCountries ?? NO_FAILURES,
    isLoading: query.data === undefined && query.isLoading,
    isRefreshing: query.isPlaceholderData,
    refetch: () => query.refetch(),
  };
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

/**
 * Renders the sample preview. The previous result stays on screen while a new request loads
 * (`isPlaceholderData`); 4xx errors are not retried automatically, `refetch` retries on demand.
 * The abort signal cancels superseded requests.
 */
export const useLeasePreview = (
  request: LeaseAgreementPreviewRequest | null
) => {
  const query = useQuery({
    queryKey: ['lease-preview', request],
    queryFn: ({ signal }) =>
      previewLeaseAgreement(request as LeaseAgreementPreviewRequest, {
        signal,
      }),
    enabled: request !== null,
    placeholderData: keepPreviousData,
    retry: false,
    staleTime: 30_000,
  });
  return {
    data: query.data,
    error: query.error,
    isFetching: query.isFetching,
    isError: query.isError,
    isPlaceholderData: query.isPlaceholderData,
    refetch: () => query.refetch(),
  };
};
