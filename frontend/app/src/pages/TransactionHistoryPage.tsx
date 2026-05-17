import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Filter,
  TrendingUp,
  TrendingDown,
  Calendar,
  ArrowUpDown,
  FileText,
  List,
} from 'lucide-react';
import { usePayments } from '@/hooks/usePaymentHooks';
import { useExpenses } from '@/hooks/useExpenseHooks';
import {
  DataList,
  LoadingSpinner,
  Pagination,
  RefreshButton,
} from '@buurman/ui';
import { PaymentStatus } from '@/types/payment';
import client from '@/api/client';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { ExportDropdown } from '@/components/common/ExportDropdown';
import { ExportOptionIcon } from '@/components/common/ExportOptionIcon';
import { useGoogleSheetsExport } from '@/hooks/useGoogleSheetsExport';
import { exportTransactionsGoogleSheet } from '@/api/googleSheetsExport';
import { GoogleSheetExportPill } from '@/components/common/GoogleSheetExportPill';

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
  const { t } = useTranslation('admin');
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const { isEnabled } = useFeatureFlags();
  const {
    triggerExport: triggerGoogleSheet,
    isExporting: isGoogleExporting,
    lastResult: lastGoogleSheet,
    clearLastResult: clearLastGoogleSheet,
  } = useGoogleSheetsExport();
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
            date: payment.paymentDate ?? '',
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

  const handleDownloadExcel = async () => {
    try {
      const params: Record<string, string> = {};
      if (startDate) {
        params.startDate = startDate;
      }
      if (endDate) {
        params.endDate = endDate;
      }

      const response = await client.get('/reports/export/transactions/excel', {
        params,
        responseType: 'blob',
      });

      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'transactions.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download Excel:', error);
      alert(t('transactions.downloadFailed'));
    }
  };

  const handleDownloadCSV = async () => {
    try {
      const params: Record<string, string> = {};
      if (startDate) {
        params.startDate = startDate;
      }
      if (endDate) {
        params.endDate = endDate;
      }

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
      alert(t('transactions.downloadFailed'));
    }
  };

  const handleDownloadPDF = async () => {
    try {
      const params: Record<string, string> = {};
      if (startDate) {
        params.startDate = startDate;
      }
      if (endDate) {
        params.endDate = endDate;
      }

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
      alert(t('transactions.downloadFailed'));
    }
  };

  return (
    <div className="px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <List className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('transactions.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('transactions.subtitle')}
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
              <GoogleSheetExportPill
                result={lastGoogleSheet}
                onDismiss={clearLastGoogleSheet}
              />
              <ExportDropdown
                size="md"
                exporting={isGoogleExporting}
                disabled={isGoogleExporting}
                options={[
                  {
                    label: 'CSV',
                    icon: <ExportOptionIcon format="csv" />,
                    onExport: handleDownloadCSV,
                  },
                  ...(isEnabled(FeatureFlags.EXCEL_EXPORT)
                    ? [
                        {
                          label: 'Excel',
                          icon: <ExportOptionIcon format="excel" />,
                          onExport: handleDownloadExcel,
                        },
                      ]
                    : []),
                  ...(isEnabled(FeatureFlags.GOOGLE_SHEETS_EXPORT)
                    ? [
                        {
                          label: 'Google Sheets',
                          icon: <ExportOptionIcon format="google-sheets" />,
                          onExport: () =>
                            triggerGoogleSheet((token) =>
                              exportTransactionsGoogleSheet(
                                token,
                                startDate || undefined,
                                endDate || undefined
                              )
                            ),
                        },
                      ]
                    : []),
                ]}
              />
              <button
                onClick={handleDownloadPDF}
                className="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-md hover:bg-primary-600 transition-colors"
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
        <div className="bg-success-bg rounded-lg shadow-sm p-6 border border-success-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-success-text font-medium mb-1">
                {t('transactions.totalIncome')}
              </p>
              <p className="text-2xl font-bold text-success-text">
                {formatCurrency(totals.income, defaultCurrency)}
              </p>
            </div>
            <TrendingUp className="h-8 w-8 text-success-text" />
          </div>
        </div>

        <div className="bg-error-bg rounded-lg shadow-sm p-6 border border-error-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-error-text font-medium mb-1">
                {t('transactions.totalExpenses')}
              </p>
              <p className="text-2xl font-bold text-error-text">
                {formatCurrency(totals.expenses, defaultCurrency)}
              </p>
            </div>
            <TrendingDown className="h-8 w-8 text-error-text" />
          </div>
        </div>

        <div
          className={`bg-gradient-to-br ${
            totals.net >= 0
              ? 'bg-info-bg border-info-border'
              : 'bg-warning-bg border-warning-border'
          } rounded-lg shadow-sm p-6 border`}
        >
          <div className="flex items-center justify-between">
            <div>
              <p
                className={`text-sm font-medium mb-1 ${
                  totals.net >= 0 ? 'text-info-text' : 'text-warning-text'
                }`}
              >
                {t('transactions.netTotal')}
              </p>
              <p
                className={`text-2xl font-bold ${
                  totals.net >= 0 ? 'text-info-text' : 'text-warning-text'
                }`}
              >
                {formatCurrency(totals.net, defaultCurrency)}
              </p>
            </div>
            <ArrowUpDown
              className={`h-8 w-8 ${
                totals.net >= 0 ? 'text-info-text' : 'text-warning-text'
              }`}
            />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <input
              type="text"
              placeholder={t('transactions.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Type Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value as TransactionType);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="ALL">{t('transactions.allTypes')}</option>
              <option value="INCOME">{t('transactions.incomeOnly')}</option>
              <option value="EXPENSE">{t('transactions.expensesOnly')}</option>
            </select>
          </div>

          {/* Start Date */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* End Date */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
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
        <div className="bg-surface-card rounded-lg shadow-sm p-12 text-center">
          <p className="text-text-secondary">
            {t('transactions.noTransactions')}
          </p>
        </div>
      ) : (
        <>
        {/* Mobile card list (<md). */}
        <ul className="md:hidden space-y-3 mb-4">
          {paginatedTransactions.map((transaction) => (
            <li key={`m-${transaction.type}-${transaction.id}`}>
              <button
                type="button"
                onClick={() => handleRowClick(transaction)}
                className="block w-full text-left bg-surface-card rounded-lg border border-border-default p-4 min-h-touch hover:border-primary-300 transition-colors focus-ring"
              >
                <DataList
                  title={transaction.description}
                  trailing={
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        transaction.type === 'INCOME'
                          ? 'bg-success-bg text-success-text'
                          : 'bg-error-bg text-error-text'
                      }`}
                    >
                      {transaction.type === 'INCOME' ? (
                        <TrendingUp className="h-3 w-3" />
                      ) : (
                        <TrendingDown className="h-3 w-3" />
                      )}
                      {transaction.type}
                    </span>
                  }
                  items={[
                    {
                      label: t('transactions.table.date'),
                      value: formatDate(transaction.date),
                    },
                    {
                      label: t('transactions.table.property'),
                      value: transaction.property,
                    },
                    {
                      label: t('transactions.table.amount'),
                      value: (
                        <span
                          className={`font-semibold ${
                            transaction.type === 'INCOME'
                              ? 'text-success-text'
                              : 'text-error-text'
                          }`}
                        >
                          {transaction.type === 'INCOME' ? '+' : '-'}
                          {formatCurrency(
                            transaction.amount,
                            transaction.currency
                          )}
                        </span>
                      ),
                      align: 'right',
                    },
                  ]}
                />
              </button>
            </li>
          ))}
        </ul>

        {/* md+ table */}
        <div className="hidden md:block bg-surface-card rounded-lg shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-border-default">
              <thead className="bg-surface-page">
                <tr>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('date')}
                  >
                    <div className="flex items-center gap-1">
                      {t('transactions.table.date')}
                      {sortField === 'date' && (
                        <ArrowUpDown className="h-4 w-4" />
                      )}
                    </div>
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('transactions.table.type')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('transactions.table.description')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                    {t('transactions.table.property')}
                  </th>
                  <th
                    className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('amount')}
                  >
                    <div className="flex items-center justify-end gap-1">
                      {t('transactions.table.amount')}
                      {sortField === 'amount' && (
                        <ArrowUpDown className="h-4 w-4" />
                      )}
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody className="bg-surface-card divide-y divide-border-default">
                {paginatedTransactions.map((transaction) => (
                  <tr
                    key={`${transaction.type}-${transaction.id}`}
                    className="hover:bg-primary-50 cursor-pointer transition-colors"
                    onClick={() => handleRowClick(transaction)}
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                      {formatDate(transaction.date)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          transaction.type === 'INCOME'
                            ? 'bg-success-bg text-success-text'
                            : 'bg-error-bg text-error-text'
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
                    <td className="px-6 py-4 text-sm text-text-primary">
                      {transaction.description}
                    </td>
                    <td className="px-6 py-4 text-sm text-text-secondary">
                      {transaction.property}
                    </td>
                    <td
                      className={`px-6 py-4 whitespace-nowrap text-sm font-semibold text-right ${
                        transaction.type === 'INCOME'
                          ? 'text-success-text'
                          : 'text-error-text'
                      }`}
                    >
                      {transaction.type === 'INCOME' ? '+' : '-'}
                      {formatCurrency(transaction.amount, transaction.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

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
        </>
      )}
    </div>
  );
};
