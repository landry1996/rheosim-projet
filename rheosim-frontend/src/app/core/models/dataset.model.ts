export interface Dataset {
  id: string;
  fileName: string;
  experimentType: ExperimentType;
  status: DatasetStatus;
  projectId: string;
  rowCount: number;
  columns: DataColumn[];
  validationErrors: ValidationError[];
  createdAt: string;
}

export type ExperimentType =
  | 'CREEP'
  | 'RELAXATION'
  | 'DYNAMIC_OSCILLATORY'
  | 'FLOW_CURVE'
  | 'TEMPERATURE_SWEEP'
  | 'FREQUENCY_SWEEP'
  | 'STRAIN_SWEEP'
  | 'OTHER';

export type DatasetStatus = 'UPLOADED' | 'VALIDATING' | 'VALID' | 'INVALID' | 'PROCESSING';

export interface DataColumn {
  name: string;
  unit: string;
  physicalQuantity: string;
}

export interface ValidationError {
  row: number;
  column: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
}
