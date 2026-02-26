import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as financialsApi from '../api/propertyFinancials';
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
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

// ---------------------------------------------------------------------------
// Query hooks
// ---------------------------------------------------------------------------

export const useFinancialSummary = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyFinancials', propertyId],
    queryFn: () => financialsApi.getFinancialSummary(propertyId!),
    enabled: !!propertyId,
  });
};

export const useAcquisition = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyAcquisition', propertyId],
    queryFn: () => financialsApi.getAcquisition(propertyId!),
    enabled: !!propertyId,
  });
};

export const useValuations = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyValuations', propertyId],
    queryFn: () => financialsApi.getValuations(propertyId!),
    enabled: !!propertyId,
  });
};

export const useLatestValuation = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyValuation', 'latest', propertyId],
    queryFn: () => financialsApi.getLatestValuation(propertyId!),
    enabled: !!propertyId,
  });
};

export const useFinancings = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyFinancings', propertyId],
    queryFn: () => financialsApi.getFinancings(propertyId!),
    enabled: !!propertyId,
  });
};

export const useFinancingPayments = (
  propertyId: string | undefined,
  financingId: string | undefined
) => {
  return useQuery({
    queryKey: ['financingPayments', propertyId, financingId],
    queryFn: () =>
      financialsApi.getFinancingPayments(propertyId!, financingId!),
    enabled: !!propertyId && !!financingId,
  });
};

export const useInsurances = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyInsurances', propertyId],
    queryFn: () => financialsApi.getInsurances(propertyId!),
    enabled: !!propertyId,
  });
};

export const useTaxes = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyTaxes', propertyId],
    queryFn: () => financialsApi.getTaxes(propertyId!),
    enabled: !!propertyId,
  });
};

export const useFees = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyFees', propertyId],
    queryFn: () => financialsApi.getFees(propertyId!),
    enabled: !!propertyId,
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Acquisition
// ---------------------------------------------------------------------------

export const useUpsertAcquisition = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpsertAcquisitionRequest) =>
      financialsApi.upsertAcquisition(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyAcquisition', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Acquisition saved successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Valuations
// ---------------------------------------------------------------------------

export const useCreateValuation = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateValuationRequest) =>
      financialsApi.createValuation(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyValuations', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyValuation', 'latest', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Valuation created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateValuation = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      valuationId,
      data,
    }: {
      valuationId: string;
      data: UpdateValuationRequest;
    }) => financialsApi.updateValuation(propertyId, valuationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyValuations', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyValuation', 'latest', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Valuation updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteValuation = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (valuationId: string) =>
      financialsApi.deleteValuation(propertyId, valuationId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyValuations', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyValuation', 'latest', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Valuation deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Financings
// ---------------------------------------------------------------------------

export const useCreateFinancing = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateFinancingRequest) =>
      financialsApi.createFinancing(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancings', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Financing created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateFinancing = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      financingId,
      data,
    }: {
      financingId: string;
      data: UpdateFinancingRequest;
    }) => financialsApi.updateFinancing(propertyId, financingId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancings', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Financing updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteFinancing = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (financingId: string) =>
      financialsApi.deleteFinancing(propertyId, financingId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancings', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Financing deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Financing Payments
// ---------------------------------------------------------------------------

export const useCreateFinancingPayment = (
  propertyId: string,
  financingId: string
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateFinancingPaymentRequest) =>
      financialsApi.createFinancingPayment(propertyId, financingId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['financingPayments', propertyId, financingId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Payment created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateFinancingPayment = (
  propertyId: string,
  financingId: string
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      paymentId,
      data,
    }: {
      paymentId: string;
      data: UpdateFinancingPaymentRequest;
    }) =>
      financialsApi.updateFinancingPayment(
        propertyId,
        financingId,
        paymentId,
        data
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['financingPayments', propertyId, financingId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Payment updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteFinancingPayment = (
  propertyId: string,
  financingId: string
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (paymentId: string) =>
      financialsApi.deleteFinancingPayment(propertyId, financingId, paymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['financingPayments', propertyId, financingId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Payment deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
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
    queryKey: ['financingPaymentDocuments', propertyId, financingId, paymentId],
    queryFn: () =>
      financialsApi.getFinancingPaymentDocuments(
        propertyId!,
        financingId!,
        paymentId!
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
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) =>
      financialsApi.uploadFinancingPaymentDocument(
        propertyId,
        financingId,
        paymentId,
        file,
        title,
        notes
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: [
          'financingPaymentDocuments',
          propertyId,
          financingId,
          paymentId,
        ],
      });
      showToast('Document uploaded successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteFinancingPaymentDocument = (
  propertyId: string,
  financingId: string,
  paymentId: string
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      financialsApi.deleteFinancingPaymentDocument(
        propertyId,
        financingId,
        documentId
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: [
          'financingPaymentDocuments',
          propertyId,
          financingId,
          paymentId,
        ],
      });
      showToast('Document deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Insurances
// ---------------------------------------------------------------------------

export const useCreateInsurance = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateInsuranceRequest) =>
      financialsApi.createInsurance(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyInsurances', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Insurance created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateInsurance = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      insuranceId,
      data,
    }: {
      insuranceId: string;
      data: UpdateInsuranceRequest;
    }) => financialsApi.updateInsurance(propertyId, insuranceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyInsurances', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Insurance updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteInsurance = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (insuranceId: string) =>
      financialsApi.deleteInsurance(propertyId, insuranceId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyInsurances', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Insurance deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Taxes
// ---------------------------------------------------------------------------

export const useCreateTax = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateTaxRequest) =>
      financialsApi.createTax(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyTaxes', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Tax record created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateTax = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({ taxId, data }: { taxId: string; data: UpdateTaxRequest }) =>
      financialsApi.updateTax(propertyId, taxId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyTaxes', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Tax record updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteTax = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (taxId: string) => financialsApi.deleteTax(propertyId, taxId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyTaxes', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Tax record deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// ---------------------------------------------------------------------------
// Mutation hooks — Fees
// ---------------------------------------------------------------------------

export const useCreateFee = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateFeeRequest) =>
      financialsApi.createFee(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyFees', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Fee created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateFee = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({ feeId, data }: { feeId: string; data: UpdateFeeRequest }) =>
      financialsApi.updateFee(propertyId, feeId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyFees', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Fee updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteFee = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (feeId: string) => financialsApi.deleteFee(propertyId, feeId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyFees', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyFinancials', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Fee deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
