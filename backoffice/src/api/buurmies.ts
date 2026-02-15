import client from "./client";
import type { Buurmy, CreateBuurmyRequest, PageResponse } from "../types";

interface ListBuurmiesParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  direction?: string;
}

export const buurmiesApi = {
  list: (params?: ListBuurmiesParams) =>
    client.get<PageResponse<Buurmy>>("/buurmies", { params }),
  get: (keycloakId: string) => client.get<Buurmy>(`/buurmies/${keycloakId}`),
  create: (data: CreateBuurmyRequest) => client.post<Buurmy>("/buurmies", data),
  disable: (keycloakId: string) =>
    client.post(`/buurmies/${keycloakId}/disable`),
  enable: (keycloakId: string) => client.post(`/buurmies/${keycloakId}/enable`),
  delete: (keycloakId: string) => client.delete(`/buurmies/${keycloakId}`),
  forcePasswordUpdate: (keycloakId: string) =>
    client.post(`/buurmies/${keycloakId}/force-password-update`),
  forceProfileUpdate: (keycloakId: string) =>
    client.post(`/buurmies/${keycloakId}/force-profile-update`),
};
