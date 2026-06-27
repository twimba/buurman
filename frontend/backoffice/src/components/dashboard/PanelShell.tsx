import { type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { RotateCw } from 'lucide-react';
import { Skeleton } from '@buurman/ui';

import { PanelStatus } from '../../generated/models';
import { PreviewState } from './PreviewState';

interface PanelShellProps {
  title: string;
  /** Makes the title a deep link to the page that owns this panel's data. */
  deeplink?: string;
  status?: PanelStatus;
  previewCta?: string;
  docsLink?: string;
  isLoading?: boolean;
  isError?: boolean;
  /** Right-aligned header slot (e.g. a count badge). */
  headerRight?: ReactNode;
  /** When set, the error state shows a Retry button. */
  onRetry?: () => void;
  /** Visually-hidden one-line summary for screen readers (glance parity). */
  srSummary?: string;
  children: ReactNode;
  className?: string;
}

/**
 * Common chrome for every bento panel: titled header (optionally a deep link), centralised
 * loading / error / PREVIEW rendering, and the signature mission-control card surface (elevation,
 * hover-lift, focus). Individual panels only describe their live content.
 */
export const PanelShell = ({
  title,
  deeplink,
  status,
  previewCta,
  docsLink,
  isLoading,
  isError,
  headerRight,
  onRetry,
  srSummary,
  children,
  className,
}: PanelShellProps) => {
  const isPreview = status === PanelStatus.PREVIEW;
  const isDisabled = status === PanelStatus.DISABLED;
  const interactive = !isPreview && !isDisabled && !isError;

  return (
    <section
      className={`group relative flex h-full flex-col overflow-hidden rounded-xl border border-border-default bg-surface-card motion-safe:animate-mc-rise ${
        interactive ? 'mc-panel' : 'mc-panel-quiet'
      } ${isPreview || isDisabled ? 'opacity-[0.94]' : ''} ${className ?? ''}`}
    >
      <header className="flex items-center justify-between gap-2 border-b border-border-subtle px-3.5 py-2.5">
        <div className="flex min-w-0 items-center gap-2">
          {deeplink ? (
            <Link
              to={deeplink}
              className="focus-ring truncate rounded text-[13px] font-semibold tracking-[-0.006em] text-text-primary underline-offset-2 hover:text-text-link hover:underline"
            >
              {title}
            </Link>
          ) : (
            <h2 className="truncate text-[13px] font-semibold tracking-[-0.006em] text-text-primary">
              {title}
            </h2>
          )}
          {(isPreview || isDisabled) && (
            <span
              className="rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-text-muted"
              style={{ backgroundColor: 'var(--severity-info-bg)' }}
            >
              {isDisabled ? 'Off' : 'Preview'}
            </span>
          )}
        </div>
        {headerRight}
      </header>

      {srSummary && <p className="sr-only">{srSummary}</p>}

      <div className="flex-1 p-3.5">
        {isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        ) : isError ? (
          <div className="flex flex-col items-center justify-center gap-2 py-5 text-center">
            <p className="text-xs text-error-text">Failed to load.</p>
            {onRetry && (
              <button
                type="button"
                onClick={onRetry}
                className="focus-ring inline-flex items-center gap-1.5 rounded-md border border-border-default px-2.5 py-1 text-xs font-medium text-text-secondary hover:text-text-primary"
              >
                <RotateCw className="h-3.5 w-3.5" aria-hidden="true" /> Retry
              </button>
            )}
          </div>
        ) : isPreview ? (
          <PreviewState cta={previewCta} docsLink={docsLink} />
        ) : isDisabled ? (
          <p className="py-6 text-center text-xs text-text-muted">
            This panel is turned off.
          </p>
        ) : (
          children
        )}
      </div>
    </section>
  );
};
