import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Dataset } from '../models/dataset.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DatasetService {
  private readonly apiUrl = `${environment.apiUrl}/datasets`;

  constructor(private http: HttpClient) {}

  upload(projectId: string, experimentType: string, file: File): Observable<Dataset> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', projectId);
    formData.append('experimentType', experimentType);
    return this.http.post<Dataset>(this.apiUrl, formData);
  }

  validate(id: string): Observable<Dataset> {
    return this.http.post<Dataset>(`${this.apiUrl}/${id}/validate`, {});
  }

  getByProject(projectId: string): Observable<Dataset[]> {
    return this.http.get<Dataset[]>(`${this.apiUrl}?projectId=${projectId}`);
  }

  getById(id: string): Observable<Dataset> {
    return this.http.get<Dataset>(`${this.apiUrl}/${id}`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
