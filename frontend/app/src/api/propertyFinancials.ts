import { BulkCreateResult } from '../types/common';
import { DocumentResponse } from '../types/property';
import client from './client';
import {
  PropertyFinancialSummaryResponse,
  PropertyAcquisitionResponse,
  UpsertPropertyAcquisitionRequest,
  PropertyValuationResponse,
  CreatePropertyValuationRequest,
  UpdatePropertyValuationRequest,
  PropertyFinancingResponse,
  CreatePropertyFinancingRequest,
  UpdatePropertyFinancingRequest,
  FinancingPaymentResponse,
  CreateFinancingPaymentRequest,
  UpdateFinancingPaymentRequest,
  PropertyInsuranceResponse,
  CreatePropertyInsuranceRequest,
  UpdatePropertyInsuranceRequest,
  PropertyTaxResponse,
  CreatePropertyTaxRequest,
  UpdatePropertyTaxRequest,
  PropertyFeeResponse,
  CreatePropertyFeeRequest,
  UpdatePropertyFeeRequest,
} from '../types/propertyFinancials';

// ============================================================
// Financial Summary
// ============================================================

export const getFinancialSummary = async (
  propertyId: string
): Promise<PropertyFinancialSummaryResponse> => {
  const response = await client.get(`/properties/${propertyId}/financials`);
  return response.data;
};

// ============================================================
// Acquisition (1:1 — GET + PUT)
// ============================================================

export const getAcquisition = async (
  propertyId: string
): Promise<PropertyAcquisitionResponse | null> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/acquisition`
  );
  return response.data;
};

export const upsertAcquisition = async (
  propertyId: string,
  data: UpsertPropertyAcquisitionRequest
): Promise<PropertyAcquisitionResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/acquisition`,
    data
  );
  return response.data;
};

// ============================================================
// Valuations (CRUD)
// ============================================================

export const getValuations = async (
  propertyId: string
): Promise<PropertyValuationResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/valuations`
  );
  return response.data;
};

export const getLatestValuation = async (
  propertyId: string
): Promise<PropertyValuationResponse | null> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/valuations/latest`
  );
  return response.data;
};

export const createValuation = async (
  propertyId: string,
  data: CreatePropertyValuationRequest
): Promise<PropertyValuationResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/valuations`,
    data
  );
  return response.data;
};

export const updateValuation = async (
  propertyId: string,
  id: string,
  data: UpdatePropertyValuationRequest
): Promise<PropertyValuationResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/valuations/${id}`,
    data
  );
  return response.data;
};

export const deleteValuation = async (
  propertyId: string,
  id: string
): Promise<void> => {
  await client.delete(`/properties/${propertyId}/financials/valuations/${id}`);
};

// ============================================================
// Financings (CRUD)
// ============================================================

export const getFinancings = async (
  propertyId: string
): Promise<PropertyFinancingResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/financings`
  );
  return response.data;
};

export const getFinancing = async (
  propertyId: string,
  id: string
): Promise<PropertyFinancingResponse> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/financings/${id}`
  );
  return response.data;
};

export const createFinancing = async (
  propertyId: string,
  data: CreatePropertyFinancingRequest
): Promise<PropertyFinancingResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/financings`,
    data
  );
  return response.data;
};

export const updateFinancing = async (
  propertyId: string,
  id: string,
  data: UpdatePropertyFinancingRequest
): Promise<PropertyFinancingResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/financings/${id}`,
    data
  );
  return response.data;
};

export const deleteFinancing = async (
  propertyId: string,
  id: string
): Promise<void> => {
  await client.delete(`/properties/${propertyId}/financials/financings/${id}`);
};

// ============================================================
// Financing Payments (CRUD, nested under financing)
// ============================================================

export const getFinancingPayments = async (
  propertyId: string,
  financingId: string
): Promise<FinancingPaymentResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/financings/${financingId}/payments`
  );
  return response.data;
};

export const createFinancingPayment = async (
  propertyId: string,
  financingId: string,
  data: CreateFinancingPaymentRequest
): Promise<FinancingPaymentResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/financings/${financingId}/payments`,
    data
  );
  return response.data;
};

