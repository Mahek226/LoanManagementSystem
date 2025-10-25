# API Integration Guide

## Backend API Structure

### Admin Login Endpoint

**URL:** `POST http://localhost:8080/api/admin/auth/login`

**Request Body:**
```json
{
  "usernameOrEmail": "admin123",
  "password": "admin123456"
}
```

**Response (200 OK):**
```json
{
  "adminId": 1,
  "username": "admin123",
  "email": "admin@example.com",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

---

## Frontend Integration

### 1. Login Request Mapping

The Angular frontend sends the following structure:

**TypeScript Interface:**
```typescript
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}
```

**Form Field Mapping:**
- `usernameOrEmail` → Username or email input field
- `password` → Password input field

### 2. Response Handling

**TypeScript Interface:**
```typescript
export interface AdminLoginResponse {
  adminId: number;
  username: string;
  email: string;
  accessToken: string;
  tokenType: string;
}
```

**Storage:**
- `accessToken` → Stored in sessionStorage as 'lms_auth_token'
- User object → Stored in sessionStorage as 'lms_user'

**User Object Structure:**
```typescript
{
  id: response.adminId,
  username: response.username,
  email: response.email,
  role: 'ADMIN'
}
```

### 3. Authentication Flow

```
1. User enters credentials in login form
2. Form data → { usernameOrEmail: 'admin123', password: 'admin123456' }
3. POST request to /api/admin/auth/login
4. Backend validates credentials
5. Backend returns JWT token + user info
6. Frontend stores token + user in sessionStorage
7. Frontend redirects to /admin/dashboard
8. All subsequent requests include: Authorization: Bearer {token}
```

### 4. HTTP Interceptor

The auth interceptor automatically adds the JWT token to all API requests:

```typescript
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Excluded Endpoints (no token added):**
- `/admin/auth/login`
- `/auth/signup`
- `/auth/forgot-password`

### 5. Role-Based Access Control

**Roles:**
- `ADMIN` → `/admin/dashboard`
- `LOAN_OFFICER` → `/loan-officer/dashboard`
- `COMPLIANCE_OFFICER` → `/compliance/dashboard`
- `APPLICANT` → `/applicant/dashboard`

**Route Guards:**
- `authGuard` → Checks if user has valid token
- `roleGuard` → Checks if user has required role for route
- `guestGuard` → Prevents logged-in users from accessing login page

---

## Testing the Integration

### 1. Start Backend
```bash
cd LoanManagementSystem
mvn spring-boot:run
```

Backend should be running on `http://localhost:8080`

### 2. Start Frontend
```bash
cd Frontend
npm install
npm start
```

Frontend will be available at `http://localhost:4200`

### 3. Test Login

1. Navigate to `http://localhost:4200`
2. Enter credentials:
   - **Username/Email:** admin123
   - **Password:** admin123456
3. Click "Sign In"
4. You should be redirected to `/admin/dashboard`

### 4. Verify Token Storage

Open browser DevTools → Application → Session Storage → `http://localhost:4200`

You should see:
- `lms_auth_token`: Your JWT token
- `lms_user`: User object with role

### 5. Verify API Calls

Open browser DevTools → Network tab

After login, check subsequent API calls include the Authorization header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Troubleshooting

### CORS Issues

If you see CORS errors, ensure your Spring Boot backend has CORS configured:

**SecurityConfig.java:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Collections.singletonList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 401 Unauthorized

If you get 401 errors:
1. Check token is being sent in Authorization header
2. Verify token hasn't expired (24 hours)
3. Check backend JWT secret key matches

### Login Fails

If login doesn't work:
1. Check backend is running on port 8080
2. Verify credentials are correct in database
3. Check browser console for error messages
4. Check network tab for request/response details

---

## API Endpoints Summary

### Authentication
- **Admin Login:** `POST /api/admin/auth/login`
- **Admin Register:** `POST /api/admin/auth/register`
- **Applicant Login:** `POST /api/applicant/auth/login`
- **Applicant Register:** `POST /api/applicant/auth/register`

### Protected Routes (Require JWT Token)
- **Admin Dashboard:** `GET /api/admin/*`
- **Loan Officer:** `GET /api/loan-officer/*`
- **Compliance Officer:** `GET /api/compliance/*`
- **Applicant:** `GET /api/applicant/*`

---

## Security Notes

1. **JWT Token Storage:** Tokens are stored in sessionStorage (cleared on browser close)
2. **Token Expiry:** Tokens expire after 24 hours
3. **Auto Logout:** Frontend automatically logs out on 401 errors
4. **HTTPS:** Always use HTTPS in production
5. **Token Security:** Never log or expose tokens in client-side code

---

## Next Steps

1. ✅ Admin login implemented
2. 🔲 Add Applicant login (separate endpoint: `/api/applicant/auth/login`)
3. 🔲 Add Loan Officer login (if separate endpoint exists)
4. 🔲 Add Compliance Officer login (if separate endpoint exists)
5. 🔲 Implement role-specific features in each dashboard
6. 🔲 Add logout functionality
7. 🔲 Implement token refresh mechanism

---

**Last Updated:** October 7, 2025  
**Version:** 1.0.0
