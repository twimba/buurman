import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getContracts,
  getContract,
  createContract,
  updateContract,
  deleteContract,
  changeContractStatus,
  reopenContract,
  duplicateContract,
  getContractDocuments,
  uploadContractDocument,
  deleteContractDocument,
  generatePayments,
  getMetadataSchema,
  addParty,
  removeParty,
  changePrimaryContact,
  getContractDeposits,
  upsertContractDeposit,
  addDepositDeduction,
  removeDepositDeduction,
  returnContractDeposit,
  forfeitContractDeposit,
  getContractPaymentPlans,
  createContractPaymentPlan,
  cancelContractPaymentPlan,
  getContractTimeline,
} from '../generated/api/contracts/contracts';
import {
  CreateContractRequest,
  UpdateContractRequest,
  ChangeContractStatusRequest,
  AddContractPartyRequest,
  ChangePrimaryContactRequest,
  CountryMetadataSchema,
  ContractResponse,
  UpsertDepositRequest,
  CreateDepositDeductionRequest,
  ReturnDepositRequest,
  ForfeitDepositRequest,
  CreatePaymentPlanRequest,
  CancelPaymentPlanRequest,
} from '../types/contract';
import type { PageResponse, PageParams } from '@/types/common';
import type { GetContractsParams } from '../generated/models';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { toRentComponentRequests } from '../utils/rentComponents';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

// Param shape is widened to PageParams (sort as a plain string) for caller convenience;
// cast to the generated params at the call site.
export const useContracts = (
  params?: Omit<GetContractsParams, 'direction'> & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.contracts.all(params),
    queryFn: () =>
      getContracts(params as GetContractsParams) as Promise<
        PageResponse<ContractResponse>
      >,
    placeholderData: keepPreviousData,
  });
};

export const useContract = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.detail(id),
    queryFn: () => getContract(id ?? '') as Promise<ContractResponse>,
    enabled: !!id,
  });
};

export const useCreateContract = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreateContractRequest) =>
      createContract({
        ...data,
        rentComponents: toRentComponentRequests(data.rentComponents),
      }),
    onSuccess: (newContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(newContract.property?.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          newContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contract created successfully', 'success');
      trackEvent(AnalyticsEvent.CONTRACT_CREATED);
    },
  });
};

export const useUpdateContract = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Contract updated successfully',
    mutationFn: (data: UpdateContractRequest) =>
      updateContract(id, {
        ...data,
        rentComponents: toRentComponentRequests(data.rentComponents),
      }),
    onSuccess: (updatedContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedContract.property?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          updatedContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      // Contract changes may affect payment display
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(id),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
    },
  });
};

export const useDeleteContract = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Contract deleted successfully',
    mutationFn: (id: string) => deleteContract(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useChangeContractStatus = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: ChangeContractStatusRequest) =>
      changeContractStatus(id, data),
    onSuccess: (updatedContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedContract.property?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          updatedContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(id),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      trackEvent(AnalyticsEvent.CONTRACT_STATUS_CHANGED);
    },
  });
};

export const useReopenContract = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => reopenContract(id),
    onSuccess: (updatedContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedContract.property?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          updatedContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      // Reopening cancels future payments
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(id),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      showToast('Contract reopened successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDuplicateContract = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Contract duplicated successfully',
    mutationFn: (id: string) => duplicateContract(id),
    onSuccess: (newContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(newContract.property?.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          newContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useContractDocuments = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.documents(contractId),
    queryFn: () => getContractDocuments(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useUploadContractDocument = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => uploadContractDocument(contractId, { file }, { title, notes }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
  });
};

export const useDeleteContractDocument = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (documentId: string) => deleteContractDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
  });
};

export const useGenerateContractPayments = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: ({
      count,
      markAsPaid,
    }: {
      count: number;
      markAsPaid?: boolean;
    }) =>
      generatePayments(contractId, { count, markAsPaid }) as Promise<{
        generated: number;
        requested: number;
        markedAsPaid?: number;
      }>,
    onSuccess: (result) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      const paidSuffix =
        result.markedAsPaid && result.markedAsPaid > 0
          ? ' and marked as paid'
          : '';
      trackEvent(AnalyticsEvent.PAYMENTS_GENERATED, {
        count: result.generated,
      });
      if (result.generated === result.requested) {
        showToast(
          `Scheduled ${result.generated} payment(s)${paidSuffix} successfully`,
          'success'
        );
      } else {
        showToast(
          `Scheduled ${result.generated} of ${result.requested} payment(s)${paidSuffix}. Some dates already had payments.`,
          'success'
        );
      }
    },
  });
};

