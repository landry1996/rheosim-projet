export interface Material {
  id: string;
  name: string;
  grade: string;
  family: MaterialFamily;
  supplier: string;
  version: number;
  model: MaterialModel | null;
  projectId: string;
}

export type MaterialFamily = 'THERMOPLASTIC' | 'ELASTOMER' | 'THERMOSET' | 'COMPOSITE' | 'FOAM' | 'OTHER';

export interface MaterialModel {
  modelType: ConstitutiveModelType;
  numberOfBranches: number;
  equilibriumModulus: number;
  branches: PronyBranch[];
  referenceTemperatureK: number;
}

export interface PronyBranch {
  modulusPa: number;
  relaxationTimeS: number;
}

export type ConstitutiveModelType = 'MAXWELL' | 'KELVIN_VOIGT' | 'PRONY' | 'POWER_LAW' | 'CARREAU_YASUDA';

export interface CreateMaterialRequest {
  name: string;
  grade?: string;
  family: MaterialFamily;
  supplier?: string;
  projectId: string;
}
