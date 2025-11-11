import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { APP_CONSTANTS } from '@core/constants/app.constants';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[];
  
  if (!authService.isAuthenticated) {
    router.navigate([APP_CONSTANTS.ROUTES.LOGIN]);
    return false;
  }

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const hasRole = requiredRoles.some(role => authService.hasRole(role));
  
  if (hasRole) {
    return true;
  }

  // User doesn't have required role, redirect to their default dashboard
  const defaultRoute = authService.getDefaultRoute();
  router.navigate([defaultRoute]);
  return false;
};
