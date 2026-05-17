import { Building2, TrendingUp, DollarSign, Percent, Home } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { MetricCard } from '@buurman/ui';
import type { PortfolioSummary } from '@/types/portfolio';
import { MetricHint } from '@/components/common/MetricHint';

interface PortfolioSummaryCardsProps {
  summary: PortfolioSummary;
  currency?: string;
}

function formatMoney(
  value: number | undefined,
  currencyCode: string | undefined,
  fallback: string
): string {
  if (value == null) {
    return fallback;
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

function formatPercent(value: number | undefined, fallback: string): string {
  if (value == null) {
    return fallback;
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
  const { t } = useTranslation('common');
  const na = t('dashboard.portfolio.notAvailable');

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
      label: <MetricHint label={t('dashboard.portfolio.portfolioValue')} />,
      value: formatMoney(summary.totalPortfolioValue, currency, na),
      valueClassName: '',
      icon: <Building2 />,
      iconBgVariant: 'primary',
    },
    {
      key: 'total-equity',
      label: <MetricHint label={t('dashboard.portfolio.totalEquity')} />,
      value: formatMoney(summary.totalEquity, currency, na),
      valueClassName: '',
      icon: <TrendingUp />,
      iconBgVariant: 'success',
    },
    {
      key: 'monthly-cash-flow',
      label: (
        <MetricHint
          label={t('dashboard.portfolio.monthlyCashFlow')}
          hintKey="metricHints.monthlyCF"
        />
      ),
      value: formatMoney(summary.monthlyCashFlow, currency, na),
      valueClassName: cashFlowColor(summary.monthlyCashFlow),
      icon: <DollarSign />,
      iconBgVariant: 'success',
    },
    {
      key: 'wtd-cap-rate',
      label: (
        <MetricHint
          label={t('dashboard.portfolio.wtdCapRate')}
          hintKey="metricHints.wtdCapRate"
        />
      ),
      value: formatPercent(summary.weightedCapRate, na),
      valueClassName: rateColor(summary.weightedCapRate, 5, 3),
      icon: <Percent />,
      iconBgVariant: 'accent',
    },
    {
      key: 'wtd-cash-on-cash',
      label: (
        <MetricHint
          label={t('dashboard.portfolio.wtdCashOnCash')}
          hintKey="metricHints.wtdCashOnCash"
        />
      ),
      value: formatPercent(summary.weightedCashOnCash, na),
      valueClassName: rateColor(summary.weightedCashOnCash, 8, 4),
      icon: <Percent />,
      iconBgVariant: 'accent',
    },
    {
      key: 'occupancy',
      label: (
        <MetricHint
          label={t('dashboard.portfolio.occupancy')}
          hintKey="metricHints.occupancy"
        />
      ),
      value: formatPercent(summary.portfolioOccupancy, na),
      valueClassName: rateColor(summary.portfolioOccupancy, 90, 75),
      icon: <Home />,
      iconBgVariant: 'info',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
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
