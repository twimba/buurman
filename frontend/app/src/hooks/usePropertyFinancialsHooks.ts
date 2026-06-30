import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import { getFinancialSummary } from '../generated/api/property-financials/property-financials';
import {
  getAcquisition,
  upsertAcquisition,
} from '../generated/api/property-acquisitions/property-acquisitions';
import {
  listValuations as getValuations,
  getLatestValuation,
  createValuation,
  updateValuation,
  deleteValuation,
} from '../generated/api/property-valuations/property-valuations';
import {
  listFinancings as getFinancings,
  createFinancing,
  updateFinancing,
  deleteFinancing,
  listPayments as getFinancingPayments,
  createFinancingPayment,
  updateFinancingPayment,
  deleteFinancingPayment,
  getFinancingPaymentDocuments,
  uploadFinancingPaymentDocument,
  deleteFinancingPaymentDocument,
} from '../generated/api/property-financings/property-financings';
import {
  listInsurances as getInsurances,
  createInsurance,
  updateInsurance,
  deleteInsurance,
} from '../generated/api/property-insurances/property-insurances';
import {
  listTaxes as getTaxes,
  createTax,
  updateTax,
  deleteTax,
} from '../generated/api/property-taxes/property-taxes';
import {
  listFees as getFees,
  createFee,
  updateFee,
  deleteFee,
} from '../generated/api/property-fees/property-fees';
import {
  UpsertAcquisitionRequest,
  CreateValuationRequest,
  UpdateValuationRequest,
  CreateFinancingRequest,
  UpdateFinancingRequest,
  CreateFinancingPaymentRequest,
  UpdateFinancingPaymentRequest,
  CreateInsuranceRequest,
  UpdateInsuranceRequest,
  CreateTaxRequest,
  UpdateTaxRequest,
  CreateFeeRequest,
  UpdateFeeRequest,
} from '../types/propertyFinancials';
import { queryKeys } from '../lib/queryKeys';

// ---------------------------------------------------------------------------
// Query hooks
// ---------------------------------------------------------------------------

