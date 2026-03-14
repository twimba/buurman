import client from './client';
import {
  ExpenseResponse,
  CreateExpenseRequest,
  UpdateExpenseRequest,
  ExpenseSummaryResponse,
  GetExpensesParams,
} from '../types/expense';
import { DocumentResponse, AuditLogEntry } from '../types/property';
import type { ExpenseStatsResponse } from '@/generated/models';
import { PageResponse, PageParams, BulkCreateResult } from '@/types/common';

export const getExpenses = async (
  params?: GetExpensesParams & PageParams
): Promise<PageResponse<ExpenseResponse>> => {
  const response = await client.get('/expenses', { params });
  return response.data;
};

export const getExpenseStats = async (): Promise<ExpenseStatsResponse> => {
  const response = await client.get('/expenses/stats');
  return response.data;
};

export const getExpense = async (id: string): Promise<ExpenseResponse> => {
  const response = await client.get(`/expenses/${id}`);
  return response.data;
};

export const createExpense = async (
  data: CreateExpenseRequest
): Promise<ExpenseResponse> => {
  const response = await client.post('/expenses', data);
  return response.data;
};

export const bulkCreateExpenses = async (
  items: CreateExpenseRequest[]
): Promise<BulkCreateResult<ExpenseResponse>[]> => {
  const response = await client.post('/expenses/bulk', { items });
  return response.data;
};

export const updateExpense = async (
  id: string,
  data: UpdateExpenseRequest
): Promise<ExpenseResponse> => {
  const response = await client.put(`/expenses/${id}`, data);
  return response.data;
};

export const deleteExpense = async (id: string): Promise<void> => {
  await client.delete(`/expenses/${id}`);
};

export const getExpensesByProperty = async (
  propertyIdentifier: string
): Promise<ExpenseResponse[]> => {
  const response = await client.get('/expenses', {
    params: { propertyIdentifier, size: 1000 },
  });
  return response.data.content;
};

export const getExpenseSummary = async (
  period: string
): Promise<ExpenseSummaryResponse> => {
  const response = await client.get('/expenses/summary', {
    params: { period },
  });
  return response.data;
};

export const uploadExpenseDocument = async (
  expenseId: string,
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
    `/expenses/${expenseId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const deleteExpenseDocument = async (
  expenseId: string,
  documentId: string
): Promise<void> => {
  await client.delete(`/expenses/${expenseId}/documents/${documentId}`);
};

export const getExpenseDocuments = async (
  expenseId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/expenses/${expenseId}/documents`);
  return response.data;
};

export const getExpenseAuditLog = async (
  expenseId: string
): Promise<AuditLogEntry[]> => {
  const response = await client.get(`/expenses/${expenseId}/audit-log`);
  return response.data;
};
