import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReportCreateRequest, ReportResponse } from './report.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  create(req: ReportCreateRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/api/travel/reports`, req);
  }

  getAll(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${this.apiUrl}/api/travel/reports`);
  }
}
