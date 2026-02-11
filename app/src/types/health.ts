export interface HealthResponse {
  status: string;
  timestamp: string;
}

export interface InfoResponse {
  version: string;
  environment: string;
  buildTime: string;
}
