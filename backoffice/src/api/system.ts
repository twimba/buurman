import client from "./client";
import type { SystemInfoResponse } from "../types";

export const systemApi = {
  info: () => client.get<SystemInfoResponse>("/system/info"),
};
