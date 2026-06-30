import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowRight,
  Building2,
  DollarSign,
  Home,
  Receipt,
  TrendingDown,
  TrendingUp,
  Users,
} from 'lucide-react';
import { formatMoney, formatMoneyCompact } from '@/utils/formatMoney';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import { MetricHint } from '@/components/common/MetricHint';
import { usePortfolioDashboard } from '@/hooks/usePortfolioDashboard';
import { useLongPress } from '@buurman/ui';
import type { DashboardStatsResponse as DashboardStats } from '@/generated/models';

interface DashboardKpiStripProps {
  stats?: DashboardStats;
  overdueCount: number;
  pendingExtensionsCount: number;
}

/**
 * Single owner of the dashboard's at-a-glance KPI presentation. Renders
 * two variants:
 *
 * - **Phone (`<md`):** alerts strip + sign-colored cash-flow hero (P1-10)
 *   + horizontal KPI rail (P0-9) + "Portfolio analytics" CTA (P1-21).
 * - **Desktop (`md+`):** the existing 4-card grid (Total Properties /
 *   Occupied / Occupancy Rate / Monthly Income).
 *
 * Co-locating both layouts replaces the previous fork (separate
 * `MobileDashboardSummary` component + inline `hidden md:grid` block in
 * DashboardPage) — one place for value formatting, one place for design
 * decisions. Both variants are gated by `md:` so desktop visuals stay
 * pixel-identical to before; the consolidation is structural, not visual.
 */
