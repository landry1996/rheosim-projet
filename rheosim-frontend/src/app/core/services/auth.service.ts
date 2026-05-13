import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private readonly currentUser = signal<User | null>(null);
  private readonly tokenExpiry = signal<number>(0);
  private readonly STORAGE_KEY_TOKEN = 'rs_at';
  private readonly STORAGE_KEY_REFRESH = 'rs_rt';
  private readonly STORAGE_KEY_USER = 'rs_u';

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUser() && Date.now() < this.tokenExpiry());

  constructor(private http: HttpClient, private router: Router) {
    this.loadFromStorage();
    this.setupTabCloseCleanup();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(err => throwError(() => err))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(err => throwError(() => err))
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getStorageItem(this.STORAGE_KEY_REFRESH);
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(err => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    const refreshToken = this.getStorageItem(this.STORAGE_KEY_REFRESH);
    if (refreshToken) {
      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe();
    }
    this.clearStorage();
    this.currentUser.set(null);
    this.tokenExpiry.set(0);
    this.router.navigate(['/auth/login']);
  }

  getAccessToken(): string | null {
    return this.getStorageItem(this.STORAGE_KEY_TOKEN);
  }

  private handleAuthResponse(response: AuthResponse): void {
    this.setStorageItem(this.STORAGE_KEY_TOKEN, response.accessToken);
    this.setStorageItem(this.STORAGE_KEY_REFRESH, response.refreshToken);
    this.setStorageItem(this.STORAGE_KEY_USER, JSON.stringify({
      id: response.user.id,
      firstName: response.user.firstName,
      roles: response.user.roles
    }));
    this.currentUser.set(response.user);
    this.tokenExpiry.set(Date.now() + response.expiresIn * 1000);
  }

  private loadFromStorage(): void {
    const userJson = this.getStorageItem(this.STORAGE_KEY_USER);
    const token = this.getStorageItem(this.STORAGE_KEY_TOKEN);
    if (userJson && token) {
      try {
        const parts = token.split('.');
        if (parts.length !== 3) {
          this.clearStorage();
          return;
        }
        this.currentUser.set(JSON.parse(userJson));
        const payload = JSON.parse(atob(parts[1]));
        const expiry = payload.exp * 1000;
        if (Date.now() >= expiry) {
          this.clearStorage();
          return;
        }
        this.tokenExpiry.set(expiry);
      } catch {
        this.clearStorage();
      }
    }
  }

  private setStorageItem(key: string, value: string): void {
    sessionStorage.setItem(key, value);
  }

  private getStorageItem(key: string): string | null {
    return sessionStorage.getItem(key);
  }

  private clearStorage(): void {
    sessionStorage.removeItem(this.STORAGE_KEY_TOKEN);
    sessionStorage.removeItem(this.STORAGE_KEY_REFRESH);
    sessionStorage.removeItem(this.STORAGE_KEY_USER);
  }

  private setupTabCloseCleanup(): void {
    window.addEventListener('beforeunload', () => {
      if (!this.isAuthenticated()) {
        this.clearStorage();
      }
    });
  }
}
