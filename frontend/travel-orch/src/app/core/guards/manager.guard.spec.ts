import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { managerGuard } from './manager.guard';
import { AuthService } from '../../features/auth/auth.service';

describe('managerGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'isTravelManager']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
      ],
    });

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router);
  });

  it('should allow activation when authenticated and is travel_manager', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.isTravelManager.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => managerGuard(null as any, null as any));

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should redirect to /auth when not authenticated', () => {
    authService.isAuthenticated.and.returnValue(false);
    authService.isTravelManager.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => managerGuard(null as any, null as any));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth']);
  });

  it('should redirect to /auth when authenticated but not travel_manager', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.isTravelManager.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => managerGuard(null as any, null as any));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth']);
  });
});
