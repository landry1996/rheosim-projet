import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PluginResponse {
  id: string;
  name: string;
  slug: string;
  description: string;
  authorId: string;
  authorName: string | null;
  license: string;
  tags: string[];
  downloadsCount: number;
  ratingAverage: number;
  ratingCount: number;
  status: string;
  latestVersion: string | null;
  createdAt: string;
}

export interface CreatePluginRequest {
  name: string;
  description: string;
  license: string;
  tags: string[];
}

export interface CreateVersionRequest {
  version: string;
  artifactUrl: string;
  artifactHash: string;
  changelog: string;
}

export interface CreateReviewRequest {
  rating: number;
  comment: string;
}

export interface PluginReview {
  id: string;
  pluginId: string;
  userId: string;
  rating: number;
  comment: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class MarketplaceService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/marketplace`;

  listPlugins(page = 0, size = 20): Observable<PluginResponse[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PluginResponse[]>(`${this.baseUrl}/plugins`, { params });
  }

  searchPlugins(query: string, tags: string[] = [], page = 0, size = 20): Observable<PluginResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) params = params.set('query', query);
    tags.forEach(tag => params = params.append('tags', tag));
    return this.http.get<PluginResponse[]>(`${this.baseUrl}/plugins/search`, { params });
  }

  getPlugin(slug: string): Observable<PluginResponse> {
    return this.http.get<PluginResponse>(`${this.baseUrl}/plugins/${slug}`);
  }

  createPlugin(request: CreatePluginRequest): Observable<PluginResponse> {
    return this.http.post<PluginResponse>(`${this.baseUrl}/plugins`, request);
  }

  publishVersion(pluginId: string, request: CreateVersionRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/plugins/${pluginId}/versions`, request);
  }

  getReviews(pluginId: string, page = 0, size = 10): Observable<PluginReview[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PluginReview[]>(`${this.baseUrl}/plugins/${pluginId}/reviews`, { params });
  }

  addReview(pluginId: string, request: CreateReviewRequest): Observable<PluginReview> {
    return this.http.post<PluginReview>(`${this.baseUrl}/plugins/${pluginId}/reviews`, request);
  }

  downloadPlugin(pluginId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/plugins/${pluginId}/download`, {});
  }
}
