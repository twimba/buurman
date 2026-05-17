import {
  ReactNode,
  useEffect,
  useRef,
  useState,
  type ComponentType,
} from 'react';
import { MoreVertical } from 'lucide-react';
import { cn } from '../utils/cn';

export interface ListPageHeaderAction {
  /** Label shown inline (desktop) and in the overflow menu (mobile). */
  label: string;
  /** Optional Lucide icon component. */
  icon?: ComponentType<{ className?: string }>;
  onClick?: () => void;
  /** Render-prop slot for actions that need custom JSX (e.g. EntityExportControls). */
  render?: (compact: boolean) => ReactNode;
  disabled?: boolean;
  /** Force visibility per viewport. Default: `'both'`. */
  showOn?: 'mobile' | 'desktop' | 'both';
}

export interface ListPageHeaderProps {
  title: string;
  /** Subtitle — shown on `md+` only, hidden on phones to save vertical space. */
  subtitle?: string;
  /** Decorative icon next to the title. Hidden below `xs` (360px). */
  icon?: ComponentType<{ className?: string }>;
  /** Secondary actions: inline on `md+`, hidden on phones (folded into overflow menu). */
  actions?: ListPageHeaderAction[];
  /**
   * Single primary CTA. On phones this is the only visible action. On `md+` it
   * appears at the right of the inline-actions group.
   */
  primaryAction?: ListPageHeaderAction;
  /** Always-overflow items (never inline). */
  overflowActions?: ListPageHeaderAction[];
  /**
   * Leading element on phones (typically the mobile-nav hamburger). When omitted,
   * the title sits flush with the left edge.
   */
  mobileLeading?: ReactNode;
  className?: string;
}

/**
 * Standardised header for list pages (Properties, Contacts, Payments, ...).
 *
 * Desktop (`md+`): pixel-equivalent to the prior inline header pattern — icon +
 * title + subtitle on the left, all actions inline on the right. **The hamburger
 * `mobileLeading` slot is hidden via `md:hidden`, so the desktop visual is
 * untouched.**
 *
 * Phone (`<md`): condensed — no icon (`xs+` only), no subtitle, only the primary
 * action button visible; everything else collapses into a `⋯` overflow menu.
 */
export const ListPageHeader = ({
  title,
  subtitle,
  icon: Icon,
  actions = [],
  primaryAction,
  overflowActions = [],
  mobileLeading,
  className,
}: ListPageHeaderProps) => {
  const desktopActions = actions.filter((a) => a.showOn !== 'mobile');
  const mobileOverflow = [
    ...actions.filter((a) => a.showOn !== 'desktop'),
    ...overflowActions,
  ];

  return (
    <header
      className={cn(
        'flex items-start gap-3 mb-4 md:mb-6 min-h-touch',
        className
      )}
    >
      {mobileLeading && (
        <div className="md:hidden flex-shrink-0 pt-0.5">{mobileLeading}</div>
      )}

      {/* Title block */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-3 mb-1">
          {Icon && (
            <Icon className="hidden xs:inline-block h-6 w-6 md:h-8 md:w-8 text-primary-500 dark:text-primary-300 flex-shrink-0" />
          )}
          <h1 className="text-2xl md:text-3xl font-bold text-text-primary truncate">
            {title}
          </h1>
        </div>
        {subtitle && (
          <p className="hidden md:block text-text-secondary md:ml-11">
            {subtitle}
          </p>
        )}
      </div>

      {/* Actions */}
      <div className="flex-shrink-0 flex items-center gap-2">
        {/* Desktop: all secondary actions inline (md+). */}
        <div className="hidden md:flex items-center gap-2 flex-wrap justify-end">
          {desktopActions.map((a, i) => (
            <ActionRenderer key={i} action={a} compact={false} />
          ))}
        </div>

        {/* Primary CTA — visible on both. */}
        {primaryAction && (
          <ActionRenderer action={primaryAction} compact={false} />
        )}

        {/* Mobile overflow menu. */}
        {mobileOverflow.length > 0 && (
          <OverflowMenu actions={mobileOverflow} className="md:hidden" />
        )}
      </div>
    </header>
  );
};

ListPageHeader.displayName = 'ListPageHeader';

/* ------------------------------------------------------------------ */

interface ActionRendererProps {
  action: ListPageHeaderAction;
  compact: boolean;
}

const ActionRenderer = ({ action, compact }: ActionRendererProps) => {
  if (action.render) {
    return <>{action.render(compact)}</>;
  }
  const Icon = action.icon;
  return (
    <button
      type="button"
      onClick={action.onClick}
      disabled={action.disabled}
      aria-label={action.label}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded transition-colors',
        'min-h-touch min-w-touch',
        // Mobile: icon-only square button. Tablet+: pill with label.
        'px-3 py-2 sm:px-4',
        'bg-primary-500 text-white hover:bg-primary-600',
        'disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500'
      )}
    >
      {Icon && <Icon className="h-5 w-5" />}
      {/* Hide label below `sm` to keep the button compact next to a truncating title. */}
      <span className="hidden sm:inline">{action.label}</span>
    </button>
  );
};

interface OverflowMenuProps {
  actions: ListPageHeaderAction[];
  className?: string;
}

const OverflowMenu = ({ actions, className }: OverflowMenuProps) => {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const handle = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const esc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handle);
    document.addEventListener('keydown', esc);
    return () => {
      document.removeEventListener('mousedown', handle);
      document.removeEventListener('keydown', esc);
    };
  }, [open]);

  return (
    <div ref={ref} className={cn('relative', className)}>
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="More actions"
        onClick={() => setOpen((o) => !o)}
        className="inline-flex items-center justify-center min-h-touch min-w-touch p-2 rounded-lg hover:bg-surface-inset focus-ring"
      >
        <MoreVertical className="h-5 w-5 text-text-secondary" />
      </button>
      {open && (
        <div
          role="menu"
          className="absolute right-0 top-full mt-1 z-30 min-w-56 max-w-xs rounded-lg border border-border-default bg-surface-card shadow-lg overflow-hidden"
        >
          {actions.map((a, i) => (
            <OverflowItem key={i} action={a} onAfter={() => setOpen(false)} />
          ))}
        </div>
      )}
    </div>
  );
};

interface OverflowItemProps {
  action: ListPageHeaderAction;
  onAfter: () => void;
}

const OverflowItem = ({ action, onAfter }: OverflowItemProps) => {
  if (action.render) {
    return <div className="px-3 py-2 min-h-touch">{action.render(true)}</div>;
  }
  const Icon = action.icon;
  return (
    <button
      type="button"
      role="menuitem"
      disabled={action.disabled}
      onClick={() => {
        action.onClick?.();
        onAfter();
      }}
      className="w-full inline-flex items-center gap-3 px-4 py-3 min-h-touch text-left text-text-primary hover:bg-surface-inset disabled:opacity-50 disabled:cursor-not-allowed"
    >
      {Icon && <Icon className="h-4 w-4 text-text-secondary" />}
      <span>{action.label}</span>
    </button>
  );
};
