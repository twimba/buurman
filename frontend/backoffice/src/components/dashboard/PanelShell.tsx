import { type ReactNode } from 'react';
import { Link } from 'react-router-dom';
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
  children: ReactNode;
  className?: string;
}

/**
 * Common chrome for every bento panel: titled header (optionally a deep link), and centralised
 * loading / error / PREVIEW rendering so individual panels only describe their live content.
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
  children,
  className,
}: PanelShellProps) => {
  const isPreview = status === PanelStatus.PREVIEW;
  const isDisabled = status === PanelStatus.DISABLED;

  return (
    <section
      className={`flex h-full flex-col rounded-lg border border-border-default bg-surface-card ${className ?? ''}`}
    >
      <header className="flex items-center justify-between gap-2 border-b border-border-subtle px-4 py-2.5">
        <div className="flex items-center gap-2 min-w-0">
          {deeplink ? (
            <Link
              to={deeplink}
              className="focus-ring truncate rounded text-sm font-semibold text-text-primary hover:text-text-link"
            >
              {title}
            </Link>
          ) : (
            <h2 className="truncate text-sm font-semibold text-text-primary">
              {title}
            </h2>
          )}
          {(isPreview || isDisabled) && (
            <span className="rounded bg-neutral-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-text-muted">
              {isDisabled ? 'Off' : 'Preview'}
            </span>
          )}
        </div>
        {headerRight}
      </header>

      <div className="flex-1 p-4">
        {isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        ) : isError ? (
          <p className="py-4 text-center text-xs text-error-text">
            Failed to load.
          </p>
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
