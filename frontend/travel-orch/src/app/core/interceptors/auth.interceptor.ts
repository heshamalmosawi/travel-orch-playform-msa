import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/auth.service';

// Module-level single-flight state so concurrent 401s share one refresh call.
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

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

      // A refresh is already in flight: wait for the new token, then retry.
      if (isRefreshing) {
        return refreshedToken$.pipe(
          filter((t): t is string => t !== null),
          take(1),
          switchMap((newToken) => next(withToken(req, newToken)))
        );
      }

      isRefreshing = true;
      refreshedToken$.next(null);

      return auth.refresh().pipe(
        switchMap((res) => {
          isRefreshing = false;
          const newToken = res.token!;
          refreshedToken$.next(newToken);
          return next(withToken(req, newToken));
        }),
        catchError((refreshError) => {
          isRefreshing = false;
          auth.logout();
          router.navigate(['/auth']);
          return throwError(() => refreshError);
        })
      );
    })
  );
};
