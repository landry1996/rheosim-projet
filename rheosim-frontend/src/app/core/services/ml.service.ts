import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MLPredictionRequest {
  datasetId: string;
  timePoints: number[];
  values: number[];
  experimentType: string;
}

export interface ModelAlternative {
  modelType: string;
  confidence: number;
  initialParameters: number[];
}

export interface MLPredictionResponse {
  recommendedModel: string;
  confidence: number;
  initialParameters: number[];
  alternatives: ModelAlternative[];
  mlAvailable: boolean;
}

export interface MLStatus {
  available: boolean;
}

@Injectable({ providedIn: 'root' })
export class MLService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/ml`;

  predict(request: MLPredictionRequest): Observable<MLPredictionResponse> {
    return this.http.post<MLPredictionResponse>(`${this.baseUrl}/predict`, request);
  }

  getStatus(): Observable<MLStatus> {
    return this.http.get<MLStatus>(`${this.baseUrl}/status`);
  }
}
