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
  initialStartDate?: string;
  initialEndDate?: string;
  onClose: () => void;
}

export const PortfolioDashboardFullscreen = ({
  chartType,
  initialMonths,
  initialStartDate,
  initialEndDate,
  onClose,
}: Props) => {
  const { t } = useTranslation('common');
  const { effectiveTheme } = useTheme();
  const isDark = effectiveTheme === 'dark';

  const [localMonths, setLocalMonths] = useState<number | undefined>(
    initialMonths
  );
  const [localCustomStart, setLocalCustomStart] = useState<string>(
    initialStartDate ??
      (() => {
        const d = new Date();
        d.setFullYear(d.getFullYear() - 1);
        return d.toISOString().split('T')[0];
      })()
  );
  const [localCustomEnd, setLocalCustomEnd] = useState<string>(
    initialEndDate ?? new Date().toISOString().split('T')[0]
  );
  const [localIsCustom, setLocalIsCustom] = useState<boolean>(
    initialStartDate != null
  );

  const activeLocalMonths = localIsCustom ? undefined : localMonths;
  const activeLocalStartDate = localIsCustom ? localCustomStart : undefined;
  const activeLocalEndDate = localIsCustom ? localCustomEnd : undefined;

  const { data: dashboard, isLoading } = usePortfolioDashboard(
    activeLocalMonths,
    activeLocalStartDate,
    activeLocalEndDate
  );

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

  const handleLocalPeriodClick = (opt: PeriodOption) => {
    setLocalIsCustom(false);
    setLocalMonths(opt.value);
    const today = new Date();
    setLocalCustomEnd(today.toISOString().split('T')[0]);
    if (opt.value === undefined) {
      setLocalCustomStart('');
    } else {
      const start = new Date(today);
      start.setMonth(start.getMonth() - opt.value);
      setLocalCustomStart(start.toISOString().split('T')[0]);
    }
  };

  const periodControls = (
    <div className="flex gap-2 flex-wrap items-center">
      <div className="flex gap-1 flex-wrap">
        {PERIOD_OPTIONS.map((opt) => (
          <button
            key={opt.label}
            onClick={() => handleLocalPeriodClick(opt)}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
              localMonths === opt.value && !localIsCustom
                ? 'bg-primary-500 text-white'
                : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>
      <div className="flex gap-2 items-center">
        <input
          type="date"
          value={localCustomStart}
          onChange={(e) => {
            setLocalCustomStart(e.target.value);
            setLocalIsCustom(true);
            setLocalMonths(undefined);
          }}
          className="px-3 py-1.5 border border-border-strong rounded-md text-xs bg-surface-card text-text-primary focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        />
        <span className="text-text-muted text-xs">–</span>
        <input
          type="date"
          value={localCustomEnd}
          onChange={(e) => {
            setLocalCustomEnd(e.target.value);
            setLocalIsCustom(true);
            setLocalMonths(undefined);
          }}
          className="px-3 py-1.5 border border-border-strong rounded-md text-xs bg-surface-card text-text-primary focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        />
      </div>
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
