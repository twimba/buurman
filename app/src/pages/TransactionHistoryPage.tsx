import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Filter,
  TrendingUp,
  TrendingDown,
  Calendar,
  ArrowUpDown,
  Download,
  FileText,
  List,
} from 'lucide-react';
import { usePayments } from '@/hooks/usePaymentHooks';
import { useExpenses } from '@/hooks/useExpenseHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { RefreshButton } from '@/components/ui/RefreshButton';
import { Pagination } from '@/components/ui/Pagination';
import { PaymentStatus } from '@/types/payment';
import client from '@/api/client';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';

type TransactionType = 'ALL' | 'INCOME' | 'EXPENSE';

interface Transaction {
  id: string;
  date: string;
  type: 'INCOME' | 'EXPENSE';
  description: string;
  property: string;
  category?: string;
  amount: number;
  currency: string;
}

export const TransactionHistoryPage = () => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const { isEnabled } = useFeatureFlags();
  const { defaultCurrency } = useTeamDefaults();
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<TransactionType>('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortField, setSortField] = useState<'date' | 'amount'>('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const {
    data: paymentsData,
    isLoading: paymentsLoading,
    isFetching: paymentsFetching,
    refetch: refetchPayments,
  } = usePayments({});
  const {
    data: expensesData,
    isLoading: expensesLoading,
    isFetching: expensesFetching,
    refetch: refetchExpenses,
  } = useExpenses({});
  const payments = paymentsData?.content;
  const expenses = expensesData?.content;

  const isLoading = paymentsLoading || expensesLoading;

  // Combine payments and expenses into transactions
  const allTransactions = useMemo(() => {
    const transactions: Transaction[] = [];

    // Add payments as income
    if (payments) {
      payments
        .filter((p) => p.status === PaymentStatus.PAID && p.paymentDate)
        .forEach((payment) => {
          transactions.push({
            id: payment.identifier,
            date: payment.paymentDate!,
            type: 'INCOME',
            description: `Rent payment - ${payment.property?.street || 'Property'}`,
            property: payment.property
              ? `${payment.property.street}, ${payment.property.city}`
              : 'Unknown',
            amount: payment.amount,
            currency: payment.currency,
          });
        });
    }

    // Add expenses
    if (expenses) {
      expenses.forEach((expense) => {
        transactions.push({
          id: expense.identifier,
          date: expense.expenseDate,
          type: 'EXPENSE',
          description: expense.description || `${expense.category} expense`,
          property: expense.property
            ? `${expense.property.street}, ${expense.property.city}`
            : 'Unknown',
          category: expense.category,
          amount: expense.amount,
          currency: expense.currency,
        });
      });
    }

    return transactions;
  }, [payments, expenses]);

  // Filter and sort transactions
  const filteredAndSortedTransactions = useMemo(() => {
    let filtered = allTransactions;

    // Filter by type
    if (typeFilter !== 'ALL') {
      filtered = filtered.filter((t) => t.type === typeFilter);
    }

    // Filter by search term
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (t) =>
          t.description.toLowerCase().includes(search) ||
          t.property.toLowerCase().includes(search) ||
          t.category?.toLowerCase().includes(search)
      );
    }

    // Filter by date range
    if (startDate) {
      filtered = filtered.filter((t) => t.date >= startDate);
    }
    if (endDate) {
      filtered = filtered.filter((t) => t.date <= endDate);
    }

    // Sort
    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

      if (sortField === 'date') {
        aVal = new Date(a.date).getTime();
        bVal = new Date(b.date).getTime();
      } else {
        aVal = a.amount;
        bVal = b.amount;
      }

      if (sortOrder === 'asc') {
        return aVal > bVal ? 1 : -1;
      } else {
        return aVal < bVal ? 1 : -1;
      }
    });

    return filtered;
  }, [
    allTransactions,
    typeFilter,
    searchTerm,
    startDate,
    endDate,
    sortField,
    sortOrder,
  ]);

  // Pagination
  const paginatedTransactions = useMemo(() => {
    const startIndex = currentPage * pageSize;
    return filteredAndSortedTransactions.slice(
      startIndex,
      startIndex + pageSize
    );
  }, [filteredAndSortedTransactions, currentPage, pageSize]);

  const totalPages = Math.ceil(filteredAndSortedTransactions.length / pageSize);

  // Calculate totals
  const totals = useMemo(() => {
    const income = filteredAndSortedTransactions
      .filter((t) => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0);
    const expenses = filteredAndSortedTransactions
      .filter((t) => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0);
    return { income, expenses, net: income - expenses };
  }, [filteredAndSortedTransactions]);

  const formatCurrency = (value: number, cur?: string) => {
    if (!cur) {
      return value.toLocaleString('nl-NL', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    }
    return new Intl.NumberFormat('nl-NL', {
      style: 'currency',
      currency: cur,
    }).format(value);
  };

  const handleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('desc');
    }
    setCurrentPage(0);
  };

  const handleRowClick = (transaction: Transaction) => {
    if (transaction.type === 'INCOME') {
      navigate(`/payments/${transaction.id}`);
    } else {
      navigate(`/expenses/${transaction.id}`);
    }
  };

  const handleDownloadCSV = async () => {
    try {
      const params: Record<string, string> = {};
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;

      const response = await client.get('/reports/export/transactions/csv', {
        params,
        responseType: 'blob',
      });

      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'transactions.csv';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download CSV:', error);
      alert('Failed to download CSV. Please try again.');
    }
  };

  const handleDownloadPDF = async () => {
    try {
      const params: Record<string, string> = {};
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;

      const response = await client.get('/reports/export/transactions/pdf', {
        params,
        responseType: 'blob',
      });

      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'transaction-history.pdf';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download PDF:', error);
      alert('Failed to download PDF. Please try again.');
    }
  };

  return (
    <div className="px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <List className="h-8 w-8 text-[#5c7cfa] dark:text-[#91a7ff]" />
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Transaction History
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Complete history of income and expenses
          </p>
        </div>
        <div className="flex gap-2">
          <RefreshButton
            onClick={() => {
              refetchPayments();
              refetchExpenses();
            }}
            isRefreshing={paymentsFetching || expensesFetching}
          />
          {isEnabled(FeatureFlags.REPORTS) && (
            <>
              <button
                onClick={handleDownloadCSV}
                className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-[#14161f] border border-[#c9cfd9] dark:border-[#3a3f54] text-[#3d4463] dark:text-[#c4c8db] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:bg-[#0c0d14] transition-colors"
              >
                <Download className="h-4 w-4" />
                CSV
              </button>
              <button
                onClick={handleDownloadPDF}
                className="flex items-center gap-2 px-4 py-2 bg-[#5c7cfa] text-white rounded-md hover:bg-[#4c6ef5] transition-colors"
              >
                <FileText className="h-4 w-4" />
                PDF
              </button>
            </>
          )}
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <div className="bg-gradient-to-br from-green-50 to-green-100 rounded-xl shadow-sm p-6 border border-green-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-green-700 font-medium mb-1">
                Total Income
              </p>
              <p className="text-2xl font-bold text-green-900">
                {formatCurrency(totals.income, defaultCurrency)}
              </p>
            </div>
            <TrendingUp className="h-8 w-8 text-green-500" />
          </div>
        </div>

        <div className="bg-gradient-to-br from-red-50 to-red-100 rounded-xl shadow-sm p-6 border border-red-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-red-700 font-medium mb-1">
                Total Expenses
              </p>
              <p className="text-2xl font-bold text-red-900">
                {formatCurrency(totals.expenses, defaultCurrency)}
              </p>
            </div>
            <TrendingDown className="h-8 w-8 text-red-500" />
          </div>
        </div>

        <div
          className={`bg-gradient-to-br ${
            totals.net >= 0
              ? 'from-blue-50 to-blue-100 border-blue-200'
              : 'from-orange-50 to-orange-100 border-orange-200'
          } rounded-xl shadow-sm p-6 border`}
        >
          <div className="flex items-center justify-between">
            <div>
              <p
                className={`text-sm font-medium mb-1 ${
                  totals.net >= 0 ? 'text-blue-700' : 'text-orange-700'
                }`}
              >
                Net Total
              </p>
              <p
                className={`text-2xl font-bold ${
                  totals.net >= 0 ? 'text-blue-900' : 'text-orange-900'
                }`}
              >
                {formatCurrency(totals.net, defaultCurrency)}
              </p>
            </div>
            <ArrowUpDown
              className={`h-8 w-8 ${
                totals.net >= 0 ? 'text-blue-500' : 'text-orange-500'
              }`}
            />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              placeholder="Search transactions..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Type Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value as TransactionType);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="ALL">All Types</option>
              <option value="INCOME">Income Only</option>
              <option value="EXPENSE">Expenses Only</option>
            </select>
          </div>

          {/* Start Date */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* End Date */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
      </div>

      {/* Transactions Table */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      ) : filteredAndSortedTransactions.length === 0 ? (
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm p-12 text-center">
          <p className="text-[#6b7194] dark:text-[#8b90a8]">
            No transactions found
          </p>
        </div>
      ) : (
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
            <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
              <tr>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:bg-[#1e2130]"
                  onClick={() => handleSort('date')}
                >
                  <div className="flex items-center gap-1">
                    Date
                    {sortField === 'date' && (
                      <ArrowUpDown className="h-4 w-4" />
                    )}
                  </div>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Description
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Property
                </th>
                <th
                  className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:bg-[#1e2130]"
                  onClick={() => handleSort('amount')}
                >
                  <div className="flex items-center justify-end gap-1">
                    Amount
                    {sortField === 'amount' && (
                      <ArrowUpDown className="h-4 w-4" />
                    )}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
              {paginatedTransactions.map((transaction) => (
                <tr
                  key={`${transaction.type}-${transaction.id}`}
                  className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:bg-[#0c0d14] cursor-pointer transition-colors"
                  onClick={() => handleRowClick(transaction)}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                    {formatDate(transaction.date)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        transaction.type === 'INCOME'
                          ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300'
                          : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300'
                      }`}
                    >
                      {transaction.type === 'INCOME' ? (
                        <TrendingUp className="h-3 w-3" />
                      ) : (
                        <TrendingDown className="h-3 w-3" />
                      )}
                      {transaction.type}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                    {transaction.description}
                  </td>
                  <td className="px-6 py-4 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                    {transaction.property}
                  </td>
                  <td
                    className={`px-6 py-4 whitespace-nowrap text-sm font-semibold text-right ${
                      transaction.type === 'INCOME'
                        ? 'text-green-600'
                        : 'text-red-600'
                    }`}
                  >
                    {transaction.type === 'INCOME' ? '+' : '-'}
                    {formatCurrency(transaction.amount, transaction.currency)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          <div className="mt-4">
            <Pagination
              page={currentPage}
              totalPages={totalPages}
              totalElements={filteredAndSortedTransactions.length}
              size={pageSize}
              onPageChange={setCurrentPage}
              onSizeChange={(s) => {
                setPageSize(s);
                setCurrentPage(0);
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
};
