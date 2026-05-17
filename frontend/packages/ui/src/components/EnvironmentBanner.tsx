import { useEffect, useRef } from 'react';

export type Environment = 'local' | 'dev' | 'staging' | 'production';

const BANNER_CONTENT_HEIGHT = 16;

/**
 * AA-compliant background colors (≥4.5:1 contrast with white text).
 * Previous values (#f59e0b amber-500, etc.) only hit ~3.0:1 — fails WCAG AA.
 */
const CONFIG: Record<
  Exclude<Environment, 'production'>,
  { label: string; color: string; prefix: string }
> = {
  local: { label: 'LOCAL ENVIRONMENT', color: '#92400e', prefix: '[LOCAL]' }, // amber-800 — 6.0:1
  dev: { label: 'DEV ENVIRONMENT', color: '#1d4ed8', prefix: '[DEV]' }, //   blue-700  — 6.4:1
  staging: { label: 'STAGING', color: '#6d28d9', prefix: '[STAGING]' }, //    violet-700 — 6.9:1
};

interface EnvironmentBannerProps {
  /** Current environment. When 'production', nothing is rendered. */
  environment: Environment;
}

export function EnvironmentBanner({ environment }: EnvironmentBannerProps) {
  const ref = useRef<HTMLDivElement | null>(null);

  // Measure rendered height (including safe-area inset on notched devices) and publish it
  // as a CSS variable so Layout / Sidebar can offset their fixed content correctly.
  useEffect(() => {
    if (environment === 'production') {
      return;
    }

    const config = CONFIG[environment];
    const originalTitle = document.title;
    document.title = `${config.prefix} ${originalTitle}`;

    const publishHeight = () => {
      if (!ref.current) {
        return;
      }
      const h = ref.current.getBoundingClientRect().height;
      document.documentElement.style.setProperty(
        '--env-banner-height',
        `${Math.round(h)}px`
      );
    };

    publishHeight();
    const ro = new ResizeObserver(publishHeight);
    if (ref.current) {
      ro.observe(ref.current);
    }
    window.addEventListener('resize', publishHeight);

    return () => {
      ro.disconnect();
      window.removeEventListener('resize', publishHeight);
      document.documentElement.style.removeProperty('--env-banner-height');
      document.title = originalTitle;
    };
  }, [environment]);

  if (environment === 'production') {
    return null;
  }

  const config = CONFIG[environment];

  return (
    <div
      ref={ref}
      role="status"
      aria-label={config.label}
      style={{
        minHeight: BANNER_CONTENT_HEIGHT,
        // Extend under the notch on iOS while keeping text below the status bar.
        paddingTop: 'env(safe-area-inset-top, 0px)',
        paddingLeft: 'env(safe-area-inset-left, 0px)',
        paddingRight: 'env(safe-area-inset-right, 0px)',
        backgroundColor: config.color,
        color: '#fff',
        fontSize: 10,
        fontWeight: 600,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        letterSpacing: '0.05em',
        textTransform: 'uppercase',
        zIndex: 9999,
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        flexShrink: 0,
      }}
    >
      {config.label}
    </div>
  );
}

EnvironmentBanner.displayName = 'EnvironmentBanner';
