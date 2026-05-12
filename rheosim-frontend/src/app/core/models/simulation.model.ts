export interface SimulationJob {
  id: string;
  projectId: string;
  materialId: string;
  datasetId: string;
  simulationType: SimulationType;
  status: JobStatus;
  fitTarget: FitTarget;
  submittedAt: string;
  completedAt: string | null;
  elapsedMs: number | null;
}

export type SimulationType = 'PARAMETER_IDENTIFICATION' | 'FORWARD_SIMULATION' | 'SENSITIVITY_ANALYSIS';
export type JobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type FitTarget = 'RELAXATION_MODULUS' | 'CREEP_COMPLIANCE' | 'STORAGE_MODULUS' | 'LOSS_MODULUS' | 'COMPLEX_VISCOSITY';

export interface SubmitJobRequest {
  projectId: string;
  materialId: string;
  datasetId: string;
  simulationType: SimulationType;
  fitTarget: FitTarget;
  initialGuess?: number[];
}

export interface SimulationResult {
  jobId: string;
  identifiedParameters: number[];
  parameterNames: string[];
  rSquared: number;
  iterations: number;
  converged: boolean;
  fittedCurve: number[];
  metrics: Record<string, number>;
}
