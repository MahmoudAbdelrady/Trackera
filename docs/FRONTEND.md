# Trackera Frontend

## Tech Stack

- **Framework**: React 19 with TypeScript
- **Build Tool**: Vite 7
- **UI Library**: Ant Design (antd)
- **State Management**: Zustand (auth state) + TanStack Query (server state)
- **Routing**: React Router DOM v7
- **Forms**: Formik + Yup validation
- **HTTP Client**: Axios with interceptors
- **Styling**: SCSS Modules
- **Icons**: Lucide React
- **Notifications**: react-hot-toast

---

## Folder Structure

```
src/
├── components/      # Reusable UI components
├── layouts/         # App & Auth layout wrappers
├── pages/           # Route-level page components
├── routes/          # Route guards (PrivateRoute, GuestRoute)
├── shared/          # Shared utilities
│   ├── axios/       # Axios instance & interceptors
│   ├── contexts/    # React contexts (SSE)
│   ├── hooks/       # Custom hooks
│   ├── styles/      # SCSS variables & mixins
│   ├── types/       # TypeScript types
│   └── yup-schemas/ # Form validation schemas
├── state/           # State management
│   ├── api/         # API call functions
│   ├── queries/     # React Query client
│   └── store/       # Zustand stores
└── utils/           # Helper functions
```

---

## Routing & State Management

### Route Guards

- `PrivateRoute`: Protects authenticated routes, redirects to `/login` if unauthenticated
- `GuestRoute`: Protects auth pages (login, sign-up), redirects to `/` if already authenticated

### Routes

| Path                          | Component            | Access  |
| ----------------------------- | -------------------- | ------- |
| `/`                           | Home                 | Private |
| `/login`                      | Login                | Guest   |
| `/sign-up`                    | SignUp               | Guest   |
| `/forgot-password`            | ForgotPassword       | Guest   |
| `/change-password`            | ChangePassword       | Public  |
| `/security-verification`      | SecurityVerification | Public  |
| `/worklog-details/:worklogId` | WorklogDetails       | Private |
| `/jira-tasks`                 | JiraTasks            | Private |
| `/settings`                   | Settings             | Private |
| `/oauth/:provider/callback`   | OAuthCallback        | Public  |
| `/privacy-policy`             | PrivacyPolicy        | Public  |

### State Management

- **Auth State (Zustand)**: Manages `isAuthenticated`, `isLoading`, `authChecked`, and error states
- **Server State (TanStack Query)**: Handles API data fetching, caching, and synchronization

---

## Styling & Theming

### Structure

- SCSS modules per component (`.module.scss`)
- Shared variables in `shared/styles/_colors.scss`
- Shared mixins in `shared/styles/_mixins.scss`

### Color Palette

| Variable           | Color     | Usage           |
| ------------------ | --------- | --------------- |
| `$mainColor`       | `#2563eb` | Primary blue    |
| `$greenColor`      | `#16a34a` | Success states  |
| `$orangeColor`     | `#d97706` | Warning states  |
| `$redColor`        | `#dc2626` | Error states    |
| `$backgroundColor` | `#f9fafb` | Page background |
| `$borderColor`     | `#e5e7eb` | Borders         |

### Conventions

- Component-scoped styles using CSS Modules
- Shared transitions: `$mainTransition: 0.3s`
- Consistent box shadows for cards and auth forms

---

## Key Components

### Auth Components (`components/auth/`)

- `AuthForm`: Reusable form wrapper for login/signup
- `AuthFooter`: Footer links for auth pages
- `AuthResult`: Success/error result display
- `OAuthBtns`: Google OAuth button

### Layout Components (`layouts/`)

- `AppLayout`: Main app layout with sidebar
- `AuthLayout`: Centered layout for auth pages

### Common Components (`components/`)

