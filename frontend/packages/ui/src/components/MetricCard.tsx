import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { cn } from '../utils/cn';

interface MetricCardProps {
  label: React.ReactNode;
  value: string;
  icon?: React.ReactNode;
  iconBgVariant?:
    'primary' | 'accent' | 'success' | 'warning' | 'error' | 'info';
  trend?: {
    direction: 'up' | 'down' | 'flat';
    label: string;
    sentiment?: 'positive' | 'negative' | 'neutral';
  };
  /** Override the value text color (e.g. for conditional red/green) */
  valueClassName?: string;
  subtitle?: string;
  size?: 'sm' | 'md' | 'lg';
  alert?: boolean;
  alertSeverity?: 'warning' | 'critical';
  onClick?: () => void;
  href?: string;
  hint?: string;
  className?: string;
}

const iconBgStyles = {
  primary: 'bg-primary-50 text-primary-500',
  accent: 'bg-accent-50 text-accent-600',
  success: 'bg-success-bg text-success',
  warning: 'bg-warning-bg text-warning',
  error: 'bg-error-bg text-error',
  info: 'bg-info-bg text-info',
} as const;

const trendIcons = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
} as const;

const sentimentStyles = {
  positive: 'text-success',
  negative: 'text-error',
  neutral: 'text-text-muted',
} as const;

export function MetricCard({
  label,
  value,
  icon,
  iconBgVariant = 'primary',
  trend,
  valueClassName,
  subtitle,
  size = 'md',
  alert,
  alertSeverity = 'warning',
  onClick,
  href,
  hint,
  className,
}: MetricCardProps) {
  const isInteractive = !!(onClick || href);

  const iconSize = size === 'sm' ? 'h-10 w-10' : 'h-12 w-12';
  const iconInner =
    size === 'sm' ? '[&>svg]:h-5 [&>svg]:w-5' : '[&>svg]:h-6 [&>svg]:w-6';
  const labelSize = size === 'sm' ? 'text-xs' : 'text-[13px]';
  const valueSize =
    size === 'sm' ? 'text-xl' : size === 'lg' ? 'text-[32px]' : 'text-[28px]';

  const content = (
    <div className="flex gap-4">
      {icon && (
        <div
          className={cn(
            'flex items-center justify-center rounded-lg shrink-0',
            iconSize,
            iconInner,
            iconBgStyles[iconBgVariant]
          )}
        >
          {icon}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className={cn('font-medium text-text-secondary', labelSize)}>
          {label}
        </p>
        <p
          className={cn(
            'mt-1 font-bold tracking-tight leading-tight tabular-nums text-text-primary',
            valueSize,
            valueClassName
          )}
        >
          {value}
        </p>

        {(trend || subtitle) && (
          <div className="mt-1.5 flex items-center gap-2">
            {trend &&
              (() => {
                const TrendIcon = trendIcons[trend.direction];
                const sentiment = trend.sentiment || 'neutral';
                return (
                  <span
                    className={cn(
                      'inline-flex items-center gap-1 text-xs font-medium',
                      sentimentStyles[sentiment]
                    )}
                  >
                    <TrendIcon className="h-3.5 w-3.5" />
                    {trend.label}
                  </span>
                );
              })()}
            {subtitle && (
              <span className="text-xs text-text-muted">{subtitle}</span>
            )}
          </div>
        )}

        {hint && <p className="mt-1 text-xs text-text-muted">{hint}</p>}
      </div>
    </div>
  );

  const cardClasses = cn(
    'rounded-lg border border-border-default bg-surface-card p-6 shadow-sm',
    alert &&
      alertSeverity === 'warning' &&
      'border-warning-border bg-warning-bg',
    alert && alertSeverity === 'critical' && 'border-error-border bg-error-bg',
    isInteractive && 'cursor-pointer transition-shadow hover:shadow-md',
    className
  );

  if (href) {
    return (
      <a href={href} className={cn(cardClasses, 'block no-underline')}>
        {content}
      </a>
    );
  }

  if (onClick) {
    return (
      <button onClick={onClick} className={cn(cardClasses, 'w-full text-left')}>
        {content}
      </button>
    );
  }

  return <div className={cardClasses}>{content}</div>;
}

MetricCard.displayName = 'MetricCard';
