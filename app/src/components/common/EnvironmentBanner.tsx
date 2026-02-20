import { useEffect } from 'react';
import { env } from '../../config/env';

type Environment = 'local' | 'dev' | 'staging' | 'production';

const BANNER_HEIGHT = 16;

const CONFIG: Record<
  Exclude<Environment, 'production'>,
  { label: string; color: string; prefix: string }
> = {
  local: { label: 'LOCAL ENVIRONMENT', color: '#f59e0b', prefix: '[LOCAL]' },
  dev: { label: 'DEV ENVIRONMENT', color: '#3b82f6', prefix: '[DEV]' },
  staging: { label: 'STAGING', color: '#8b5cf6', prefix: '[STAGING]' },
};

function getEnvironment(): Environment {
  const value = env('VITE_ENVIRONMENT') || 'local';
  if (
    value === 'production' ||
    value === 'staging' ||
    value === 'dev' ||
    value === 'local'
  ) {
    return value;
  }
  return 'local';
}

export function EnvironmentBanner() {
  const environment = getEnvironment();

  useEffect(() => {
    if (environment === 'production') return;

    // Set CSS variable so fixed-position elements (sidebar) can offset themselves
    document.documentElement.style.setProperty(
      '--env-banner-height',
      `${BANNER_HEIGHT}px`
    );

    const config = CONFIG[environment];
    const originalTitle = document.title;
    document.title = `${config.prefix} ${originalTitle}`;

    return () => {
      document.documentElement.style.removeProperty('--env-banner-height');
      document.title = originalTitle;
    };
  }, [environment]);

  if (environment === 'production') return null;

  const config = CONFIG[environment];

  return (
    <div
      style={{
        height: BANNER_HEIGHT,
        backgroundColor: config.color,
        color: '#fff',
        fontSize: 9,
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
