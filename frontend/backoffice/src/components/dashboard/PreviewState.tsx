import { Lock } from 'lucide-react';

interface PreviewStateProps {
  cta?: string;
  docsLink?: string;
}

/**
 * Generic placeholder for panels whose data source isn't wired yet. Deliberately muted with a
 * single CTA — never fabricated numbers, so a PREVIEW panel can never be mistaken for live data.
 */
export const PreviewState = ({ cta, docsLink }: PreviewStateProps) => (
  <div className="flex flex-col items-center justify-center gap-2 py-6 text-center">
    <Lock className="h-5 w-5 text-text-muted" aria-hidden="true" />
    <p className="text-xs text-text-secondary">
      {cta ?? 'This panel is not connected yet.'}
    </p>
    {docsLink && (
      <a
        href={docsLink}
        target="_blank"
        rel="noreferrer"
        className="text-xs font-medium text-text-link hover:underline"
      >
        Learn more
      </a>
    )}
  </div>
);
