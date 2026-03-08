import { useState, useMemo, useCallback } from 'react';
import {
  Download,
  BarChart3,
  TrendingUp,
  PieChart,
  Home,
  Table2,
  Activity,
} from 'lucide-react';
import { useTheme } from '@/context/ThemeContext';
import { usePortfolioDashboard } from '@/hooks/usePortfolioDashboard';
import {
  exportPortfolioDashboardPDF,
  exportPortfolioDashboardCSV,
  exportPortfolioDashboardExcel,
} from '@/api/dashboard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ExportDropdown } from '@/components/common/ExportDropdown';
import { PortfolioSummaryCards } from './PortfolioSummaryCards';
import { PortfolioCashFlowChart } from './PortfolioCashFlowChart';
import { PropertyComparisonChart } from './PropertyComparisonChart';
import { PortfolioAllocationChart } from './PortfolioAllocationChart';
import { EquityCompositionChart } from './EquityCompositionChart';
import { PropertyPerformanceTable } from './PropertyPerformanceTable';
import { PortfolioOccupancyChart } from './PortfolioOccupancyChart';

type PeriodOption = { value: number | undefined; label: string };

const PERIOD_OPTIONS: PeriodOption[] = [
  { value: 6, label: '6M' },
  { value: 12, label: '12M' },
  { value: 24, label: '24M' },
  { value: undefined, label: 'All' },
];

