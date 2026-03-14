import { useEffect } from "react";

export type Environment = "local" | "dev" | "staging" | "production";

const BANNER_HEIGHT = 16;

const CONFIG: Record<
  Exclude<Environment, "production">,
  { label: string; color: string; prefix: string }
> = {
  local: { label: "LOCAL ENVIRONMENT", color: "#f59e0b", prefix: "[LOCAL]" },
  dev: { label: "DEV ENVIRONMENT", color: "#3b82f6", prefix: "[DEV]" },
  staging: { label: "STAGING", color: "#8b5cf6", prefix: "[STAGING]" },
};

interface EnvironmentBannerProps {
  /** Current environment. When 'production', nothing is rendered. */
  environment: Environment;
}

export function EnvironmentBanner({ environment }: EnvironmentBannerProps) {
  useEffect(() => {
    if (environment === "production") {
      return;
    }

    document.documentElement.style.setProperty(
      "--env-banner-height",
      `${BANNER_HEIGHT}px`,
    );

    const config = CONFIG[environment];
    const originalTitle = document.title;
    document.title = `${config.prefix} ${originalTitle}`;

    return () => {
      document.documentElement.style.removeProperty("--env-banner-height");
      document.title = originalTitle;
    };
  }, [environment]);

  if (environment === "production") {
    return null;
  }

  const config = CONFIG[environment];

  return (
    <div
      style={{
        height: BANNER_HEIGHT,
        backgroundColor: config.color,
        color: "#fff",
        fontSize: 9,
        fontWeight: 600,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        letterSpacing: "0.05em",
        textTransform: "uppercase",
        zIndex: 9999,
        position: "fixed",
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

EnvironmentBanner.displayName = "EnvironmentBanner";
