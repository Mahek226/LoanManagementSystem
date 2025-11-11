import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { APP_CONSTANTS } from '@core/constants/app.constants';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Unauthorized - logout and redirect to login
        authService.logout();
        router.navigate([APP_CONSTANTS.ROUTES.LOGIN]);
      }

      const errorMessage = error.error?.message || error.message || 'An error occurred';
      return throwError(() => new Error(errorMessage));
    })
  );
};
