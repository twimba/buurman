import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { segmentsApi } from "../api/segments";
import type {
  CreateSegmentRequest,
  UpdateSegmentRequest,
} from "../api/segments";

export const useSegmentsList = () => {
  return useQuery({
    queryKey: ["segments"],
    queryFn: () => segmentsApi.list().then((res) => res.data),
  });
};

export const useSegmentDetail = (key: string | undefined) => {
  return useQuery({
    queryKey: ["segments", key],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => segmentsApi.get(key!).then((res) => res.data),
    enabled: !!key,
  });
};

export const useSegmentMatches = (key: string | undefined) => {
  return useQuery({
    queryKey: ["segments", key, "matches"],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => segmentsApi.getMatches(key!).then((res) => res.data),
    enabled: !!key,
  });
};

export const useSegmentMatchingTeams = (
  key: string | undefined,
  enabled: boolean,
) => {
  return useQuery({
    queryKey: ["segments", key, "matching-teams"],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => segmentsApi.getMatchingTeams(key!).then((res) => res.data),
    enabled: !!key && enabled,
  });
};

export const useSegmentMatchingUsers = (
  key: string | undefined,
  enabled: boolean,
) => {
  return useQuery({
    queryKey: ["segments", key, "matching-users"],
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion -- guarded by enabled
    queryFn: () => segmentsApi.getMatchingUsers(key!).then((res) => res.data),
    enabled: !!key && enabled,
  });
};

export const useCreateSegment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateSegmentRequest) =>
      segmentsApi.create(data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["segments"] });
    },
  });
};

export const useUpdateSegment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, data }: { key: string; data: UpdateSegmentRequest }) =>
      segmentsApi.update(key, data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["segments"] });
    },
  });
};

export const useDeleteSegment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (key: string) => segmentsApi.delete(key),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["segments"] });
    },
  });
};
