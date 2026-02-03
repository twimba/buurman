import client from './client';
import {
  ExpenseResponse,
  CreateExpenseRequest,
  UpdateExpenseRequest,
  ExpenseSummaryResponse,
  GetExpensesParams,
} from '../types/expense';
import { DocumentResponse, AuditLogEntry } from '../types/property';

export const getExpenses = async (
  params?: GetExpensesParams
): Promise<ExpenseResponse[]> => {
  const response = await client.get('/expenses', { params });
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
  propertyId: string
): Promise<ExpenseResponse[]> => {
  const response = await client.get('/expenses', {
    params: { propertyId },
  });
  return response.data;
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
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

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
