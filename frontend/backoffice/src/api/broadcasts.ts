import client from "./client";
import type {
  BroadcastMessage,
  CreateBroadcastMessageRequest,
  UpdateBroadcastMessageRequest,
} from "../types";

export const broadcastsApi = {
  list: () => client.get<BroadcastMessage[]>("/broadcasts"),
  create: (data: CreateBroadcastMessageRequest) =>
    client.post<BroadcastMessage>("/broadcasts", data),
  update: (identifier: string, data: UpdateBroadcastMessageRequest) =>
    client.put<BroadcastMessage>(`/broadcasts/${identifier}`, data),
  delete: (identifier: string) => client.delete(`/broadcasts/${identifier}`),
};