export const bulkCreateFinancingPayments = async (
  propertyId: string,
  financingId: string,
  items: CreateFinancingPaymentRequest[]
): Promise<BulkCreateResult<FinancingPaymentResponse>[]> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/bulk`,
    { items }
  );
  return response.data;
};

export const updateFinancingPayment = async (
  propertyId: string,
  financingId: string,
  paymentId: string,
  data: UpdateFinancingPaymentRequest
): Promise<FinancingPaymentResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/${paymentId}`,
    data
  );
  return response.data;
};

export const deleteFinancingPayment = async (
  propertyId: string,
  financingId: string,
  paymentId: string
): Promise<void> => {
  await client.delete(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/${paymentId}`
  );
};

// ============================================================
// Financing Payment Documents
// ============================================================

export const getFinancingPaymentDocuments = async (
  propertyId: string,
  financingId: string,
  paymentId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/${paymentId}/documents`
  );
  return response.data;
};

export const uploadFinancingPaymentDocument = async (
  propertyId: string,
  financingId: string,
  paymentId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) {
    formData.append('title', title);
  }
  if (notes) {
    formData.append('notes', notes);
  }
  const response = await client.post(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/${paymentId}/documents`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return response.data;
};

export const deleteFinancingPaymentDocument = async (
  propertyId: string,
  financingId: string,
  documentId: string
): Promise<void> => {
  await client.delete(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/documents/${documentId}`
  );
};

export const getFinancingPaymentDocumentDownloadUrl = async (
  propertyId: string,
  financingId: string,
  documentId: string
): Promise<{ url: string }> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/financings/${financingId}/payments/documents/${documentId}/download`
  );
  return response.data;
};

// ============================================================
// Insurances (CRUD)
// ============================================================

export const getInsurances = async (
  propertyId: string
): Promise<PropertyInsuranceResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/insurances`
  );
  return response.data;
};

export const createInsurance = async (
  propertyId: string,
  data: CreatePropertyInsuranceRequest
): Promise<PropertyInsuranceResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/insurances`,
    data
  );
  return response.data;
};

export const updateInsurance = async (
  propertyId: string,
  id: string,
  data: UpdatePropertyInsuranceRequest
): Promise<PropertyInsuranceResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/insurances/${id}`,
    data
  );
  return response.data;
};

export const deleteInsurance = async (
  propertyId: string,
  id: string
): Promise<void> => {
  await client.delete(`/properties/${propertyId}/financials/insurances/${id}`);
};

// ============================================================
// Taxes (CRUD)
// ============================================================

export const getTaxes = async (
  propertyId: string
): Promise<PropertyTaxResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/taxes`
  );
  return response.data;
};

export const createTax = async (
  propertyId: string,
  data: CreatePropertyTaxRequest
): Promise<PropertyTaxResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/taxes`,
    data
  );
  return response.data;
};

export const updateTax = async (
  propertyId: string,
  id: string,
  data: UpdatePropertyTaxRequest
): Promise<PropertyTaxResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/taxes/${id}`,
    data
  );
  return response.data;
};

export const deleteTax = async (
  propertyId: string,
  id: string
): Promise<void> => {
  await client.delete(`/properties/${propertyId}/financials/taxes/${id}`);
};

// ============================================================
// Fees (CRUD)
// ============================================================

export const getFees = async (
  propertyId: string
): Promise<PropertyFeeResponse[]> => {
  const response = await client.get(
    `/properties/${propertyId}/financials/fees`
  );
  return response.data;
};

export const createFee = async (
  propertyId: string,
  data: CreatePropertyFeeRequest
): Promise<PropertyFeeResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/financials/fees`,
    data
  );
  return response.data;
};

export const updateFee = async (
  propertyId: string,
  id: string,
  data: UpdatePropertyFeeRequest
): Promise<PropertyFeeResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/financials/fees/${id}`,
    data
  );
  return response.data;
};

export const deleteFee = async (
  propertyId: string,
  id: string
): Promise<void> => {
  await client.delete(`/properties/${propertyId}/financials/fees/${id}`);
};
