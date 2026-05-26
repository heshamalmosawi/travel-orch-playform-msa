import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, UserUpdateRequest, RoleUpdateRequest } from './admin.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/api/user/users`);
  }

  getUsersByRole(role: string): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/api/user/users`, { params: { role } });
  }

  getUser(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/api/user/users/${id}`);
  }

  updateUser(id: number, data: UserUpdateRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/api/user/users/${id}`, data);
  }

  updateUserRole(id: number, data: RoleUpdateRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/api/user/users/${id}/role`, data);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/user/users/${id}`);
  }
}
