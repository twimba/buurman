import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  BarChart3,
  PieChart,
  TrendingUp,
  Activity,
  Table2,
  Home,
} from 'lucide-react';
import { useTheme } from '@/context/ThemeContext';
import { usePortfolioDashboard } from '@/hooks/usePortfolioDashboard';
import { LoadingSpinner } from '@buurman/ui';
import { ChartFullscreenModal } from '@/components/common/ChartFullscreenModal';
import { PortfolioCashFlowChart } from './PortfolioCashFlowChart';
import { PortfolioAllocationChart } from './PortfolioAllocationChart';
import { PropertyComparisonChart } from './PropertyComparisonChart';
import { EquityCompositionChart } from './EquityCompositionChart';
import { PropertyPerformanceTable } from './PropertyPerformanceTable';
import { PortfolioOccupancyChart } from './PortfolioOccupancyChart';

export type DashboardChartType =
  | 'cashflow'
  | 'allocation'
  | 'comparison'
  | 'equity'
  | 'performance'
  | 'occupancy';

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

interface Props {
  chartType: DashboardChartType;
  initialMonths: number | undefined;
  onClose: () => void;
}

export const PortfolioDashboardFullscreen = ({
  chartType,
  initialMonths,
  onClose,
}: Props) => {
  const { t } = useTranslation('common');
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';

  const [localMonths, setLocalMonths] = useState<number | undefined>(
    initialMonths
  );

  const { data: dashboard, isLoading } = usePortfolioDashboard(localMonths);

  const chartConfig: Record<
    DashboardChartType,
    { title: string; icon: React.ReactNode }
  > = {
    cashflow: {
      title: t('dashboard.charts.cashFlow'),
      icon: <BarChart3 className="h-5 w-5" />,
    },
    allocation: {
      title: t('dashboard.charts.allocation'),
      icon: <PieChart className="h-5 w-5" />,
    },
    comparison: {
      title: t('dashboard.charts.propertyComparison'),
      icon: <TrendingUp className="h-5 w-5" />,
    },
    equity: {
      title: t('dashboard.charts.equityComposition'),
      icon: <Activity className="h-5 w-5" />,
    },
    performance: {
      title: t('dashboard.charts.performance'),
      icon: <Table2 className="h-5 w-5" />,
    },
    occupancy: {
      title: t('dashboard.charts.occupancy'),
      icon: <Home className="h-5 w-5" />,
    },
  };

  const { title, icon } = chartConfig[chartType];

  const periodControls = (
    <div className="flex gap-1 flex-wrap">
      {PERIOD_OPTIONS.map((opt) => (
        <button
          key={opt.label}
          onClick={() => setLocalMonths(opt.value)}
          className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
            localMonths === opt.value
              ? 'bg-primary-500 text-white'
              : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );

  const renderChart = () => {
    if (isLoading) {
      return (
        <div className="flex items-center justify-center h-[520px]">
          <LoadingSpinner />
        </div>
      );
    }

    if (!dashboard) {
      return null;
    }

    switch (chartType) {
      case 'cashflow':
        return (
          <PortfolioCashFlowChart
            data={dashboard.cashFlow.months}
            currency={dashboard.currency}
            isDark={isDark}
            height={520}
          />
        );
      case 'allocation':
        return (
          <PortfolioAllocationChart
            data={dashboard.allocation}
            isDark={isDark}
            height={520}
          />
        );
      case 'comparison':
        return (
          <PropertyComparisonChart
            data={dashboard.propertyComparison}
            currency={dashboard.currency}
            isDark={isDark}
          />
        );
      case 'equity':
        return (
          <EquityCompositionChart
            data={dashboard.equityComposition.properties}
            currency={dashboard.currency}
            isDark={isDark}
          />
        );
      case 'performance':
        return (
          <PropertyPerformanceTable
            data={dashboard.propertyComparison}
            currency={dashboard.currency}
          />
        );
      case 'occupancy':
        return (
          <PortfolioOccupancyChart
            data={dashboard.occupancy.months}
            isDark={isDark}
            height={520}
          />
        );
      default:
        return null;
    }
  };

  return (
    <ChartFullscreenModal
      title={title}
      icon={icon}
      controls={periodControls}
      onClose={onClose}
    >
      {renderChart()}
    </ChartFullscreenModal>
  );
};
