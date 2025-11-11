import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { APP_CONSTANTS } from '@core/constants/app.constants';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated) {
    return true;
  }

  // Not logged in, redirect to login page with return url
  router.navigate([APP_CONSTANTS.ROUTES.LOGIN], { 
    queryParams: { returnUrl: state.url } 
  });
  return false;
};