export const DashboardKpiStrip = ({
  stats,
  overdueCount,
  pendingExtensionsCount,
}: DashboardKpiStripProps) => {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  // 6-month default mirrors PortfolioDashboard's initial period. The query
  // is cache-shared via react-query so this doesn't cost a second roundtrip
  // when PortfolioDashboard also mounts (md+).
  const { data: portfolio } = usePortfolioDashboard(6);

  // Long-press the phone hero KPI to surface the full-precision number
  // (the compact value hides accounting-grade digits). 1.6s visible window.
  // Hooks must run unconditionally — declared above the early return.
  const [showPrecise, setShowPrecise] = useState(false);
  const heroLongPress = useLongPress<HTMLElement>(() => {
    setShowPrecise(true);
    window.setTimeout(() => setShowPrecise(false), 1600);
  });

  if (!stats) {
    return null;
  }

  const hasAlerts = overdueCount > 0 || pendingExtensionsCount > 0;
  const income = stats.monthlyIncome?.amount ?? 0;
  const incomeCurrency = stats.monthlyIncome?.currency ?? 'EUR';
  const cashFlow = portfolio?.summary.monthlyCashFlow;
  const cashFlowCurrency = portfolio?.currency ?? incomeCurrency;
  const cashFlowNegative = cashFlow != null && cashFlow < 0;

  return (
    <>
      {/* ── Phone variant ── */}
      <div className="md:hidden -mt-2 space-y-4">
        {/* Top bar — anchors the mobile-nav hamburger inside the page so
            the Sidebar's floating fallback hides (see MobileNavContext). */}
        <div className="flex items-center -mb-2 -mt-1">
          <MobileMenuButton />
        </div>

        {/* 1. Alerts strip */}
        {hasAlerts && (
          <button
            type="button"
            onClick={() => {
              if (overdueCount > 0) {
                navigate('/payments?status=OVERDUE');
              } else {
                navigate('/contracts');
              }
            }}
            className="w-full flex items-center gap-3 px-4 py-3 min-h-touch rounded-lg bg-warning-bg border border-warning-border text-warning-text focus-ring"
          >
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <div className="flex-1 text-left text-sm">
              <span className="font-semibold">
                {[
                  overdueCount > 0 &&
                    t('dashboard.overdueCount', {
                      count: overdueCount,
                      defaultValue: `${overdueCount} overdue`,
                    }),
                  pendingExtensionsCount > 0 &&
                    t('dashboard.extensionsCount', {
                      count: pendingExtensionsCount,
                      defaultValue: `${pendingExtensionsCount} extensions`,
                    }),
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
            </div>
            <ArrowRight className="h-4 w-4 flex-shrink-0" />
          </button>
        )}

        {/* 2. Hero KPI — Monthly Cash Flow (P1-10). Sign-colored; falls
            back to income while portfolio query is loading. */}
        <section
          {...heroLongPress}
          className={`relative select-none rounded-xl text-white p-5 shadow-sm bg-gradient-to-br ${
            cashFlow == null
              ? 'from-primary-500 to-primary-700 dark:from-primary-700 dark:to-primary-900'
              : cashFlowNegative
                ? 'from-error to-error-bg dark:from-error dark:to-error-bg'
                : 'from-success to-success-text dark:from-success-text dark:to-success'
          }`}
          aria-labelledby="hero-kpi-label"
        >
          <div className="flex items-center gap-2 opacity-90">
            {cashFlow == null ? (
              <DollarSign className="h-4 w-4" />
            ) : cashFlowNegative ? (
              <TrendingDown className="h-4 w-4" />
            ) : (
              <TrendingUp className="h-4 w-4" />
            )}
            <span
              id="hero-kpi-label"
              className="text-xs font-medium uppercase tracking-wider"
            >
              {cashFlow == null
                ? t('dashboard.monthlyIncome')
                : t('dashboard.monthlyCashFlow', {
                    defaultValue: 'Monthly cash flow',
                  })}
            </span>
          </div>
          <div className="mt-2 text-4xl font-bold tabular-nums">
            {cashFlow == null
              ? formatMoneyCompact(income, incomeCurrency)
              : formatMoneyCompact(cashFlow, cashFlowCurrency)}
          </div>
          <div className="mt-1 text-sm opacity-80">
            {cashFlow == null
              ? t('dashboard.expectedRevenue')
              : t('dashboard.incomeMinusExpenses', {
                  defaultValue: 'Income minus expenses',
                })}
          </div>
          {showPrecise && (
            <div
              role="tooltip"
              aria-live="polite"
              className="absolute inset-x-3 bottom-3 bg-black/70 text-white rounded-md px-3 py-2 text-sm tabular-nums backdrop-blur-sm shadow-lg pointer-events-none"
            >
              {cashFlow == null
                ? formatMoney(income, incomeCurrency)
                : formatMoney(cashFlow, cashFlowCurrency)}
            </div>
          )}
        </section>

        {/* 3. KPI rail — horizontal snap scroll w/ fade-right mask (P2-8/9). */}
        <div
          className="flex gap-3 overflow-x-auto snap-x snap-proximity -mx-4 px-4 scroll-pl-4 [mask-image:linear-gradient(to_right,black_0,black_calc(100%-2rem),transparent_100%)]"
          role="list"
        >
          {cashFlow != null && (
            <RailTile
              icon={DollarSign}
              label={t('dashboard.monthlyIncome')}
              value={formatMoneyCompact(income, incomeCurrency)}
              sub={t('dashboard.expectedRevenue')}
            />
          )}
          <RailTile
            icon={Building2}
            label={t('dashboard.totalProperties')}
            value={String(stats.totalProperties ?? 0)}
            sub={t('dashboard.activeProperties')}
          />
          <RailTile
            icon={Home}
            label={t('dashboard.occupied')}
            value={`${stats.occupancyRate?.toFixed(1) ?? 0}%`}
            sub={t('dashboard.occupancySublabel', {
              occupied: stats.occupiedUnits ?? 0,
              total: stats.totalProperties ?? 0,
              defaultValue: `${stats.occupiedUnits ?? 0} of ${stats.totalProperties ?? 0}`,
            })}
          />
          <RailTile
            icon={Receipt}
            label={t('dashboard.vacant')}
            value={String(stats.vacantUnits ?? 0)}
            sub={t('dashboard.vacantSublabel', {
              count: stats.vacantUnits ?? 0,
              defaultValue: 'Available to rent',
            })}
          />
          <RailTile
            icon={TrendingUp}
            label={t('dashboard.maintenance', { defaultValue: 'Maintenance' })}
            value={String(stats.maintenanceUnits ?? 0)}
          />
        </div>

        {/* 4. Portfolio analytics CTA (P1-21). */}
        <button
          type="button"
          onClick={() => navigate('/reports')}
          className="w-full flex items-center justify-between gap-2 px-4 py-3 min-h-touch rounded-lg border border-border-default bg-surface-card text-text-secondary focus-ring"
        >
          <span className="inline-flex items-center gap-2 text-sm font-medium">
            <TrendingUp className="h-4 w-4 text-primary-500" />
            {t('dashboard.portfolioAnalyticsCta', {
              defaultValue: 'Portfolio analytics',
            })}
          </span>
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>

      {/* ── Desktop variant — 4-card grid (preserved bit-for-bit) ── */}
      <div className="hidden md:grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Properties */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-info-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              {t('dashboard.totalProperties')}
            </h3>
            <div className="p-2 bg-primary-100 rounded-lg">
              <Home className="h-5 w-5 text-primary-500" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.totalProperties ?? 0}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {t('dashboard.activeProperties')}
          </div>
        </div>

        {/* Occupied Units */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-success-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              {t('dashboard.occupied')}
            </h3>
            <div className="p-2 bg-success-bg rounded-lg">
              <Users className="h-5 w-5 text-success-text" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.occupiedUnits ?? 0}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {stats?.selfOccupiedUnits
              ? `${stats.selfOccupiedUnits} ${t('dashboard.selfOccupied')},`
              : ''}
            {stats?.vacantUnits ?? 0} {t('dashboard.vacant')},{' '}
            {stats?.maintenanceUnits ?? 0} {t('dashboard.inMaintenance')}
          </div>
        </div>

        {/* Occupancy Rate */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-primary-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              <MetricHint label={t('dashboard.occupancyRate')} />
            </h3>
            <div className="p-2 bg-primary-50 rounded-lg">
              <TrendingUp className="h-5 w-5 text-primary-700" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.occupancyRate?.toFixed(1) ?? 0}%
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {t('dashboard.currentOccupancy')}
          </div>
        </div>

        {/* Monthly Income */}
        <div className="bg-surface-card rounded-lg shadow-sm p-6 hover:shadow-lg transition-shadow duration-300 border border-border-default hover:border-success-border">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-text-secondary">
              {t('dashboard.monthlyIncome')}
            </h3>
            <div className="p-2 bg-success-bg rounded-lg">
              <DollarSign className="h-5 w-5 text-success-text" />
            </div>
          </div>
          <div className="text-2xl md:text-3xl font-bold text-text-primary">
            {stats?.monthlyIncome?.currency
              ? new Intl.NumberFormat('nl-NL', {
                  style: 'currency',
                  currency: stats.monthlyIncome.currency,
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                }).format(stats?.monthlyIncome?.amount ?? 0)
              : (stats?.monthlyIncome?.amount ?? 0).toLocaleString('nl-NL', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                })}
          </div>
          <div className="text-sm text-text-secondary mt-2">
            {t('dashboard.expectedRevenue')}
          </div>
        </div>
      </div>
    </>
  );
};

interface RailTileProps {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  sub?: string;
}

const RailTile = ({ icon: Icon, label, value, sub }: RailTileProps) => (
  <div
    role="listitem"
    className="snap-start flex-shrink-0 w-[78%] xs:w-[60%] sm:w-[44%] bg-surface-card rounded-lg border border-border-default p-4"
  >
    <div className="flex items-center gap-2 text-text-secondary text-xs font-medium uppercase tracking-wider">
      <Icon className="h-4 w-4 flex-shrink-0" />
      <span className="leading-tight">{label}</span>
    </div>
    <div className="mt-2 text-2xl font-bold text-text-primary tabular-nums">
      {value}
    </div>
    {sub && (
      <div className="mt-1 text-xs text-text-secondary truncate">{sub}</div>
    )}
  </div>
);

DashboardKpiStrip.displayName = 'DashboardKpiStrip';
