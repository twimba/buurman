import { useQuery } from "@tanstack/react-query";
import { featureFlagsApi } from "../api/featureFlags";

export const useGlobalFeatureFlags = () => {
  return useQuery({
    queryKey: ["feature-flags", "global"],
    queryFn: () => featureFlagsApi.getGlobal().then((res) => res.data),
  });
};

export const useUserFeatureFlags = (userIdentifier: string | null) => {
  return useQuery({
    queryKey: ["feature-flags", "user", userIdentifier],
    queryFn: () =>
      featureFlagsApi.getForUser(userIdentifier!).then((res) => res.data),
    enabled: !!userIdentifier,
  });
};
