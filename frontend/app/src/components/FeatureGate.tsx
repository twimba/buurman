import { ReactNode } from 'react';
import { useFeatureFlags } from '../context/FeatureFlagContext';

interface FeatureGateProps {
  flag: string;
  children: ReactNode;
  fallback?: ReactNode;
}

export const FeatureGate = ({
  flag,
  children,
  fallback = null,
}: FeatureGateProps) => {
  const { isEnabled } = useFeatureFlags();
  return isEnabled(flag) ? <>{children}</> : <>{fallback}</>;
};

FeatureGate.displayName = 'FeatureGate';
