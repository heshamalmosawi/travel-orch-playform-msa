export interface ReportCreateRequest {
  managerId: number;
  reason?: string;
}

export interface ReportResponse {
  id: number;
  managerId: number;
  reporterId: number | null;
  reporterUsername: string | null;
  reason: string | null;
  createdAt: string;
}
