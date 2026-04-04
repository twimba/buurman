import { cn } from '../utils/cn';

export type BadgeColorVariant =
  | 'gray'
  | 'red'
  | 'orange'
  | 'amber'
  | 'yellow'
  | 'green'
  | 'emerald'
  | 'teal'
  | 'cyan'
  | 'blue'
  | 'indigo'
  | 'violet'
  | 'purple'
  | 'rose';

interface StatusBadgeProps {
  label: string;
  color: BadgeColorVariant;
  size?: 'xs' | 'sm' | 'md';
  dot?: boolean;
  icon?: React.ReactNode;
  shape?: 'rounded' | 'pill';
  className?: string;
}

const colorStyles: Record<BadgeColorVariant, string> = {
  gray: 'bg-neutral-100 text-neutral-700',
  red: 'bg-red-50 text-red-700',
  orange: 'bg-orange-50 text-orange-700',
  amber: 'bg-amber-50 text-amber-700',
  yellow: 'bg-yellow-50 text-yellow-700',
  green: 'bg-green-50 text-green-700',
  emerald: 'bg-emerald-50 text-emerald-700',
  teal: 'bg-teal-50 text-teal-700',
  cyan: 'bg-cyan-50 text-cyan-700',
  blue: 'bg-primary-50 text-primary-700',
  indigo: 'bg-indigo-50 text-indigo-700',
  violet: 'bg-violet-50 text-violet-700',
  purple: 'bg-purple-50 text-purple-700',
  rose: 'bg-rose-50 text-rose-700',
};

const dotColors: Record<BadgeColorVariant, string> = {
  gray: 'bg-neutral-500',
  red: 'bg-red-500',
  orange: 'bg-orange-500',
  amber: 'bg-amber-500',
  yellow: 'bg-yellow-500',
  green: 'bg-green-500',
  emerald: 'bg-emerald-500',
  teal: 'bg-teal-500',
  cyan: 'bg-cyan-500',
  blue: 'bg-primary-500',
  indigo: 'bg-indigo-500',
  violet: 'bg-violet-500',
  purple: 'bg-purple-500',
  rose: 'bg-rose-500',
};

const sizeStyles = {
  xs: 'px-1.5 py-0.5 text-[11px]',
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
} as const;

export function StatusBadge({
  label,
  color,
  size = 'sm',
  dot,
  icon,
  shape = 'rounded',
  className,
}: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 font-medium',
        shape === 'pill' ? 'rounded-full' : 'rounded-sm',
        colorStyles[color],
        sizeStyles[size],
        className
      )}
    >
      {dot && (
        <span
          className={cn('h-1.5 w-1.5 shrink-0 rounded-full', dotColors[color])}
          aria-hidden="true"
        />
      )}
      {icon && (
        <span className="shrink-0" aria-hidden="true">
          {icon}
        </span>
      )}
      {label}
    </span>
  );
}

StatusBadge.displayName = 'StatusBadge';
