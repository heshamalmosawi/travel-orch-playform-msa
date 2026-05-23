import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { PurchaseService } from './purchase.service';
import { PurchaseResponse } from './purchase.model';
import { environment } from '../../../environments/environment';

describe('PurchaseService', () => {
  let service: PurchaseService;
  let http: jasmine.SpyObj<HttpClient>;
  const base = `${environment.apiUrl}/api/payment/transactions`;

  beforeEach(() => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['get', 'post']);
    TestBed.configureTestingModule({
      providers: [{ provide: HttpClient, useValue: httpSpy }],
    });
    http = TestBed.inject(HttpClient) as jasmine.SpyObj<HttpClient>;
    service = TestBed.inject(PurchaseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('purchase posts the travelId to the purchase endpoint', () => {
    const resp = { id: 1, travelId: 7 } as PurchaseResponse;
    http.post.and.returnValue(of(resp));

    let result: PurchaseResponse | undefined;
    service.purchase(7).subscribe((r) => (result = r));

    expect(http.post).toHaveBeenCalledWith(`${base}/purchase`, { travelId: 7 });
    expect(result).toEqual(resp);
  });

  it('getMine queries the mine endpoint', () => {
    http.get.and.returnValue(of([]));
    service.getMine().subscribe();
    expect(http.get).toHaveBeenCalledWith(`${base}/mine`);
  });

  it('cancelRefund posts to the cancel endpoint', () => {
    const resp = { id: 3, status: 'refunded' } as PurchaseResponse;
    http.post.and.returnValue(of(resp));

    let result: PurchaseResponse | undefined;
    service.cancelRefund(3).subscribe((r) => (result = r));

    expect(http.post).toHaveBeenCalledWith(`${base}/3/cancel`, {});
    expect(result).toEqual(resp);
  });
});
