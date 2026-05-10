import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, UserUpdateRequest } from './admin.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/api/user/users`);
  }

  getUser(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/api/user/users/${id}`);
  }

  updateUser(id: number, data: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/api/user/users/${id}`, data);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/user/users/${id}`);
  }
}
