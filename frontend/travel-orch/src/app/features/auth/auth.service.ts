import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, RegistrationRequest, AuthResponse } from './auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly tokenKey = 'auth_token';
  private readonly refreshTokenKey = 'refresh_token';

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/api/user/auth/login`, data)
      .pipe(tap((res) => this.handleAuthSuccess(res)));
  }

  register(data: RegistrationRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/api/user/auth/register`, data)
      .pipe(tap((res) => this.handleAuthSuccess(res)));
  }

  refresh(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/api/user/auth/refresh`, {
        refreshToken: this.getRefreshToken(),
      })
      .pipe(tap((res) => this.handleAuthSuccess(res)));
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  /**
   * Authenticated as long as a non-expired access token exists, or a non-expired
   * refresh token is available (the interceptor will silently mint a new access
   * token on the next request).
   */
  isAuthenticated(): boolean {
    const access = this.getToken();
    if (access && !this.isExpired(access)) return true;
    const refresh = this.getRefreshToken();
    return !!refresh && !this.isExpired(refresh);
  }

  private isExpired(token: string): boolean {
    try {
      const payload = JSON.parse(this.decodeBase64Url(token.split('.')[1]));
      const exp = payload['exp'];
      if (!exp) return false;
      return Date.now() >= exp * 1000;
    } catch {
      return true;
    }
  }

  isAdmin(): boolean {
    return this.hasRole('admin');
  }

  isTravelManager(): boolean {
    return this.hasRole('travel_manager');
  }

  isTraveler(): boolean {
    return this.isAuthenticated() && this.hasRole('user');
  }

  private decodeBase64Url(str: string): string {
    let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) {
      base64 += '=';
    }
    return atob(base64);
  }

  getUsername(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(this.decodeBase64Url(token.split('.')[1]));
      return payload['sub'] || null;
    } catch {
      return null;
    }
  }

  hasRole(role: string): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(this.decodeBase64Url(token.split('.')[1]));
      const tokenRole = payload['role'] || '';
      return tokenRole.toLowerCase() === role.toLowerCase();
    } catch {
      return false;
    }
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    if (response.token) {
      localStorage.setItem(this.tokenKey, response.token);
    }
    if (response.refreshToken) {
      localStorage.setItem(this.refreshTokenKey, response.refreshToken);
    }
  }
}