// --- Country Metadata Schema hook ---

export const useContractMetadataSchema = (countryCode?: string) => {
  return useQuery({
    queryKey: queryKeys.contracts.metadataSchema(countryCode),
    queryFn: () =>
      getMetadataSchema(countryCode ?? '') as Promise<CountryMetadataSchema>,
    enabled: !!countryCode,
    staleTime: Infinity, // Schemas don't change during a session
  });
};

// --- Contract Party hooks ---

export const useAddContractParty = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Party added successfully',
    mutationFn: (data: AddContractPartyRequest) => addParty(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
    },
  });
};

export const useRemoveContractParty = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Party removed successfully',
    mutationFn: (partyIdentifier: string) =>
      removeParty(contractId, partyIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
    },
  });
};

export const useChangePrimaryContact = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Primary contact changed successfully',
    mutationFn: (data: ChangePrimaryContactRequest) =>
      changePrimaryContact(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
    },
  });
};

// --- Deposits (BUUR-101) ---

export const useContractDeposit = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.deposit(contractId),
    queryFn: async () =>
      (await getContractDeposits(contractId ?? ''))[0] ?? null,
    enabled: !!contractId,
  });
};

const useDepositMutation = <TVars>(
  contractId: string,
  fn: (vars: TVars) => Promise<unknown>,
  successMessage: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: fn,
    successMessage,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.deposit(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
  });
};

export const useUpsertDeposit = (contractId: string, successMessage: string) =>
  useDepositMutation(
    contractId,
    (data: UpsertDepositRequest) => upsertContractDeposit(contractId, data),
    successMessage
  );

export const useAddDepositDeduction = (
  contractId: string,
  successMessage: string
) =>
  useDepositMutation(
    contractId,
    (data: CreateDepositDeductionRequest) =>
      addDepositDeduction(contractId, data),
    successMessage
  );

export const useRemoveDepositDeduction = (
  contractId: string,
  successMessage: string
) =>
  useDepositMutation(
    contractId,
    (deductionId: string) => removeDepositDeduction(contractId, deductionId),
    successMessage
  );

export const useReturnDeposit = (contractId: string, successMessage: string) =>
  useDepositMutation(
    contractId,
    (data: ReturnDepositRequest) => returnContractDeposit(contractId, data),
    successMessage
  );

export const useForfeitDeposit = (contractId: string, successMessage: string) =>
  useDepositMutation(
    contractId,
    (data: ForfeitDepositRequest) => forfeitContractDeposit(contractId, data),
    successMessage
  );

// --- Payment plans (BUUR-101) ---

export const useContractPaymentPlans = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.paymentPlans(contractId),
    queryFn: () => getContractPaymentPlans(contractId ?? ''),
    enabled: !!contractId,
  });
};

const invalidateAfterPlanChange = (
  queryClient: ReturnType<typeof useQueryClient>,
  contractId: string
) => {
  queryClient.invalidateQueries({
    queryKey: queryKeys.contracts.paymentPlans(contractId),
  });
  queryClient.invalidateQueries({
    queryKey: queryKeys.contracts.detail(contractId),
  });
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.arrears() });
  queryClient.invalidateQueries({
    queryKey: queryKeys.contracts.paymentsByContract(contractId),
  });
};

export const useCreatePaymentPlan = (
  contractId: string,
  successMessage: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreatePaymentPlanRequest) =>
      createContractPaymentPlan(contractId, data),
    successMessage,
    onSuccess: () => {
      invalidateAfterPlanChange(queryClient, contractId);
      trackEvent(AnalyticsEvent.PAYMENT_PLAN_CREATED);
    },
  });
};

export const useCancelPaymentPlan = (
  contractId: string,
  successMessage: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      planId,
      data,
    }: {
      planId: string;
      data: CancelPaymentPlanRequest;
    }) => cancelContractPaymentPlan(contractId, planId, data),
    successMessage,
    onSuccess: () => {
      invalidateAfterPlanChange(queryClient, contractId);
      trackEvent(AnalyticsEvent.PAYMENT_PLAN_CANCELLED);
    },
  });
};

export const useContractTimeline = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.timeline(contractId),
    queryFn: () => getContractTimeline(contractId ?? ''),
    enabled: !!contractId,
  });
};
