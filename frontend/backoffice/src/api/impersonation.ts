import client from './client';
import type { PageResponse } from '../types';

export interface ImpersonationSession {
  identifier: string;
  adminEmail: string;
  adminName: string;
  targetUserIdentifier: string;
  targetUserEmail: string;
  targetTeamIdentifier: string;
  mode: string;
  reason: string;
  status: string;
  createdAt: string;
  activatedAt?: string;
  expiresAt: string;
  endedAt?: string;
  endReason?: string;
}

export interface ListImpersonationSessionsParams {
  adminEmail?: string;
  targetUserEmail?: string;
  teamIdentifier?: string;
  status?: string;
  mode?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}

export const impersonationApi = {
  listSessions: (params?: ListImpersonationSessionsParams) =>
    client.get<PageResponse<ImpersonationSession>>('/impersonation/sessions', {
      params,
    }),
};
