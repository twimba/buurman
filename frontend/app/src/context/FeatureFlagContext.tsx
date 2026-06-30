import { createContext, useContext, ReactNode, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getFeatureFlags } from '../generated/api/feature-flags/feature-flags';
import type { FeatureFlagState } from '../generated/models';
import { useAuth } from './AuthContext';

type FeatureFlagsResponse = Record<string, FeatureFlagState>;

interface FeatureFlagContextType {
  flags: FeatureFlagsResponse;
  isEnabled: (key: string) => boolean;
  getValue: (key: string) => unknown;
  isLoading: boolean;
}

const FeatureFlagContext = createContext<FeatureFlagContextType | undefined>(
  undefined
);

export const FeatureFlagProvider = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated, token } = useAuth();

  const { data: flags = {}, isLoading } = useQuery({
    queryKey: ['feature-flags'],
    queryFn: () => getFeatureFlags(),
    staleTime: 60_000,
    enabled: isAuthenticated && !!token,
  });

  const isEnabled = useCallback(
    (key: string): boolean => {
      return flags[key]?.enabled === true;
    },
    [flags]
  );

  const getValue = useCallback(
    (key: string): unknown => {
      return flags[key]?.value ?? null;
    },
    [flags]
  );

  return (
    <FeatureFlagContext.Provider
      value={{ flags, isEnabled, getValue, isLoading }}
    >
      {children}
    </FeatureFlagContext.Provider>
  );
};

export const useFeatureFlags = () => {
  const context = useContext(FeatureFlagContext);
  if (context === undefined) {
    throw new Error(
      'useFeatureFlags must be used within a FeatureFlagProvider'
    );
  }
  return context;
};
