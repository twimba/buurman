import {
  EnvironmentBanner as SharedEnvironmentBanner,
  type Environment,
} from "@buurman/ui";
import { env } from "../config/env";

function getEnvironment(): Environment {
  const value = env("VITE_ENVIRONMENT") || "local";
  if (
    value === "production" ||
    value === "staging" ||
    value === "dev" ||
    value === "local"
  ) {
    return value;
  }
  return "local";
}

export function EnvironmentBanner() {
  return <SharedEnvironmentBanner environment={getEnvironment()} />;
}
