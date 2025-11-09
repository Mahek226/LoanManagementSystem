# Loan Screening Frontend - Angular Application

## Quick Start

1. **Install Dependencies:**
```bash
npm install
```

2. **Configure API URL:**
Edit `src/environments/environment.ts`:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8000/api'
};
```

3. **Run Development Server:**
```bash
ng serve
```

4. **Access Application:**
Open http://localhost:4200

## Features

- User Registration & Login
- Loan Application Submission
- Document Upload with OCR
- Dashboard for all user roles
- Real-time notifications

## Project Structure

```
src/
├── app/
│   ├── components/
│   │   ├── login/
│   │   ├── register/
│   │   ├── dashboard/
│   │   ├── loan-application/
│   │   └── document-upload/
│   ├── services/
│   │   └── auth.service.ts
│   └── app.component.ts
├── environments/
│   └── environment.ts
└── styles.css
```

## Build for Production

```bash
ng build --configuration production
```

Output will be in `dist/loan-screening-frontend/`


