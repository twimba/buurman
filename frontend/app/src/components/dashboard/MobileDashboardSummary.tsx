import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowRight,
  Building2,
  DollarSign,
  Home,
  Receipt,
  TrendingUp,
} from 'lucide-react';
import { formatMoneyCompact } from '@/utils/formatMoney';
import { MobileMenuButton } from '@/components/MobileMenuButton';
import type { DashboardStats } from '@/api/dashboard';

interface MobileDashboardSummaryProps {
  stats?: DashboardStats;
  overdueCount: number;
  pendingExtensionsCount: number;
}

/**
 * Phone-only top-of-dashboard composition. Hidden md+ so the desktop dashboard
 * remains pixel-equivalent. Three blocks:
 *
 * 1. Alerts strip — overdue + extensions counts at-a-glance, tap-to-navigate.
 *    Only rendered when there's something to alert about.
 * 2. Hero KPI — Monthly Income, the most-asked-about number on the dashboard.
 *    Uses formatMoneyCompact so big numbers fit (€30K, €1.2M).
 * 3. KPI rail — horizontal-scrolling snap row with 3 secondary KPIs.
 */
export const MobileDashboardSummary = ({
  stats,
  overdueCount,
  pendingExtensionsCount,
}: MobileDashboardSummaryProps) => {
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  if (!stats) {
    return null;
  }

  const hasAlerts = overdueCount > 0 || pendingExtensionsCount > 0;
  const income = stats.monthlyIncome?.amount ?? 0;
  const incomeCurrency = stats.monthlyIncome?.currency ?? 'EUR';

  return (
    <div className="md:hidden -mt-2 space-y-4">
      {/* Top bar — anchors the mobile-nav hamburger inside the page so the
          Sidebar's floating fallback hides (see MobileNavContext). */}
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

      {/* 2. Hero KPI — Monthly Income */}
      <section
        className="rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 dark:from-primary-700 dark:to-primary-900 text-white p-5 shadow-sm"
        aria-labelledby="hero-kpi-label"
      >
        <div className="flex items-center gap-2 opacity-90">
          <DollarSign className="h-4 w-4" />
          <span
            id="hero-kpi-label"
            className="text-xs font-medium uppercase tracking-wider"
          >
            {t('dashboard.monthlyIncome')}
          </span>
        </div>
        <div className="mt-2 text-4xl font-bold tabular-nums">
          {formatMoneyCompact(income, incomeCurrency)}
        </div>
        <div className="mt-1 text-sm opacity-80">
          {t('dashboard.expectedRevenue')}
        </div>
      </section>

      {/* 3. KPI rail — horizontal snap scroll */}
      <div
        className="flex gap-3 overflow-x-auto snap-x snap-mandatory -mx-4 px-4 scroll-pl-4"
        role="list"
      >
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
    </div>
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

MobileDashboardSummary.displayName = 'MobileDashboardSummary';
