import { useState, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
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
import { Card, LoadingSpinner } from '@buurman/ui';
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
  { value: 12, label: '1Y' },
  { value: 24, label: '2Y' },
  { value: 36, label: '3Y' },
  { value: 48, label: '4Y' },
  { value: 60, label: '5Y' },
  { value: 120, label: '10Y' },
  { value: undefined, label: 'All' },
];

export const PortfolioDashboard = () => {
  const { t } = useTranslation('common');
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
      <Card padding="lg">
        <div className="flex items-center justify-center min-h-[200px]">
          <LoadingSpinner />
        </div>
      </Card>
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
      <Card padding="lg" className="text-center">
        <Home className="h-10 w-10 text-text-secondary mx-auto mb-3" />
        <p className="text-text-secondary">{t('dashboard.addProperties')}</p>
      </Card>
    );
  }

  if (dashboard.propertiesWithFinancialData === 0) {
    return (
      <Card padding="lg" className="text-center">
        <BarChart3 className="h-10 w-10 text-text-secondary mx-auto mb-3" />
        <p className="text-text-secondary">{t('dashboard.addFinancialData')}</p>
        <p className="text-xs text-text-muted mt-1">
          {t('dashboard.propertiesNoData', {
            count: dashboard.totalProperties,
          })}
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header: period selector + export + data indicator */}
      <Card>
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3 flex-wrap">
            <h2 className="text-lg font-semibold text-text-primary">
              {t('dashboard.portfolioOverview')}
            </h2>
            <span className="text-xs text-text-secondary bg-surface-inset px-2 py-1 rounded-md">
              {t('dashboard.propertiesWithData', {
                withData: dashboard.propertiesWithFinancialData,
                total: dashboard.totalProperties,
              })}
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
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
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
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border border-border-default text-text-secondary hover:bg-surface-inset transition-colors disabled:opacity-50"
              >
                <Download className="h-3.5 w-3.5" />
                {exporting === 'pdf' ? t('dashboard.exporting') : 'PDF'}
              </button>
            </div>
          </div>
        </div>
      </Card>

      {/* Summary Cards */}
      <PortfolioSummaryCards
        summary={dashboard.summary}
        currency={dashboard.currency}
      />

      {/* Cash Flow + Allocation */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard
          title={t('dashboard.charts.cashFlow')}
          icon={<BarChart3 className="h-5 w-5" />}
        >
          <PortfolioCashFlowChart
            data={dashboard.cashFlow.months}
            currency={dashboard.currency}
            isDark={isDark}
          />
        </ChartCard>

        <ChartCard
          title={t('dashboard.charts.allocation')}
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
          title={t('dashboard.charts.propertyComparison')}
          icon={<TrendingUp className="h-5 w-5" />}
        >
          <PropertyComparisonChart
            data={dashboard.propertyComparison}
            currency={dashboard.currency}
            isDark={isDark}
          />
        </ChartCard>

        <ChartCard
          title={t('dashboard.charts.equityComposition')}
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
        title={t('dashboard.charts.performance')}
        icon={<Table2 className="h-5 w-5" />}
      >
        <PropertyPerformanceTable
          data={dashboard.propertyComparison}
          currency={dashboard.currency}
        />
      </ChartCard>

      {/* Occupancy */}
      <ChartCard
        title={t('dashboard.charts.occupancy')}
        icon={<Home className="h-5 w-5" />}
      >
        <PortfolioOccupancyChart
          data={dashboard.occupancy.months}
          isDark={isDark}
        />
      </ChartCard>

      {/* Disclaimer */}
      <p className="text-xs text-text-muted text-center">
        {t('dashboard.disclaimer')}
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
  <Card>
    <div className="flex items-center gap-2 mb-4">
      <span className="text-text-secondary">{icon}</span>
      <h3 className="text-base font-semibold text-text-primary">{title}</h3>
    </div>
    {children}
  </Card>
);
