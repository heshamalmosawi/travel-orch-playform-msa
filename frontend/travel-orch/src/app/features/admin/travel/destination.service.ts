import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DestinationResponse, DestinationCreateRequest, DestinationUpdateRequest } from './destination.model';

@Injectable({ providedIn: 'root' })
export class DestinationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getAll(): Observable<DestinationResponse[]> {
    return this.http.get<DestinationResponse[]>(`${this.apiUrl}/api/travel/destinations`);
  }

  search(params: { name?: string; country?: string; city?: string }): Observable<DestinationResponse[]> {
    let httpParams = new HttpParams();
    if (params.name) httpParams = httpParams.set('name', params.name);
    if (params.country) httpParams = httpParams.set('country', params.country);
    if (params.city) httpParams = httpParams.set('city', params.city);
    return this.http.get<DestinationResponse[]>(`${this.apiUrl}/api/travel/destinations/search`, { params: httpParams });
  }

  getById(id: number): Observable<DestinationResponse> {
    return this.http.get<DestinationResponse>(`${this.apiUrl}/api/travel/destinations/${id}`);
  }

  create(data: DestinationCreateRequest): Observable<DestinationResponse> {
    return this.http.post<DestinationResponse>(`${this.apiUrl}/api/travel/destinations`, data);
  }

  update(id: number, data: DestinationUpdateRequest): Observable<DestinationResponse> {
    return this.http.put<DestinationResponse>(`${this.apiUrl}/api/travel/destinations/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/travel/destinations/${id}`);
  }
}
