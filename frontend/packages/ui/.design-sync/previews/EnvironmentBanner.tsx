import { EnvironmentBanner } from '@buurman/ui';

function Frame({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative h-20 bg-surface-page border border-border-default rounded-lg overflow-hidden">
      {children}
      <div className="pt-8 px-4 text-sm text-text-secondary">
        App content sits below the banner.
      </div>
    </div>
  );
}

export function Staging() {
  return (
    <Frame>
      <EnvironmentBanner environment="staging" />
    </Frame>
  );
}

export function Development() {
  return (
    <Frame>
      <EnvironmentBanner environment="dev" />
    </Frame>
  );
}

export function Local() {
  return (
    <Frame>
      <EnvironmentBanner environment="local" />
    </Frame>
  );
}
