import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  TravelResponse,
  TravelCreateRequest,
  TravelUpdateRequest,
} from './travel.model';

@Injectable({ providedIn: 'root' })
export class TravelService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getAll(): Observable<TravelResponse[]> {
    return this.http.get<TravelResponse[]>(`${this.apiUrl}/api/travel/travels`);
  }

  getByUser(userId: number): Observable<TravelResponse[]> {
    return this.http.get<TravelResponse[]>(`${this.apiUrl}/api/travel/travels/user/${userId}`);
  }

  getByStatus(status: string): Observable<TravelResponse[]> {
    return this.http.get<TravelResponse[]>(`${this.apiUrl}/api/travel/travels/status/${status}`);
  }

  getById(id: number): Observable<TravelResponse> {
    return this.http.get<TravelResponse>(`${this.apiUrl}/api/travel/travels/${id}`);
  }

  create(data: TravelCreateRequest): Observable<TravelResponse> {
    return this.http.post<TravelResponse>(`${this.apiUrl}/api/travel/travels`, data);
  }

  update(id: number, data: TravelUpdateRequest): Observable<TravelResponse> {
    return this.http.put<TravelResponse>(`${this.apiUrl}/api/travel/travels/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/travel/travels/${id}`);
  }
}
