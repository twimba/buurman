import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listRegistrationInvitations,
  get as getRegistrationInvitation,
  create as createRegistrationInvitation,
  revoke as revokeRegistrationInvitation,
  send as sendRegistrationInvitation,
  suggestCode,
  updateNote,
} from '../generated/api/backoffice-registration-invitations/backoffice-registration-invitations';
import type {
  CreateRegistrationInvitationRequest,
  ListRegistrationInvitationsParams,
  SendRegistrationInvitationRequest,
  UpdateRegistrationInvitationNoteRequest,
} from '../generated/models';
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
      listRegistrationInvitations(params as ListRegistrationInvitationsParams),
  });
};

export const useRegistrationInvitation = (identifier: string) => {
  return useQuery({
    queryKey: ['registrationInvitations', identifier],
    queryFn: () => getRegistrationInvitation(identifier),
    enabled: !!identifier,
  });
};

export const useCreateRegistrationInvitation = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateRegistrationInvitationRequest) =>
      createRegistrationInvitation(data),
    errorTitle: "Couldn't create invitation",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['registrationInvitations'] });
    },
  });
};

export const useRevokeRegistrationInvitation = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (identifier: string) =>
      revokeRegistrationInvitation(identifier),
    errorTitle: "Couldn't revoke invitation",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['registrationInvitations'] });
    },
  });
};

export const useSendRegistrationInvitation = () => {
  return useMutationWithToast({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: SendRegistrationInvitationRequest;
    }) => sendRegistrationInvitation(identifier, data),
    errorTitle: "Couldn't send invitation",
    onSuccess: () => {
      trackEvent(AnalyticsEvent.BO_INVITATION_SENT);
    },
  });
};

export const useUpdateRegistrationInvitationNote = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpdateRegistrationInvitationNoteRequest;
    }) => updateNote(identifier, data),
    errorTitle: "Couldn't update note",
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
    queryFn: () => suggestCode().then((res) => res.code),
    staleTime: 0,
  });
};
