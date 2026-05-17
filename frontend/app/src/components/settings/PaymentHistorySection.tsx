import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Receipt,
  Download,
  CheckCircle,
  Clock,
  XCircle,
  Calendar,
  CreditCard,
  Search,
  Filter,
} from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';
import { DataList } from '@buurman/ui';

interface Invoice {
  id: string;
  invoiceNumber: string;
  date: string;
  amount: number;
  currency: string;
  status: 'paid' | 'pending' | 'failed';
  plan: string;
  paymentMethod: string;
  pdfUrl?: string;
}

export const PaymentHistorySection = () => {
  const { t } = useTranslation('settings');
  const { formatDate } = useFormatDate();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>(
    undefined
  );

  // Mock data - will be replaced with actual payment history
  const [invoices] = useState<Invoice[]>([
    {
      id: '1',
      invoiceNumber: 'INV-2026-001',
      date: '2026-02-01',
      amount: 29.99,
      currency: 'EUR',
      status: 'paid',
      plan: 'Professional',
      paymentMethod: 'Visa •••• 4242',
    },
    {
      id: '2',
      invoiceNumber: 'INV-2026-002',
      date: '2026-01-01',
      amount: 29.99,
      currency: 'EUR',
      status: 'paid',
      plan: 'Professional',
      paymentMethod: 'Visa •••• 4242',
    },
    {
      id: '3',
      invoiceNumber: 'INV-2025-012',
      date: '2025-12-01',
      amount: 29.99,
      currency: 'EUR',
      status: 'paid',
      plan: 'Professional',
      paymentMethod: 'Visa •••• 4242',
    },
    {
      id: '4',
      invoiceNumber: 'INV-2025-011',
      date: '2025-11-01',
      amount: 9.99,
      currency: 'EUR',
      status: 'paid',
      plan: 'Starter',
      paymentMethod: 'Visa •••• 4242',
    },
  ]);

  const nextPayment = {
    date: '2026-03-01',
    amount: 29.99,
    currency: 'EUR',
    plan: 'Professional',
  };

  const statusFilters = useMemo(
    () => [
      { value: undefined, label: t('paymentHistory.allStatuses') },
      { value: 'paid', label: t('paymentHistory.statuses.paid') },
      { value: 'pending', label: t('paymentHistory.statuses.pending') },
      { value: 'failed', label: t('paymentHistory.statuses.failed') },
    ],
    [t]
  );

  const statusConfig = useMemo(
    () => ({
      paid: {
        label: t('paymentHistory.statuses.paid'),
        color: 'bg-success-bg text-success-text',
        icon: CheckCircle,
        iconColor: 'text-success-text',
      },
      pending: {
        label: t('paymentHistory.statuses.pending'),
        color: 'bg-warning-bg text-warning-text',
        icon: Clock,
        iconColor: 'text-warning-text',
      },
      failed: {
        label: t('paymentHistory.statuses.failed'),
        color: 'bg-error-bg text-error-text',
        icon: XCircle,
        iconColor: 'text-error-text',
      },
    }),
    [t]
  );

  const filteredInvoices = useMemo(() => {
    let filtered = invoices;

    if (statusFilter) {
      filtered = filtered.filter((inv) => inv.status === statusFilter);
    }

    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (inv) =>
          inv.invoiceNumber.toLowerCase().includes(search) ||
          inv.plan.toLowerCase().includes(search) ||
          inv.paymentMethod.toLowerCase().includes(search)
      );
    }

    return filtered;
  }, [invoices, statusFilter, searchTerm]);

  const totalPaid = useMemo(
    () =>
      invoices
        .filter((inv) => inv.status === 'paid')
        .reduce((sum, inv) => sum + inv.amount, 0),
    [invoices]
  );

  const handleDownloadInvoice = (invoice: Invoice) => {
    // TODO: API call to download invoice
    console.log('Download invoice:', invoice.invoiceNumber);
  };

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid md:grid-cols-3 gap-6">
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-text-secondary">
                {t('paymentHistory.nextPayment')}
              </p>
              <p className="text-2xl font-bold text-text-primary mt-1">
                {nextPayment.currency} {nextPayment.amount.toFixed(2)}
              </p>
              <p className="text-xs text-text-secondary mt-1">
                {formatDate(nextPayment.date)}
              </p>
            </div>
            <Calendar className="h-12 w-12 text-primary-500 dark:text-primary-300" />
          </div>
        </div>

        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-text-secondary">
                {t('paymentHistory.totalPaid')}
              </p>
              <p className="text-2xl font-bold text-text-primary mt-1">
                EUR {totalPaid.toFixed(2)}
              </p>
              <p className="text-xs text-text-secondary mt-1">
                {t('paymentHistory.allTime')}
              </p>
            </div>
            <CreditCard className="h-12 w-12 text-success-text" />
          </div>
        </div>

        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-text-secondary">
                {t('paymentHistory.totalInvoices')}
              </p>
              <p className="text-2xl font-bold text-text-primary mt-1">
                {invoices.length}
              </p>
              <p className="text-xs text-text-secondary mt-1">
                {t('paymentHistory.paid', {
                  count: invoices.filter((i) => i.status === 'paid').length,
                })}
              </p>
            </div>
            <Receipt className="h-12 w-12 text-purple-600" />
          </div>
        </div>
      </div>

      {/* Payment History Table */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
        <div className="p-6 border-b border-border-default">
          <h2 className="text-xl font-semibold text-text-primary">
            {t('paymentHistory.title')}
          </h2>
          <p className="text-sm text-text-secondary mt-1">
            {t('paymentHistory.subtitle')}
          </p>
        </div>

        {/* Search and Filters */}
        <div className="p-6 border-b border-border-default space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-text-muted " />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={t('paymentHistory.searchPlaceholder')}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>

          <div className="flex items-center gap-2">
            <Filter className="h-5 w-5 text-text-secondary " />
            <h3 className="font-semibold text-text-primary text-sm">
              {t('paymentHistory.statusFilter')}
            </h3>
          </div>
          <div className="flex gap-2 flex-wrap">
            {statusFilters.map((filter) => (
              <button
                key={filter.label}
                onClick={() => setStatusFilter(filter.value)}
                className={`px-4 py-2 rounded transition-colors text-sm ${
                  statusFilter === filter.value
                    ? 'bg-primary-500 text-white'
                    : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>

        {/* Phone: card list */}
        <ul className="md:hidden space-y-3 mb-4">
          {filteredInvoices.map((invoice) => {
            const StatusIcon = statusConfig[invoice.status].icon;
            return (
              <li
                key={`m-${invoice.id}`}
                className="bg-surface-card rounded-lg border border-border-default p-4"
              >
                <DataList
                  title={invoice.invoiceNumber}
                  trailing={
                    <span
                      className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded ${statusConfig[invoice.status].color}`}
                    >
                      <StatusIcon
                        className={`h-3.5 w-3.5 ${statusConfig[invoice.status].iconColor}`}
                      />
                      {statusConfig[invoice.status].label}
                    </span>
                  }
                  items={[
                    {
                      label: t('paymentHistory.date'),
                      value: formatDate(invoice.date),
                    },
                    {
                      label: t('paymentHistory.plan'),
                      value: invoice.plan,
                    },
                    {
                      label: t('paymentHistory.amount'),
                      value: (
                        <span className="font-semibold text-text-primary">
                          {invoice.currency} {invoice.amount.toFixed(2)}
                        </span>
                      ),
                      align: 'right',
                    },
                  ]}
                />
                <button
                  onClick={() => handleDownloadInvoice(invoice)}
                  disabled={invoice.status !== 'paid'}
                  className={`mt-3 w-full inline-flex items-center justify-center gap-2 min-h-touch rounded border ${
                    invoice.status === 'paid'
                      ? 'border-primary-300 text-primary-600 hover:bg-primary-50 focus-ring'
                      : 'border-border-default text-text-muted cursor-not-allowed'
                  }`}
                >
                  <Download className="h-4 w-4" />
                  {t('paymentHistory.download')}
                </button>
              </li>
            );
          })}
        </ul>

        {/* Invoices Table (md+) */}
        <div className="hidden md:block overflow-x-auto">
          <table className="min-w-full divide-y divide-border-default">
            <thead className="bg-surface-page">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.invoice')}
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.date')}
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.plan')}
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.paymentMethod')}
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.amount')}
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.status')}
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                  {t('paymentHistory.actions')}
                </th>
              </tr>
            </thead>
            <tbody className="bg-surface-card divide-y divide-border-default">
              {filteredInvoices.map((invoice) => {
                const StatusIcon = statusConfig[invoice.status].icon;
                return (
                  <tr key={invoice.id} className="hover:bg-surface-inset">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <Receipt className="h-4 w-4 text-text-muted " />
                        <span className="text-sm font-medium text-text-primary">
                          {invoice.invoiceNumber}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                      {formatDate(invoice.date)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                      {invoice.plan}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
                      {invoice.paymentMethod}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-text-primary">
                      {invoice.currency} {invoice.amount.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <StatusIcon
                          className={`h-4 w-4 ${statusConfig[invoice.status].iconColor}`}
                        />
                        <span
                          className={`px-2 py-1 text-xs font-semibold rounded ${statusConfig[invoice.status].color}`}
                        >
                          {statusConfig[invoice.status].label}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <button
                        onClick={() => handleDownloadInvoice(invoice)}
                        disabled={invoice.status !== 'paid'}
                        className={`flex items-center gap-1 ${
                          invoice.status === 'paid'
                            ? 'text-primary-500 dark:text-primary-300 hover:text-primary-600'
                            : 'text-text-muted cursor-not-allowed'
                        }`}
                      >
                        <Download className="h-4 w-4" />
                        {t('paymentHistory.download')}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {filteredInvoices.length === 0 && (
          <div className="p-12 text-center">
            <Receipt className="h-12 w-12 text-text-muted mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              {t('paymentHistory.noInvoices')}
            </h3>
            <p className="text-text-secondary">
              {statusFilter || searchTerm
                ? t('paymentHistory.adjustFilters')
                : t('paymentHistory.historyWillAppear')}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
