import client from './client';

export interface SegmentCondition {
  attribute:
    | 'is_demo'
    | 'is_owner'
    | 'is_team'
    | 'is_user'
    | 'role'
    | 'email'
    | 'email_verified'
    | 'property_count'
    | 'member_count'
    | 'team_age_days'
    | 'contract_count'
    | 'contact_count'
    | 'photo_count'
    | 'document_count'
    | 'expense_count'
    | 'payment_count'
    | 'calendar_feed_count';
  operator:
    | 'eq'
    | 'neq'
    | 'in'
    | 'not_in'
    | 'gt'
    | 'gte'
    | 'lt'
    | 'lte'
    | 'contains'
    | 'not_contains'
    | 'starts_with'
    | 'ends_with'
    | 'regex';
  value: string;
}

export interface SegmentDetailResponse {
  key: string;
  name: string;
  description?: string;
  priority: number;
  conditions: SegmentCondition[];
  matchingTeamCount: number;
}

export interface CreateSegmentRequest {
  key: string;
  name: string;
  description?: string;
  priority?: number;
  conditions?: SegmentCondition[];
}

export interface UpdateSegmentRequest {
  name: string;
  description?: string;
  priority?: number;
  conditions?: SegmentCondition[];
}

export interface SegmentMatchesResponse {
  matchingTeamCount: number;
  matchingUserCount: number;
}

export interface MatchingTeam {
  identifier: string;
  teamName: string;
  demo: boolean;
  createdAt: string;
}

export interface MatchingUser {
  identifier: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: string;
  teamIdentifier: string;
  teamName: string;
}

export const segmentsApi = {
  list: () => client.get<SegmentDetailResponse[]>('/segments'),
  get: (key: string) => client.get<SegmentDetailResponse>(`/segments/${key}`),
  create: (data: CreateSegmentRequest) =>
    client.post<SegmentDetailResponse>('/segments', data),
  update: (key: string, data: UpdateSegmentRequest) =>
    client.put<SegmentDetailResponse>(`/segments/${key}`, data),
  delete: (key: string) => client.delete(`/segments/${key}`),
  getMatches: (key: string) =>
    client.get<SegmentMatchesResponse>(`/segments/${key}/matches`),
  getMatchingTeams: (key: string) =>
    client.get<MatchingTeam[]>(`/segments/${key}/matching-teams`),
  getMatchingUsers: (key: string) =>
    client.get<MatchingUser[]>(`/segments/${key}/matching-users`),
};