- `Sidebar`: Main navigation sidebar
- `LoadingSpinner`: Loading state indicator
- `StatusBadge`: Status display badges
- `TrackeraTable`: Reusable data table
- `EmptyState`: Empty data placeholder
- `CollapsibleSection`: Expandable content sections

### Worklog Components (`components/worklogs/`)

- `WorklogInfo`: Worklog details display
- `WorklogStatusCard`: Status summary cards
- `WorklogActionButtons`: Action button group
- `SearchFilter`: Filter and search controls
- `DeleteWarning`: Delete confirmation modal

### Settings Components (`components/settings/`)

- `LinkedAccount`: OAuth account connections
- `UserPreference`: User settings controls

---

## API Layer

### Structure

- `shared/axios/request-instance.ts`: Configured Axios instance
- `shared/axios/interceptors/`: Request/response interceptors
- `state/api/`: API functions organized by domain (auth, user, jira, worklog)

### Error Handling

- Centralized error categorization via `utils/categorizeAxiosError.ts`
- Toast notifications for user feedback via `utils/toast-handler/`

---

## Form Validation

Yup schemas organized by domain in `shared/yup-schemas/`:

- `auth/`: Login, signup, password validation
- `worklog/`: Worklog creation and editing
- `email.ts`: Email format validation

Formik utilities in `utils/formik/`:

- `getFormikErrors.ts`: Error extraction
- `getFormikFieldProps.ts`: Field prop helpers
- `getFormikValidationResult.ts`: Validation helpers

---

## TypeScript Types (Backend DTOs)

Types mirroring backend DTOs are organized in `shared/types/`:

### File Structure

| File         | Description                                       |
| ------------ | ------------------------------------------------- |
| `index.ts`   | Central export for all types and constants        |
| `global.ts`  | Shared types (pagination, error types, operators) |
| `auth.ts`    | Auth-related types and OAuth provider info        |
| `worklog.ts` | Worklog, task, and entry DTOs                     |
| `jira.ts`    | Jira integration types (tasks, sites)             |

### Key Types

**Global Types:**

- `PaginatedResponse<T>`: Generic paginated API response wrapper
- `AxiosErrorType`: Error categorization (`"SERVER"` | `"AUTH"` | `"FORBIDDEN"`)
- `OPERATORS`: Filter operators for search functionality

**Auth Types:**

- `LoginFormFields`, `SignUpFormFields`: Form input types
- `UserInfo`: User profile data
- `OAuthAccount`, `OAuthProvider`: OAuth integration types

**Worklog Types:**

- `Worklog`: Main worklog entity
- `WorklogTask`: Task within a worklog
- `WorklogEntry`: Time entry within a task
- `WorklogStatusType`: Sync status (`SYNCED`, `PARTIALLY`, `NOT_SYNCED`, etc.)
- `WorklogEvaluationType`: Performance rating (`EXCELLENT`, `GOOD`, `MODERATE`, `POOR`)
- `SyncPayload`: Jira sync request payload

**Jira Types:**

- `JiraTask`: Task fetched from Jira
- `JiraSite`: Connected Jira site info

### Usage

Import types from the central index:

```typescript
import type { Worklog, PaginatedResponse } from "@/shared/types";
import { WORKLOG_STATUS, OPERATORS } from "@/shared/types";
```

---

## Development

### Scripts

```bash
pnpm dev      # Start development server
pnpm build    # Build for production
pnpm lint     # Run ESLint
pnpm preview  # Preview production build
```

### Adding New Features

1. Create page component in `pages/`
2. Add route in `App.tsx`
3. Create reusable components in `components/`
4. Add API functions in `state/api/`
5. Create validation schemas in `shared/yup-schemas/`

### Integrating with a New API

1. Define TypeScript types/interfaces in `shared/types/` matching backend DTOs
2. Export new types from `shared/types/index.ts`
3. Create API functions in `state/api/` using the new types
4. Add validation schemas if forms are involved
5. Use types in components for type-safe data handling
