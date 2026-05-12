import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SimulationJob, SimulationResult, SubmitJobRequest } from '../models/simulation.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SimulationService {
  private readonly apiUrl = `${environment.apiUrl}/simulations`;

  constructor(private http: HttpClient) {}

  submit(request: SubmitJobRequest): Observable<SimulationJob> {
    return this.http.post<SimulationJob>(this.apiUrl, request);
  }

  getStatus(id: string): Observable<SimulationJob> {
    return this.http.get<SimulationJob>(`${this.apiUrl}/${id}`);
  }

  getResult(id: string): Observable<SimulationResult> {
    return this.http.get<SimulationResult>(`${this.apiUrl}/${id}/result`);
  }

  cancel(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/cancel`, {});
  }

  getByProject(projectId: string): Observable<SimulationJob[]> {
    return this.http.get<SimulationJob[]>(`${this.apiUrl}?projectId=${projectId}`);
  }
}
