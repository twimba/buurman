import { useState, useMemo } from 'react';
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
import { format } from 'date-fns';

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

  const statusFilters = [
    { value: undefined, label: 'All Statuses' },
    { value: 'paid', label: 'Paid' },
    { value: 'pending', label: 'Pending' },
    { value: 'failed', label: 'Failed' },
  ];

  const statusConfig = {
    paid: {
      label: 'Paid',
      color: 'bg-green-100 text-green-800',
      icon: CheckCircle,
      iconColor: 'text-green-600',
    },
    pending: {
      label: 'Pending',
      color: 'bg-yellow-100 text-yellow-800',
      icon: Clock,
      iconColor: 'text-yellow-600',
    },
    failed: {
      label: 'Failed',
      color: 'bg-red-100 text-red-800',
      icon: XCircle,
      iconColor: 'text-red-600',
    },
  };

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
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Next Payment</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {nextPayment.currency} {nextPayment.amount.toFixed(2)}
              </p>
              <p className="text-xs text-gray-600 mt-1">
                {format(new Date(nextPayment.date), 'MMM d, yyyy')}
              </p>
            </div>
            <Calendar className="h-12 w-12 text-blue-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Paid</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                EUR {totalPaid.toFixed(2)}
              </p>
              <p className="text-xs text-gray-600 mt-1">All time</p>
            </div>
            <CreditCard className="h-12 w-12 text-green-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Invoices</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {invoices.length}
              </p>
              <p className="text-xs text-gray-600 mt-1">
                {invoices.filter((i) => i.status === 'paid').length} paid
              </p>
            </div>
            <Receipt className="h-12 w-12 text-purple-600" />
          </div>
        </div>
      </div>

      {/* Payment History Table */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Payment History
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            View and download your invoices
          </p>
        </div>

        {/* Search and Filters */}
        <div className="p-6 border-b border-gray-200 space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by invoice number, plan, or payment method..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div className="flex items-center gap-2">
            <Filter className="h-5 w-5 text-gray-600" />
            <h3 className="font-semibold text-gray-900 text-sm">
              Status Filter
            </h3>
          </div>
          <div className="flex gap-2 flex-wrap">
            {statusFilters.map((filter) => (
              <button
                key={filter.label}
                onClick={() => setStatusFilter(filter.value)}
                className={`px-4 py-2 rounded transition-colors text-sm ${
                  statusFilter === filter.value
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>

        {/* Invoices List */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Invoice
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Plan
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Payment Method
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredInvoices.map((invoice) => {
                const StatusIcon = statusConfig[invoice.status].icon;
                return (
                  <tr key={invoice.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <Receipt className="h-4 w-4 text-gray-400" />
                        <span className="text-sm font-medium text-gray-900">
                          {invoice.invoiceNumber}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {format(new Date(invoice.date), 'MMM d, yyyy')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {invoice.plan}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {invoice.paymentMethod}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
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
                            ? 'text-blue-600 hover:text-blue-800'
                            : 'text-gray-400 cursor-not-allowed'
                        }`}
                      >
                        <Download className="h-4 w-4" />
                        Download
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
            <Receipt className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No invoices found
            </h3>
            <p className="text-gray-600">
              {statusFilter || searchTerm
                ? 'Try adjusting your filters or search'
                : 'Your payment history will appear here'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
