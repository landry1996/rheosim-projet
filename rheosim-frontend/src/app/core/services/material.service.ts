import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Material, CreateMaterialRequest, MaterialModel } from '../models/material.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MaterialService {
  private readonly apiUrl = `${environment.apiUrl}/materials`;

  constructor(private http: HttpClient) {}

  getByProject(projectId: string): Observable<Material[]> {
    return this.http.get<Material[]>(`${this.apiUrl}?projectId=${projectId}`);
  }

  getById(id: string): Observable<Material> {
    return this.http.get<Material>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateMaterialRequest): Observable<Material> {
    return this.http.post<Material>(this.apiUrl, request);
  }

  updateModel(id: string, model: MaterialModel): Observable<Material> {
    return this.http.put<Material>(`${this.apiUrl}/${id}/model`, model);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
