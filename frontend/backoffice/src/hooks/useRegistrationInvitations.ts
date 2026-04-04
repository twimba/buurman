import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  registrationInvitationsApi,
  type CreateRegistrationInvitationRequest,
  type SendRegistrationInvitationRequest,
  type UpdateRegistrationInvitationNoteRequest,
} from '../api/registrationInvitations';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

interface ListParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
}

export const useRegistrationInvitations = (params?: ListParams) => {
  return useQuery({
    queryKey: ['registrationInvitations', params],
    queryFn: () =>
      registrationInvitationsApi
        .list(params as Record<string, unknown>)
        .then((res) => res.data),
  });
};

export const useRegistrationInvitation = (identifier: string) => {
  return useQuery({
    queryKey: ['registrationInvitations', identifier],
    queryFn: () =>
      registrationInvitationsApi.get(identifier).then((res) => res.data),
    enabled: !!identifier,
  });
};

export const useCreateRegistrationInvitation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateRegistrationInvitationRequest) =>
      registrationInvitationsApi.create(data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['registrationInvitations'] });
    },
  });
};

export const useRevokeRegistrationInvitation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (identifier: string) =>
      registrationInvitationsApi.revoke(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['registrationInvitations'] });
    },
  });
};

export const useSendRegistrationInvitation = () => {
  return useMutation({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: SendRegistrationInvitationRequest;
    }) => registrationInvitationsApi.send(identifier, data),
    onSuccess: () => {
      trackEvent(AnalyticsEvent.BO_INVITATION_SENT);
    },
  });
};

export const useUpdateRegistrationInvitationNote = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpdateRegistrationInvitationNoteRequest;
    }) =>
      registrationInvitationsApi
        .updateNote(identifier, data)
        .then((res) => res.data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['registrationInvitations'],
      });
      queryClient.invalidateQueries({
        queryKey: ['registrationInvitations', variables.identifier],
      });
    },
  });
};

export const useSuggestCode = () => {
  return useQuery({
    queryKey: ['registrationInvitations', 'suggestCode'],
    queryFn: registrationInvitationsApi.suggestCode,
    staleTime: 0,
  });
};
