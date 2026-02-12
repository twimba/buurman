import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as settingsApi from "../api/settings";

export const usePhonePolicy = () => {
  return useQuery({
    queryKey: ["phone-policy"],
    queryFn: settingsApi.getPhonePolicy,
  });
};

export const usePhonePolicyMetadata = () => {
  return useQuery({
    queryKey: ["phone-policy-metadata"],
    queryFn: settingsApi.getPhonePolicyMetadata,
    staleTime: Infinity, // static data, never changes
  });
};

export const useUpdatePhonePolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: settingsApi.updatePhonePolicy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["phone-policy"] });
    },
  });
};
