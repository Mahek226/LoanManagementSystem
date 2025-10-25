# Common Login Endpoint Guide

## 🎯 Overview

I've created a **unified login endpoint** that handles authentication for all user types:
- **Admin**
- **Applicant** 
- **Loan Officer**
- **Compliance Officer**

## 🔗 API Endpoint

**URL:** `POST http://localhost:8080/api/auth/login`

## 📝 Request Format

```json
{
  "usernameOrEmail": "admin123",
  "password": "admin123456"
}
```

## 📤 Response Format

```json
{
  "userId": 1,
  "username": "admin123",
  "firstName": null,
  "lastName": null,
  "email": "admin@example.com",
  "userType": "ADMIN",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "message": "Login successful"
}
```

## 🔄 How It Works

### Backend Logic (CommonAuthService)

1. **Try Admin Login** - Check admin table first
2. **Try Applicant Login** - Check applicant table (with approval/verification checks)
3. **Try Loan Officer Login** - Check loan officer table
4. **Try Compliance Officer Login** - Check compliance officer table
5. **Return Error** - If no match found

### Frontend Integration

The Angular frontend automatically:
1. Sends login request to `/api/auth/login`
2. Receives response with `userType`
3. Stores user data with role information
4. Redirects to appropriate dashboard based on `userType`

## 🎭 User Type Mapping

| Backend userType | Frontend Role | Dashboard Route |
|------------------|---------------|-----------------|
| `ADMIN` | `ADMIN` | `/admin/dashboard` |
| `APPLICANT` | `APPLICANT` | `/applicant/dashboard` |
| `LOAN_OFFICER` | `LOAN_OFFICER` | `/loan-officer/dashboard` |
| `COMPLIANCE_OFFICER` | `COMPLIANCE_OFFICER` | `/compliance/dashboard` |

## 🧪 Testing Different User Types

### 1. Admin Login
```json
{
  "usernameOrEmail": "admin123",
  "password": "admin123456"
}
```
**Expected Response:**
```json
{
  "userId": 1,
  "username": "admin123",
  "email": "admin@example.com",
  "userType": "ADMIN",
  "accessToken": "...",
  "message": "Login successful"
}
```

### 2. Applicant Login
```json
{
  "usernameOrEmail": "john.doe@example.com",
  "password": "password123"
}
```
**Expected Response:**
```json
{
  "userId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "userType": "APPLICANT",
  "accessToken": "...",
  "message": "Login successful"
}
```

### 3. Loan Officer Login
```json
{
  "usernameOrEmail": "loanofficer1",
  "password": "officer123"
}
```

### 4. Compliance Officer Login
```json
{
  "usernameOrEmail": "complianceofficer1",
  "password": "compliance123"
}
```

## ⚠️ Error Handling

### Common Error Messages:
- `"Invalid username/email or password"` - No user found or wrong password
- `"Please verify your email before logging in"` - Applicant email not verified
- `"Your account is pending admin approval"` - Applicant not approved yet

## 🚀 How to Test

### 1. Start Backend
```bash
cd LoanManagementSystem
mvn spring-boot:run
```

### 2. Start Frontend
```bash
cd Frontend
npm start
```

### 3. Test Login
1. Go to `http://localhost:4200`
2. Enter any user credentials (admin, applicant, etc.)
3. System will automatically detect user type and redirect appropriately

## 🔧 Frontend Changes Made

### 1. Updated API Endpoint
```typescript
// OLD
LOGIN: '/admin/auth/login'

// NEW
LOGIN: '/auth/login'
```

### 2. Updated Response Interface
```typescript
export interface CommonLoginResponse {
  userId: number;
  username?: string;
  firstName?: string;
  lastName?: string;
  email: string;
  userType: string; // "ADMIN", "APPLICANT", "LOAN_OFFICER", "COMPLIANCE_OFFICER"
  accessToken: string;
  tokenType: string;
  message?: string;
}
```

### 3. Updated Auth Service
- Now handles `CommonLoginResponse`
- Maps `userType` to frontend `role`
- Stores complete user information

### 4. Updated Dashboards
- Display appropriate name based on user type
- Admin: Shows `username`
- Applicant: Shows `firstName lastName`
- Officers: Shows `username` or `firstName lastName`

## 🎨 UI Features

### Login Form
- Single form for all user types
- No role selection needed
- Automatic detection and routing

### Dashboard Display
- **Admin Dashboard**: "Welcome, admin123!"
- **Applicant Dashboard**: "Welcome, John Doe!"
- **Loan Officer Dashboard**: "Welcome, loanofficer1!"
- **Compliance Dashboard**: "Welcome, complianceofficer1!"

## 🔐 Security Features

1. **JWT Token**: Same token format for all user types
2. **Role-Based Routing**: Automatic redirection based on `userType`
3. **Route Guards**: Protect role-specific routes
4. **Session Storage**: Secure token storage
5. **Auto Logout**: On token expiration or 401 errors

## 📋 Next Steps

1. ✅ **Common login endpoint created**
2. ✅ **Frontend updated to use common endpoint**
3. ✅ **Role-based routing implemented**
4. 🔲 **Create test users for each role**
5. 🔲 **Add logout functionality**
6. 🔲 **Implement role-specific features**

---

**The system now provides a seamless login experience for all user types through a single endpoint!** 🎉
