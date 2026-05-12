export interface Report {
  id: string;
  title: string;
  format: ReportFormat;
  status: ReportStatus;
  projectId: string;
  generatedBy: string;
  filePath: string;
  fileSizeBytes: number;
  createdAt: string;
}

export type ReportFormat = 'PDF' | 'CSV' | 'JSON';
export type ReportStatus = 'GENERATING' | 'READY' | 'FAILED' | 'EXPIRED';

export interface GenerateReportRequest {
  projectId: string;
  title: string;
  format: ReportFormat;
  includeSimulationIds?: string[];
}
