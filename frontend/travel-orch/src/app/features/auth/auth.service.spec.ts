import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { AuthResponse } from './auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let http: jasmine.SpyObj<HttpClient>;

  beforeEach(() => {
    const httpSpy = jasmine.createSpyObj('HttpClient', ['post']);

    TestBed.configureTestingModule({
      providers: [
        { provide: HttpClient, useValue: httpSpy },
      ],
    });

    http = TestBed.inject(HttpClient) as jasmine.SpyObj<HttpClient>;
    service = TestBed.inject(AuthService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return null token when not logged in', () => {
    expect(service.getToken()).toBeNull();
  });

  it('should return false for isAuthenticated when no token', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should return false for isAdmin when no token', () => {
    expect(service.isAdmin()).toBeFalse();
  });

  it('should return false for isTravelManager when no token', () => {
    expect(service.isTravelManager()).toBeFalse();
  });

  it('should return false for hasRole when no token', () => {
    expect(service.hasRole('admin')).toBeFalse();
  });

  it('should detect admin role from token', () => {
    const payload = btoa(JSON.stringify({ role: 'admin' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.isAdmin()).toBeTrue();
    expect(service.hasRole('admin')).toBeTrue();
  });

  it('should detect travel_manager role from token', () => {
    const payload = btoa(JSON.stringify({ role: 'travel_manager' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.isTravelManager()).toBeTrue();
    expect(service.hasRole('travel_manager')).toBeTrue();
    expect(service.isAdmin()).toBeFalse();
  });

  it('should return true for isTraveler when authenticated but not admin', () => {
    const payload = btoa(JSON.stringify({ role: 'user' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.isTraveler()).toBeTrue();
  });

  it('should return false for isTraveler when admin', () => {
    const payload = btoa(JSON.stringify({ role: 'admin' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.isTraveler()).toBeFalse();
  });

  it('should return false for isTraveler when travel_manager', () => {
    const payload = btoa(JSON.stringify({ role: 'travel_manager' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.isTraveler()).toBeFalse();
  });

  it('should handle malformed token gracefully', () => {
    localStorage.setItem('auth_token', 'not-a-valid-token');

    expect(service.hasRole('admin')).toBeFalse();
    expect(service.isAdmin()).toBeFalse();
    expect(service.isTravelManager()).toBeFalse();
  });

  it('should handle token with no role claim', () => {
    const payload = btoa(JSON.stringify({ sub: 'testuser' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.hasRole('admin')).toBeFalse();
    expect(service.isTravelManager()).toBeFalse();
  });

  it('should perform case-insensitive role matching', () => {
    const payload = btoa(JSON.stringify({ role: 'ADMIN' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.hasRole('admin')).toBeTrue();
    expect(service.hasRole('Admin')).toBeTrue();
    expect(service.hasRole('ADMIN')).toBeTrue();
  });

  it('should remove token on logout', () => {
    const payload = btoa(JSON.stringify({ role: 'user' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('auth_token', fakeToken);

    expect(service.isAuthenticated()).toBeTrue();

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should store token on successful login', () => {
    const response: AuthResponse = {
      message: 'Login successful',
      username: 'testuser',
      email: 'test@example.com',
      token: 'fake.jwt.token',
    };

    const payload = btoa(JSON.stringify({ role: 'user' }));
    response.token = `header.${payload}.signature`;

    http.post.and.returnValue(of(response));

    service.login({ username: 'testuser', password: 'pass' }).subscribe();

    expect(localStorage.getItem('auth_token')).toBe(response.token);
  });

  it('should store token on successful register', () => {
    const response: AuthResponse = {
      message: 'Registered',
      username: 'newuser',
      token: 'fake.jwt.token',
    };

    const payload = btoa(JSON.stringify({ role: 'user' }));
    response.token = `header.${payload}.signature`;

    http.post.and.returnValue(of(response));

    service.register({ username: 'newuser', email: 'new@test.com', password: 'pass', firstName: 'New', lastName: 'User' }).subscribe();

    expect(localStorage.getItem('auth_token')).toBe(response.token);
  });
});
