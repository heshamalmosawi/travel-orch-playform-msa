import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, share, switchMap, take, tap, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/auth.service';

// Module-level single-flight: concurrent 401s share one refresh call. The shared
// observable emits the new token on success (waiters retry) or errors on failure
// (waiters propagate the error), so queued requests never hang.
let refreshInFlight$: Observable<string> | null = null;

function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login')
    || url.includes('/auth/register')
    || url.includes('/auth/refresh');
}

function withToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.getToken();
  const authReq = token && !isAuthEndpoint(req.url) ? withToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Only attempt a refresh on a genuine 401 for a protected request,
      // and only when we actually have a refresh token to use.
      if (error.status !== 401 || isAuthEndpoint(req.url) || !auth.getRefreshToken()) {
        return throwError(() => error);
      }

      if (!refreshInFlight$) {
        refreshInFlight$ = auth.refresh().pipe(
          map((res) => res.token!),
          tap({
            error: () => {
              auth.logout();
              router.navigate(['/auth']);
            },
          }),
          finalize(() => {
            refreshInFlight$ = null;
          }),
          share()
        );
      }

      return refreshInFlight$.pipe(
        take(1),
        switchMap((newToken) => next(withToken(req, newToken)))
      );
    })
  );
};
