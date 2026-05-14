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
  Maximize2,
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
import {
  PortfolioDashboardFullscreen,
  type DashboardChartType,
} from './PortfolioDashboardFullscreen';

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
  const [customStart, setCustomStart] = useState(() => {
    const d = new Date();
    d.setFullYear(d.getFullYear() - 1);
    return d.toISOString().split('T')[0];
  });
  const [customEnd, setCustomEnd] = useState(
    () => new Date().toISOString().split('T')[0]
  );
  const [isCustom, setIsCustom] = useState(false);
  const [exporting, setExporting] = useState<'pdf' | 'csv' | 'excel' | null>(
    null
  );
  const [expandedChart, setExpandedChart] =
    useState<DashboardChartType | null>(null);
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';

  const activeMonths = isCustom ? undefined : months;
  const activeStartDate = isCustom ? customStart : undefined;
  const activeEndDate = isCustom ? customEnd : undefined;

  const { data: dashboard, isLoading, error } = usePortfolioDashboard(
    activeMonths,
    activeStartDate,
    activeEndDate
  );

  const handlePeriodClick = useCallback((opt: PeriodOption) => {
    setIsCustom(false);
    setMonths(opt.value);
    const today = new Date();
    setCustomEnd(today.toISOString().split('T')[0]);
    if (opt.value === undefined) {
      setCustomStart('');
    } else {
      const start = new Date(today);
      start.setMonth(start.getMonth() - opt.value);
      setCustomStart(start.toISOString().split('T')[0]);
    }
  }, []);

  const handleExport = useCallback(
    async (format: 'pdf' | 'csv' | 'excel') => {
      setExporting(format);
      try {
        let blob: Blob;
        let mimeType: string;
        let filename: string;
        if (format === 'pdf') {
          blob = await exportPortfolioDashboardPDF(activeMonths, activeStartDate, activeEndDate);
          mimeType = 'application/pdf';
          filename = 'portfolio-dashboard.pdf';
        } else if (format === 'excel') {
          blob = await exportPortfolioDashboardExcel(activeMonths, activeStartDate, activeEndDate);
          mimeType =
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
          filename = 'portfolio-dashboard.xlsx';
        } else {
          blob = await exportPortfolioDashboardCSV(activeMonths, activeStartDate, activeEndDate);
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
    [activeMonths, activeStartDate, activeEndDate]
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
          <div className="flex items-center gap-3 flex-wrap">
            <div className="flex gap-1">
              {PERIOD_OPTIONS.map((opt) => (
                <button
                  key={opt.label}
                  onClick={() => handlePeriodClick(opt)}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    months === opt.value && !isCustom
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <div className="flex gap-2 items-center">
              <input
                type="date"
                value={customStart}
                onChange={(e) => {
                  setCustomStart(e.target.value);
                  setIsCustom(true);
                  setMonths(undefined);
                }}
                className="px-3 py-1.5 border border-border-strong rounded-md text-xs bg-surface-card text-text-primary focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
              <span className="text-text-muted text-xs">–</span>
              <input
                type="date"
                value={customEnd}
                onChange={(e) => {
                  setCustomEnd(e.target.value);
                  setIsCustom(true);
                  setMonths(undefined);
                }}
                className="px-3 py-1.5 border border-border-strong rounded-md text-xs bg-surface-card text-text-primary focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
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
          onExpand={() => setExpandedChart('cashflow')}
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
          onExpand={() => setExpandedChart('allocation')}
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
          onExpand={() => setExpandedChart('comparison')}
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
          onExpand={() => setExpandedChart('equity')}
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
        onExpand={() => setExpandedChart('performance')}
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
        onExpand={() => setExpandedChart('occupancy')}
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

      {expandedChart && (
        <PortfolioDashboardFullscreen
          chartType={expandedChart}
          initialMonths={activeMonths}
          initialStartDate={activeStartDate}
          initialEndDate={activeEndDate}
          onClose={() => setExpandedChart(null)}
        />
      )}
    </div>
  );
};

// --- Reusable chart card wrapper ---

interface ChartCardProps {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
  onExpand?: () => void;
}

const ChartCard = ({ title, icon, children, onExpand }: ChartCardProps) => (
  <Card>
    <div className="flex items-center justify-between mb-4">
      <div className="flex items-center gap-2">
        <span className="text-text-secondary">{icon}</span>
        <h3 className="text-base font-semibold text-text-primary">{title}</h3>
      </div>
      {onExpand && (
        <button
          onClick={onExpand}
          className="p-1.5 rounded-md text-text-muted hover:text-text-secondary hover:bg-surface-inset transition-colors"
          title="Expand chart"
        >
          <Maximize2 className="h-4 w-4" />
        </button>
      )}
    </div>
    {children}
  </Card>
);