export const useFinancialSummary = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.summary(propertyId),
    queryFn: () => getFinancialSummary(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useAcquisition = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.acquisition(propertyId),
    queryFn: () => getAcquisition(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useValuations = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.valuations(propertyId),
    queryFn: () => getValuations(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useLatestValuation = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.latestValuation(propertyId),
    queryFn: () => getLatestValuation(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useFinancings = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.financings(propertyId),
    queryFn: () => getFinancings(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useFinancingPayments = (
  propertyId: string | undefined,
  financingId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.financingPayments(
      propertyId,
      financingId
    ),
    queryFn: () => getFinancingPayments(propertyId ?? '', financingId ?? ''),
    enabled: !!propertyId && !!financingId,
  });
};

export const useInsurances = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.insurances(propertyId),
    queryFn: () => getInsurances(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useTaxes = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.taxes(propertyId),
    queryFn: () => getTaxes(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useFees = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.fees(propertyId),
    queryFn: () => getFees(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Acquisition
// ---------------------------------------------------------------------------

export const useUpsertAcquisition = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Acquisition saved successfully',
    mutationFn: (data: UpsertAcquisitionRequest) =>
      upsertAcquisition(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.acquisition(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Valuations
// ---------------------------------------------------------------------------

export const useCreateValuation = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Valuation created successfully',
    mutationFn: (data: CreateValuationRequest) =>
      createValuation(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.valuations(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.latestValuation(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateValuation = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Valuation updated successfully',
    mutationFn: ({
      valuationId,
      data,
    }: {
      valuationId: string;
      data: UpdateValuationRequest;
    }) => updateValuation(propertyId, valuationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.valuations(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.latestValuation(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteValuation = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Valuation deleted successfully',
    mutationFn: (valuationId: string) =>
      deleteValuation(propertyId, valuationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.valuations(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.latestValuation(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Financings
// ---------------------------------------------------------------------------

export const useCreateFinancing = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Financing created successfully',
    mutationFn: (data: CreateFinancingRequest) =>
      createFinancing(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financings(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.timeline(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateFinancing = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Financing updated successfully',
    mutationFn: ({
      financingId,
      data,
    }: {
      financingId: string;
      data: UpdateFinancingRequest;
    }) => updateFinancing(propertyId, financingId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financings(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.timeline(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteFinancing = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Financing deleted successfully',
    mutationFn: (financingId: string) =>
      deleteFinancing(propertyId, financingId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financings(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.occupancyPeriods.timeline(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Financing Payments
// ---------------------------------------------------------------------------

export const useCreateFinancingPayment = (
  propertyId: string,
  financingId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment created successfully',
    mutationFn: (data: CreateFinancingPaymentRequest) =>
      createFinancingPayment(propertyId, financingId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financingPayments(
          propertyId,
          financingId
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financings(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateFinancingPayment = (
  propertyId: string,
  financingId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment updated successfully',
    mutationFn: ({
      paymentId,
      data,
    }: {
      paymentId: string;
      data: UpdateFinancingPaymentRequest;
    }) => updateFinancingPayment(propertyId, financingId, paymentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financingPayments(
          propertyId,
          financingId
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financings(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteFinancingPayment = (
  propertyId: string,
  financingId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment deleted successfully',
    mutationFn: (paymentId: string) =>
      deleteFinancingPayment(propertyId, financingId, paymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financingPayments(
          propertyId,
          financingId
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financings(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Financing Payment Documents
// ---------------------------------------------------------------------------

export const useFinancingPaymentDocuments = (
  propertyId: string | undefined,
  financingId: string | undefined,
  paymentId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.propertyFinancials.financingPaymentDocuments(
      propertyId,
      financingId,
      paymentId
    ),
    queryFn: () =>
      getFinancingPaymentDocuments(
        propertyId ?? '',
        financingId ?? '',
        paymentId ?? ''
      ),
    enabled: !!propertyId && !!financingId && !!paymentId,
  });
};

export const useUploadFinancingPaymentDocument = (
  propertyId: string,
  financingId: string,
  paymentId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document uploaded successfully',
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) =>
      uploadFinancingPaymentDocument(
        propertyId,
        financingId,
        paymentId,
        { file },
        { title, notes }
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financingPaymentDocuments(
          propertyId,
          financingId,
          paymentId
        ),
      });
    },
  });
};

export const useDeleteFinancingPaymentDocument = (
  propertyId: string,
  financingId: string,
  paymentId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document deleted successfully',
    mutationFn: (documentId: string) =>
      deleteFinancingPaymentDocument(propertyId, financingId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.financingPaymentDocuments(
          propertyId,
          financingId,
          paymentId
        ),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Insurances
// ---------------------------------------------------------------------------

export const useCreateInsurance = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Insurance created successfully',
    mutationFn: (data: CreateInsuranceRequest) =>
      createInsurance(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.insurances(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateInsurance = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Insurance updated successfully',
    mutationFn: ({
      insuranceId,
      data,
    }: {
      insuranceId: string;
      data: UpdateInsuranceRequest;
    }) => updateInsurance(propertyId, insuranceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.insurances(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteInsurance = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Insurance deleted successfully',
    mutationFn: (insuranceId: string) =>
      deleteInsurance(propertyId, insuranceId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.insurances(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Taxes
// ---------------------------------------------------------------------------

export const useCreateTax = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Tax record created successfully',
    mutationFn: (data: CreateTaxRequest) => createTax(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.taxes(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateTax = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Tax record updated successfully',
    mutationFn: ({ taxId, data }: { taxId: string; data: UpdateTaxRequest }) =>
      updateTax(propertyId, taxId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.taxes(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteTax = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Tax record deleted successfully',
    mutationFn: (taxId: string) => deleteTax(propertyId, taxId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.taxes(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks -- Fees
// ---------------------------------------------------------------------------

export const useCreateFee = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Fee created successfully',
    mutationFn: (data: CreateFeeRequest) => createFee(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.fees(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateFee = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Fee updated successfully',
    mutationFn: ({ feeId, data }: { feeId: string; data: UpdateFeeRequest }) =>
      updateFee(propertyId, feeId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.fees(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteFee = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Fee deleted successfully',
    mutationFn: (feeId: string) => deleteFee(propertyId, feeId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.fees(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.propertyFinancials.summary(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};
