import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  AnalyticsOverviewResponse,
  MonthlyIncomeResponse,
  ManagerRankingResponse,
  TravelRankingResponse,
  PagedResponse,
} from './analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getOverview(): Observable<AnalyticsOverviewResponse> {
    return this.http.get<AnalyticsOverviewResponse>(`${this.apiUrl}/api/travel/analytics/overview`);
  }

  getIncome(months: number = 6): Observable<MonthlyIncomeResponse[]> {
    return this.http.get<MonthlyIncomeResponse[]>(`${this.apiUrl}/api/travel/analytics/income`, {
      params: { months },
    });
  }

  getTopManagers(page: number = 0, size: number = 5): Observable<PagedResponse<ManagerRankingResponse>> {
    return this.http.get<PagedResponse<ManagerRankingResponse>>(`${this.apiUrl}/api/travel/analytics/managers/top`, {
      params: { page, size },
    });
  }

  getTopTravels(page: number = 0, size: number = 5): Observable<PagedResponse<TravelRankingResponse>> {
    return this.http.get<PagedResponse<TravelRankingResponse>>(`${this.apiUrl}/api/travel/analytics/travels/top`, {
      params: { page, size },
    });
  }
}
