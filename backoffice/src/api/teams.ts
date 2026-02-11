import client from "./client";
import type { BackofficeTeam, PageResponse } from "../types";

interface ListTeamsParams {
  page?: number;
  size?: number;
  search?: string;
}

export const teamsApi = {
  list: (params?: ListTeamsParams) =>
    client.get<PageResponse<BackofficeTeam>>("/teams", { params }),
  get: (identifier: string) =>
    client.get<BackofficeTeam>(`/teams/${identifier}`),
  update: (identifier: string, data: { name: string }) =>
    client.put<BackofficeTeam>(`/teams/${identifier}`, data),
  delete: (identifier: string) => client.delete(`/teams/${identifier}`),
};
