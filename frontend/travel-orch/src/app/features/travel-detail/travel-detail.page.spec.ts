import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { TravelDetailPage } from './travel-detail.page';
import { TravelService } from '../admin/travel/travel.service';
import { TravelResponse } from '../admin/travel/travel.model';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../../shared/components/toast/toast.service';
import { PurchaseService } from './purchase.service';
import { PurchaseResponse } from './purchase.model';

function isoDaysFromNow(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().split('T')[0];
}

function makeTravel(overrides: Partial<TravelResponse> = {}): TravelResponse {
  return {
    id: 1,
    title: 'Test Trip',
    description: null,
    startDate: isoDaysFromNow(10),
    endDate: isoDaysFromNow(17),
    totalPrice: 500,
    status: 'confirmed',
    managerId: 1,
    destinations: [],
    createdAt: '',
    updatedAt: '',
    ...overrides,
  };
}

interface AuthOpts {
  authenticated: boolean;
  traveler: boolean;
}

describe('TravelDetailPage', () => {
  let travelSpy: jasmine.SpyObj<TravelService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let purchaseSpy: jasmine.SpyObj<PurchaseService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  function setup(travel: TravelResponse, auth: AuthOpts, mine: PurchaseResponse[] = []) {
    travelSpy = jasmine.createSpyObj('TravelService', ['getById']);
    authSpy = jasmine.createSpyObj('AuthService', [
      'isAuthenticated',
      'isTraveler',
      'isAdmin',
      'isTravelManager',
      'logout',
    ]);
    purchaseSpy = jasmine.createSpyObj('PurchaseService', ['getMine', 'purchase', 'cancelRefund']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    travelSpy.getById.and.returnValue(of(travel));
    purchaseSpy.getMine.and.returnValue(of(mine));
    authSpy.isAuthenticated.and.returnValue(auth.authenticated);
    authSpy.isTraveler.and.returnValue(auth.traveler);
    authSpy.isAdmin.and.returnValue(false);
    authSpy.isTravelManager.and.returnValue(false);

    TestBed.configureTestingModule({
      imports: [TravelDetailPage],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: TravelService, useValue: travelSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: PurchaseService, useValue: purchaseSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    });

    return TestBed.createComponent(TravelDetailPage).componentInstance;
  }

  it('allows purchase for a traveler when start is >= 3 days away and there is a price', () => {
    const comp = setup(makeTravel(), { authenticated: false, traveler: true });

    expect(comp.daysUntilStart()).toBeGreaterThanOrEqual(3);
    expect(comp.canPurchase()).toBeTrue();
    expect(comp.purchaseClosed()).toBeFalse();
  });

  it('blocks purchase and flags closed when start is within 3 days', () => {
    const comp = setup(makeTravel({ startDate: isoDaysFromNow(1) }), {
      authenticated: false,
      traveler: true,
    });

    expect(comp.canPurchase()).toBeFalse();
    expect(comp.purchaseClosed()).toBeTrue();
  });

  it('does not allow purchase for a non-traveler', () => {
    const comp = setup(makeTravel(), { authenticated: false, traveler: false });

    expect(comp.canPurchase()).toBeFalse();
    expect(comp.purchaseClosed()).toBeFalse();
  });

  it('shows an active purchase and hides the purchase button when already owned', () => {
    const mine = [{ id: 9, travelId: 1, status: 'completed' } as PurchaseResponse];
    const comp = setup(makeTravel(), { authenticated: true, traveler: true }, mine);

    expect(comp.myPurchase()?.id).toBe(9);
    expect(comp.hasActivePurchase()).toBeTrue();
    expect(comp.canPurchase()).toBeFalse();
  });

  it('ignores refunded purchases when computing active ownership', () => {
    const mine = [{ id: 9, travelId: 1, status: 'refunded' } as PurchaseResponse];
    const comp = setup(makeTravel(), { authenticated: true, traveler: true }, mine);

    expect(comp.myPurchase()).toBeNull();
    expect(comp.hasActivePurchase()).toBeFalse();
    expect(comp.canPurchase()).toBeTrue();
  });

  it('confirmPurchase stores the purchase and toasts success', () => {
    const comp = setup(makeTravel(), { authenticated: false, traveler: true });
    const purchased = { id: 42, travelId: 1, status: 'completed' } as PurchaseResponse;
    purchaseSpy.purchase.and.returnValue(of(purchased));

    comp.confirmPurchase();

    expect(purchaseSpy.purchase).toHaveBeenCalledWith(1);
    expect(comp.myPurchase()?.id).toBe(42);
    expect(toastSpy.success).toHaveBeenCalled();
  });

  it('confirmCancel marks the purchase refunded and toasts success', () => {
    const mine = [{ id: 9, travelId: 1, status: 'completed' } as PurchaseResponse];
    const comp = setup(makeTravel(), { authenticated: true, traveler: true }, mine);
    const refunded = { id: 9, travelId: 1, status: 'refunded' } as PurchaseResponse;
    purchaseSpy.cancelRefund.and.returnValue(of(refunded));

    comp.confirmCancel();

    expect(purchaseSpy.cancelRefund).toHaveBeenCalledWith(9);
    expect(comp.hasActivePurchase()).toBeFalse();
    expect(toastSpy.success).toHaveBeenCalled();
  });
});
