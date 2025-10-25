# Quick Start Guide

## Installation (First Time Only)

```bash
# Navigate to Frontend folder
cd Frontend

# Install dependencies
npm install

# This will install all Angular packages and resolve the lint errors
```

## Running the Application

```bash
# Start the development server (with proxy for backend API)
npm start

# Application will open automatically at http://localhost:4200
```

## Test the Login

Once both backend (Spring Boot) and frontend (Angular) are running:

1. **Open browser**: http://localhost:4200
2. **Login with test credentials** (create users in your backend first)
3. **You'll be redirected** to the appropriate dashboard based on your role:
   - Admin → `/admin/dashboard`
   - Loan Officer → `/loan-officer/dashboard`
   - Compliance Officer → `/compliance/dashboard`
   - Applicant → `/applicant/dashboard`

## Available Scripts

```bash
npm start              # Start dev server with proxy (recommended)
npm run start:no-proxy # Start dev server without proxy
npm run build          # Build for development
npm run build:prod     # Build for production
npm test               # Run unit tests
```

## Folder Structure Overview

```
Frontend/
├── src/app/
│   ├── core/                    # ✅ Services, Guards, Interceptors
│   │   ├── guards/             # Auth, Role, Guest guards
│   │   ├── interceptors/       # JWT token, Error handling
│   │   ├── services/           # Auth, Storage services
│   │   ├── models/             # TypeScript interfaces
│   │   └── constants/          # App constants
│   │
│   ├── features/               # ✅ Feature modules
│   │   ├── auth/
│   │   │   ├── login/         # Login component
│   │   │   └── forgot-password/ # Forgot password component
│   │   ├── admin/             # Admin dashboard
│   │   ├── loan-officer/      # Loan officer dashboard
│   │   ├── compliance-officer/ # Compliance dashboard
│   │   └── applicant/         # Applicant dashboard
│   │
│   ├── app.component.ts       # Root component
│   ├── app.config.ts          # App configuration
│   └── app.routes.ts          # Routing configuration
│
├── environments/              # API URL configuration
└── styles.css                # Global styles
```

## Key Features Implemented

### ✅ Authentication
- Common login page for all roles
- Forgot password functionality
- JWT token-based authentication
- Auto-logout on unauthorized access

### ✅ Security
- HTTP interceptor (adds JWT token automatically)
- Auth guard (protects authenticated routes)
- Role guard (role-based access control)
- Guest guard (prevents logged-in users from accessing login page)

### ✅ Routing
- Lazy loading for all feature modules
- Automatic redirection based on user role
- Return URL support after login

### ✅ Best Practices
- Feature-based folder structure
- Standalone components (Angular 17)
- Reactive forms with validation
- TypeScript path aliases (@core, @shared, @features)
- Environment-based configuration
- Separation of concerns

## Next Steps

1. **Install dependencies**: `npm install`
2. **Start backend**: Ensure Spring Boot app is running on port 8080
3. **Start frontend**: `npm start`
4. **Create test users** in the backend with different roles
5. **Test login** and navigation

## Troubleshooting

### Dependencies Not Installing
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall
npm install
```

### Backend Connection Issues
- Check backend is running on `http://localhost:8080`
- Verify CORS is configured in Spring Boot `SecurityConfig`
- Check `proxy.conf.json` configuration

### Build Errors
```bash
# Clear Angular cache
rm -rf .angular/cache

# Rebuild
npm run build
```
