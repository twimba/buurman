import client from "./client";
import type { BackofficeUser, BackofficeUserDetail, PageResponse } from "../types";

interface ListUsersParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
  team?: string;
}

export const usersApi = {
  list: (params?: ListUsersParams) =>
    client.get<PageResponse<BackofficeUser>>("/users", { params }),
  get: (identifier: string) =>
    client.get<BackofficeUserDetail>(`/users/${identifier}`),
  disable: (identifier: string) => client.post(`/users/${identifier}/disable`),
  enable: (identifier: string) => client.post(`/users/${identifier}/enable`),
  resetPassword: (identifier: string) =>
    client.post(`/users/${identifier}/reset-password`),
};
