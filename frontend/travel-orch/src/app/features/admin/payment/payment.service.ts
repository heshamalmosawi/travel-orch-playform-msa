import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PaymentTransactionResponse } from './payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getAll(): Observable<PaymentTransactionResponse[]> {
    return this.http.get<PaymentTransactionResponse[]>(`${this.apiUrl}/api/payment/transactions`);
  }

  getById(id: number): Observable<PaymentTransactionResponse> {
    return this.http.get<PaymentTransactionResponse>(`${this.apiUrl}/api/payment/transactions/${id}`);
  }
}
