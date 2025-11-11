import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Skip token for auth endpoints
  const skipToken = req.url.includes('/auth/login') || 
                    req.url.includes('/auth/signup') ||
                    req.url.includes('/auth/logout') ||
                    req.url.includes('/auth/forgot-password');

  if (token && !skipToken) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};