export const PortfolioDashboard = () => {
  const [months, setMonths] = useState<number | undefined>(12);
  const [exporting, setExporting] = useState<'pdf' | 'csv' | 'excel' | null>(
    null
  );
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';

  const { data: dashboard, isLoading, error } = usePortfolioDashboard(months);

  const handleExport = useCallback(
    async (format: 'pdf' | 'csv' | 'excel') => {
      setExporting(format);
      try {
        let blob: Blob;
        let mimeType: string;
        let filename: string;
        if (format === 'pdf') {
          blob = await exportPortfolioDashboardPDF(months);
          mimeType = 'application/pdf';
          filename = 'portfolio-dashboard.pdf';
        } else if (format === 'excel') {
          blob = await exportPortfolioDashboardExcel(months);
          mimeType =
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
          filename = 'portfolio-dashboard.xlsx';
        } else {
          blob = await exportPortfolioDashboardCSV(months);
          mimeType = 'text/csv';
          filename = 'portfolio-dashboard.csv';
        }
        const file = new Blob([blob], { type: mimeType });
        const url = window.URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } catch {
        // silently fail
      } finally {
        setExporting(null);
      }
    },
    [months]
  );

  const is403 = useMemo(() => {
    if (
      error &&
      typeof error === 'object' &&
      'response' in error &&
      (error as { response?: { status?: number } }).response?.status === 403
    ) {
      return true;
    }
    return false;
  }, [error]);

  // Feature disabled — render nothing
  if (is403) {
    return null;
  }

  if (isLoading) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-8 border border-[#edf0f7] dark:border-[#2a2e3f]">
        <div className="flex items-center justify-center min-h-[200px]">
          <LoadingSpinner />
        </div>
      </div>
    );
  }

  if (error) {
    return null;
  }

  if (!dashboard) {
    return null;
  }

  if (dashboard.totalProperties === 0) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-8 border border-[#edf0f7] dark:border-[#2a2e3f] text-center">
        <Home className="h-10 w-10 text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8]">
          Add properties to see your portfolio dashboard.
        </p>
      </div>
    );
  }

  if (dashboard.propertiesWithFinancialData === 0) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-8 border border-[#edf0f7] dark:border-[#2a2e3f] text-center">
        <BarChart3 className="h-10 w-10 text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8]">
          Add financial data to your properties to see portfolio analytics.
        </p>
        <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-1">
          {dashboard.totalProperties} properties found, but none have financial
          data yet.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header: period selector + export + data indicator */}
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-5 border border-[#edf0f7] dark:border-[#2a2e3f]">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3 flex-wrap">
            <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Portfolio Overview
            </h2>
            <span className="text-xs text-[#6b7194] dark:text-[#8b90a8] bg-[#f1f3f9] dark:bg-[#1e2130] px-2 py-1 rounded-md">
              {dashboard.propertiesWithFinancialData} of{' '}
              {dashboard.totalProperties} properties have financial data
            </span>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex gap-1">
              {PERIOD_OPTIONS.map((opt) => (
                <button
                  key={opt.label}
                  onClick={() => setMonths(opt.value)}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    months === opt.value
                      ? 'bg-[#5c7cfa] text-white'
                      : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <div className="flex gap-2">
              <ExportDropdown
                size="sm"
                disabled={exporting !== null}
                exporting={exporting === 'csv' || exporting === 'excel'}
                options={[
                  { label: 'CSV', onExport: () => handleExport('csv') },
                  { label: 'Excel', onExport: () => handleExport('excel') },
                ]}
              />
              <button
                onClick={() => handleExport('pdf')}
                disabled={exporting !== null}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-[#e2e6f0] dark:border-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f5f7fa] dark:hover:bg-[#1e2130] transition-colors disabled:opacity-50"
              >
                <Download className="h-3.5 w-3.5" />
                {exporting === 'pdf' ? 'Exporting...' : 'PDF'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Summary Cards */}
      <PortfolioSummaryCards
        summary={dashboard.summary}
        currency={dashboard.currency}
      />

      {/* Cash Flow + Allocation */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Portfolio Cash Flow"
          icon={<BarChart3 className="h-5 w-5" />}
        >
          <PortfolioCashFlowChart
            data={dashboard.cashFlow.months}
            currency={dashboard.currency}
            isDark={isDark}
          />
        </ChartCard>

        <ChartCard
          title="Portfolio Allocation"
          icon={<PieChart className="h-5 w-5" />}
        >
          <PortfolioAllocationChart
            data={dashboard.allocation}
            isDark={isDark}
          />
        </ChartCard>
      </div>

      {/* Comparison + Equity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title="Property Comparison"
          icon={<TrendingUp className="h-5 w-5" />}
        >
          <PropertyComparisonChart
            data={dashboard.propertyComparison}
            currency={dashboard.currency}
            isDark={isDark}
          />
        </ChartCard>

        <ChartCard
          title="Equity Composition"
          icon={<Activity className="h-5 w-5" />}
        >
          <EquityCompositionChart
            data={dashboard.equityComposition.properties}
            currency={dashboard.currency}
            isDark={isDark}
          />
        </ChartCard>
      </div>

      {/* Performance Table */}
      <ChartCard
        title="Property Performance"
        icon={<Table2 className="h-5 w-5" />}
      >
        <PropertyPerformanceTable
          data={dashboard.propertyComparison}
          currency={dashboard.currency}
        />
      </ChartCard>

      {/* Occupancy */}
      <ChartCard
        title="Portfolio Occupancy"
        icon={<Home className="h-5 w-5" />}
      >
        <PortfolioOccupancyChart
          data={dashboard.occupancy.months}
          isDark={isDark}
        />
      </ChartCard>

      {/* Disclaimer */}
      <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] text-center">
        Metrics are for informational purposes only.
      </p>
    </div>
  );
};

// --- Reusable chart card wrapper ---

interface ChartCardProps {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}

const ChartCard = ({ title, icon, children }: ChartCardProps) => (
  <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-5 border border-[#edf0f7] dark:border-[#2a2e3f]">
    <div className="flex items-center gap-2 mb-4">
      <span className="text-[#6b7194] dark:text-[#8b90a8]">{icon}</span>
      <h3 className="text-base font-semibold text-[#1a1d2e] dark:text-[#c4c8db]">
        {title}
      </h3>
    </div>
    {children}
  </div>
);
