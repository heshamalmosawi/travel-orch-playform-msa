import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PurchaseResponse } from './purchase.model';

@Injectable({ providedIn: 'root' })
export class PurchaseService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  purchase(travelId: number): Observable<PurchaseResponse> {
    return this.http.post<PurchaseResponse>(
      `${this.apiUrl}/api/payment/transactions/purchase`,
      { travelId }
    );
  }

  getMine(): Observable<PurchaseResponse[]> {
    return this.http.get<PurchaseResponse[]>(
      `${this.apiUrl}/api/payment/transactions/mine`
    );
  }

  getByTravel(travelId: number): Observable<PurchaseResponse[]> {
    return this.http.get<PurchaseResponse[]>(
      `${this.apiUrl}/api/payment/transactions/travel/${travelId}`
    );
  }

  cancelRefund(id: number): Observable<PurchaseResponse> {
    return this.http.post<PurchaseResponse>(
      `${this.apiUrl}/api/payment/transactions/${id}/cancel`,
      {}
    );
  }
}
