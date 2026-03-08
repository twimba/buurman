import { Building2, TrendingUp, DollarSign, Percent, Home } from 'lucide-react';
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

function rateColor(
  value: number | undefined,
  greenThreshold: number,
  yellowThreshold: number
): string {
  if (value == null) {
    return 'text-[#6b7194] dark:text-[#8b90a8]';
  }
  if (value >= greenThreshold) {
    return 'text-green-600 dark:text-green-400';
  }
  if (value >= yellowThreshold) {
    return 'text-yellow-600 dark:text-yellow-400';
  }
  return 'text-red-600 dark:text-red-400';
}

function cashFlowColor(value: number | undefined): string {
  if (value == null) {
    return 'text-[#6b7194] dark:text-[#8b90a8]';
  }
  return value >= 0
    ? 'text-green-600 dark:text-green-400'
    : 'text-red-600 dark:text-red-400';
}

export const PortfolioSummaryCards = ({
  summary,
  currency,
}: PortfolioSummaryCardsProps) => {
  const cards = [
    {
      label: 'Portfolio Value',
      value: formatMoney(summary.totalPortfolioValue, currency),
      color: 'text-[#1a1d2e] dark:text-[#eef0f6]',
      icon: <Building2 className="h-5 w-5 text-blue-500 dark:text-blue-400" />,
      bg: 'bg-blue-100 dark:bg-blue-900/30',
    },
    {
      label: 'Total Equity',
      value: formatMoney(summary.totalEquity, currency),
      color: 'text-[#1a1d2e] dark:text-[#eef0f6]',
      icon: (
        <TrendingUp className="h-5 w-5 text-emerald-500 dark:text-emerald-400" />
      ),
      bg: 'bg-emerald-100 dark:bg-emerald-900/30',
    },
    {
      label: 'Monthly Cash Flow',
      value: formatMoney(summary.monthlyCashFlow, currency),
      color: cashFlowColor(summary.monthlyCashFlow),
      icon: (
        <DollarSign className="h-5 w-5 text-green-500 dark:text-green-400" />
      ),
      bg: 'bg-green-100 dark:bg-green-900/30',
    },
    {
      label: 'Wtd Cap Rate',
      value: formatPercent(summary.weightedCapRate),
      color: rateColor(summary.weightedCapRate, 5, 3),
      icon: (
        <Percent className="h-5 w-5 text-purple-500 dark:text-purple-400" />
      ),
      bg: 'bg-purple-100 dark:bg-purple-900/30',
    },
    {
      label: 'Wtd Cash-on-Cash',
      value: formatPercent(summary.weightedCashOnCash),
      color: rateColor(summary.weightedCashOnCash, 8, 4),
      icon: (
        <Percent className="h-5 w-5 text-indigo-500 dark:text-indigo-400" />
      ),
      bg: 'bg-indigo-100 dark:bg-indigo-900/30',
    },
    {
      label: 'Occupancy',
      value: formatPercent(summary.portfolioOccupancy),
      color: rateColor(summary.portfolioOccupancy, 90, 75),
      icon: <Home className="h-5 w-5 text-cyan-500 dark:text-cyan-400" />,
      bg: 'bg-cyan-100 dark:bg-cyan-900/30',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {cards.map((card) => (
        <div
          key={card.label}
          className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-5 border border-[#edf0f7] dark:border-[#2a2e3f]"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8]">
              <MetricHint label={card.label} />
            </span>
            <div className={`p-2 rounded-lg ${card.bg}`}>{card.icon}</div>
          </div>
          <div className={`text-2xl font-bold ${card.color}`}>{card.value}</div>
        </div>
      ))}
    </div>
  );
};
