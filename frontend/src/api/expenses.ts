import client from './client';
import {
  ExpenseResponse,
  CreateExpenseRequest,
  UpdateExpenseRequest,
  ExpenseSummaryResponse,
  GetExpensesParams,
} from '../types/expense';

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
