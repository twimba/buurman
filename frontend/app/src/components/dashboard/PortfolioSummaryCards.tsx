import { Building2, TrendingUp, DollarSign, Percent, Home } from 'lucide-react';
import { MetricCard } from '@buurman/ui';
import type { PortfolioSummary } from '@/types/portfolio';
import { MetricHint } from '@/components/common/MetricHint';

interface PortfolioSummaryCardsProps {
  summary: PortfolioSummary;
  currency?: string;
}

function formatMoney(value: number | undefined, currencyCode?: string): string {
  if (value == null) {
    return 'N/A';
  }
  if (currencyCode) {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: currencyCode,
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(value);
    } catch {
      // fall through
    }
  }
  return value.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });
}

function formatPercent(value: number | undefined): string {
  if (value == null) {
    return 'N/A';
  }
  return `${value.toFixed(1)}%`;
}

type IconBgVariant =
  | 'primary'
  | 'accent'
  | 'success'
  | 'warning'
  | 'error'
  | 'info';

function rateColor(
  value: number | undefined,
  greenThreshold: number,
  yellowThreshold: number
): string {
  if (value == null) {
    return '';
  }
  if (value >= greenThreshold) {
    return 'text-success';
  }
  if (value >= yellowThreshold) {
    return 'text-warning';
  }
  return 'text-error';
}

function cashFlowColor(value: number | undefined): string {
  if (value == null) {
    return '';
  }
  return value >= 0 ? 'text-success' : 'text-error';
}

export const PortfolioSummaryCards = ({
  summary,
  currency,
}: PortfolioSummaryCardsProps) => {
  const cards: {
    key: string;
    label: React.ReactNode;
    value: string;
    valueClassName: string;
    icon: React.ReactNode;
    iconBgVariant: IconBgVariant;
  }[] = [
    {
      key: 'portfolio-value',
      label: <MetricHint label="Portfolio Value" />,
      value: formatMoney(summary.totalPortfolioValue, currency),
      valueClassName: '',
      icon: <Building2 />,
      iconBgVariant: 'primary',
    },
    {
      key: 'total-equity',
      label: <MetricHint label="Total Equity" />,
      value: formatMoney(summary.totalEquity, currency),
      valueClassName: '',
      icon: <TrendingUp />,
      iconBgVariant: 'success',
    },
    {
      key: 'monthly-cash-flow',
      label: <MetricHint label="Monthly Cash Flow" />,
      value: formatMoney(summary.monthlyCashFlow, currency),
      valueClassName: cashFlowColor(summary.monthlyCashFlow),
      icon: <DollarSign />,
      iconBgVariant: 'success',
    },
    {
      key: 'wtd-cap-rate',
      label: <MetricHint label="Wtd Cap Rate" />,
      value: formatPercent(summary.weightedCapRate),
      valueClassName: rateColor(summary.weightedCapRate, 5, 3),
      icon: <Percent />,
      iconBgVariant: 'accent',
    },
    {
      key: 'wtd-cash-on-cash',
      label: <MetricHint label="Wtd Cash-on-Cash" />,
      value: formatPercent(summary.weightedCashOnCash),
      valueClassName: rateColor(summary.weightedCashOnCash, 8, 4),
      icon: <Percent />,
      iconBgVariant: 'accent',
    },
    {
      key: 'occupancy',
      label: <MetricHint label="Occupancy" />,
      value: formatPercent(summary.portfolioOccupancy),
      valueClassName: rateColor(summary.portfolioOccupancy, 90, 75),
      icon: <Home />,
      iconBgVariant: 'info',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      {cards.map((card) => (
        <MetricCard
          key={card.key}
          label={card.label}
          value={card.value}
          icon={card.icon}
          iconBgVariant={card.iconBgVariant}
          valueClassName={card.valueClassName}
        />
      ))}
    </div>
  );
};
