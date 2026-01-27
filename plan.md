# Buurman - Property Management for Small Landlords

A modern dashboard application to help small landlords manage their rental properties, tenants, and finances with ease.

---

## Phase 1: Foundation & Core Features (MVP)

### 1.1 Project Setup & Infrastructure

**Tasks:**
1. Initialize Spring Boot project with Maven
   - Spring Web, Spring Security, Spring Data JPA, JOOQ
   - Flyway, Lombok, MapStruct, Validation
   - Dependencies: PostgreSQL driver, AWS SDK for S3, Micrometer
2. Initialize React project with Vite
   - TypeScript, ESLint, Prettier configuration
   - Dependencies: React Router, React Query, Axios, Formik, Yup, Tailwind CSS, Bootstrap 5
3. Setup Docker Compose for local development
   - PostgreSQL container with initialization scripts
   - LocalStack container for S3 simulation
   - Keycloak container with realm configuration
4. Configure Flyway migrations structure
   - V001__create_base_schema.sql (teams, users, team_members)
   - V002__create_properties.sql
   - V003__create_documents.sql
   - V004__create_audit_log.sql

**Acceptance Criteria:**
- `docker-compose up` starts all services successfully
- Spring Boot app connects to PostgreSQL and runs migrations
- React dev server starts with `yarn dev`
- Health check endpoints respond: `/actuator/health`, `/actuator/info`
- CORS configured for local development (React -> Spring Boot)

**API Endpoints:**
- GET /api/health - Health check
- GET /api/info - Version and build info

---

### 1.2 Authentication & Team Access

**Tasks:**
1. Configure Keycloak realm and client
   - Create "buurman" realm
   - Configure client for frontend SPA
   - Setup roles: TEAM_ADMIN, TEAM_EDITOR, TEAM_VIEWER
2. Implement Spring Security with JWT
   - JWT token validation filter
   - Extract team_id and user_id from token claims
   - SecurityContextHolder with custom UserPrincipal
3. Implement user registration flow
   - POST /api/auth/register - Create user in Keycloak, create team
   - Automatically assign TEAM_ADMIN role to first team member
4. Implement login flow (delegated to Keycloak)
   - Frontend redirects to Keycloak login
   - Exchange authorization code for access token
5. Implement team invitation system
   - POST /api/teams/{teamId}/invitations - Create invitation (ADMIN only)
   - GET /api/invitations/{token}/accept - Accept invitation, join team
   - Team member CRUD operations
6. Frontend authentication UI
   - Login page with redirect to Keycloak
   - Registration form with team creation
   - User profile page (view/edit name, email)
   - Team management page (list members, invite, change roles - ADMIN only)

**Acceptance Criteria:**
- User can register and automatically create a team
- User can login via Keycloak and receive JWT token
- Token includes team_id and role claims
- All API requests validate JWT and extract team context
- Admin can invite users by email
- Invited users can accept invitation and join team
- EDITOR and VIEWER cannot access team management endpoints
- Frontend stores token in memory/secure storage
- Token refresh implemented before expiry

**API Endpoints:**
- POST /api/auth/register - Register user and create team
- GET /api/auth/me - Get current user info
- PUT /api/auth/me - Update current user profile
- GET /api/teams/current - Get current user's team info
- GET /api/teams/{teamId}/members - List team members (requires membership)
- POST /api/teams/{teamId}/invitations - Create invitation (ADMIN only)
- GET /api/invitations/{token} - View invitation details
- POST /api/invitations/{token}/accept - Accept invitation
- DELETE /api/teams/{teamId}/members/{memberId} - Remove team member (ADMIN only)
- PUT /api/teams/{teamId}/members/{memberId}/role - Update member role (ADMIN only)

**Database Migrations:**
- V001__create_base_schema.sql:
```sql
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    invited_at TIMESTAMP NOT NULL DEFAULT NOW(),
    invited_by UUID REFERENCES users(id),
    joined_at TIMESTAMP,
    UNIQUE(team_id, user_id)
);

CREATE INDEX idx_team_members_team ON team_members(team_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);
```

---

### 1.3 Property Management

**Tasks:**
1. Implement backend property service
   - PropertyService with CRUD operations
   - Automatic team_id filtering on all queries
   - ULID generation for business_id
   - MapStruct DTOs for request/response
2. Implement property REST endpoints
   - CRUD operations with validation
   - Image upload to S3 with metadata storage
   - Document attachment handling
3. Frontend property pages
   - Property list page with cards (grid view)
   - Property detail page with tabs (info, photos, documents)
   - Property form (create/edit) with validation
   - Photo gallery with upload/delete
   - Document list with upload/download/delete

**Acceptance Criteria:**
- User can create property with all required fields
- Address fields validated (non-empty strings)
- Square meters must be positive number
- Status enum validated
- Photos uploaded to S3, URLs stored in documents table
- User can only see/edit properties in their team
- Property list shows status badge (colored)
- Soft delete implemented (deleted_at column)
- Photos display in gallery with lightbox
- Documents show file size, upload date, uploaded by

**API Endpoints:**
- POST /api/properties - Create property
- GET /api/properties - List all properties (team-filtered, paginated)
- GET /api/properties/{id} - Get property details
- PUT /api/properties/{id} - Update property
- DELETE /api/properties/{id} - Soft delete property
- GET /api/properties/{id}/documents - List property documents
- POST /api/properties/{id}/documents - Upload document
- DELETE /api/properties/{id}/documents/{docId} - Delete document
- GET /api/documents/{id}/download - Download document (signed S3 URL)

**Request/Response Models:**
```typescript
// CreatePropertyRequest
{
  street: string;
  city: string;
  postalCode: string;
  country: string;
  bedrooms: number;
  bathrooms: number;
  squareMeters: number;
  propertyType: 'APARTMENT' | 'HOUSE' | 'STUDIO' | 'COMMERCIAL';
  status: 'VACANT' | 'OCCUPIED' | 'MAINTENANCE' | 'UNAVAILABLE';
}

// PropertyResponse
{
  id: string;
  businessId: string;
  address: {
    street: string;
    city: string;
    postalCode: string;
    country: string;
  };
  specifications: {
    bedrooms: number;
    bathrooms: number;
    squareMeters: number;
    propertyType: string;
  };
  status: string;
  documentCount: number;
  currentTenant?: TenantSummary;
  activeContract?: ContractSummary;
  createdAt: string;
  updatedAt: string;
}

// DocumentResponse
{
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  title: string;
  notes: string;
  uploadedBy: string;
  uploadedAt: string;
  downloadUrl: string; // pre-signed S3 URL
}
```

**Database Migrations:**
- V002__create_properties.sql:
```sql
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Netherlands',
    bedrooms INTEGER,
    bathrooms INTEGER,
    square_meters DECIMAL(10,2),
    property_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VACANT',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,
    UNIQUE(team_id, business_id)
);

CREATE INDEX idx_properties_team ON properties(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_properties_status ON properties(team_id, status) WHERE deleted_at IS NULL;
```

- V003__create_documents.sql:
```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    file_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    title VARCHAR(255),
    notes TEXT,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_documents_entity ON documents(team_id, entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_team ON documents(team_id) WHERE deleted_at IS NULL;
```

---

### 1.4 Dashboard Overview

**Tasks:**
1. Implement dashboard statistics service
   - Calculate total properties count
   - Calculate occupied units count (properties with active contract)
   - Calculate monthly income (sum of active contract rents)
   - Get recent activities from audit log (last 10)
2. Implement dashboard REST endpoint
   - GET /api/dashboard/stats
   - GET /api/dashboard/recent-activities
3. Frontend dashboard page
   - Sidebar navigation component (responsive, collapsible)
   - Summary cards with icons (properties, occupancy, income)
   - Recent activities timeline
   - Property status distribution chart (pie/donut)
   - Quick actions buttons (Add Property, Add Tenant)

**Acceptance Criteria:**
- Dashboard loads within 1 second
- Statistics accurate and real-time
- Recent activities show user-friendly descriptions
- Sidebar navigates to all main sections
- Responsive design works on mobile (hamburger menu)
- Loading states shown while fetching data
- Error states handled gracefully

**API Endpoints:**
- GET /api/dashboard/stats - Get dashboard statistics
- GET /api/dashboard/recent-activities - Get recent audit trail (paginated)

**Response Models:**
```typescript
// DashboardStatsResponse
{
  totalProperties: number;
  occupiedUnits: number;
  vacantUnits: number;
  maintenanceUnits: number;
  monthlyIncome: {
    amount: number;
    currency: string;
  };
  occupancyRate: number; // percentage
}

// RecentActivityResponse
{
  id: string;
  entityType: string;
  entityId: string;
  entityName: string; // e.g., property address
  action: 'CREATE' | 'UPDATE' | 'DELETE';
  userName: string;
  timestamp: string;
  description: string; // e.g., "Created property at Main St 123"
}
```

---

## Phase 2: Tenant & Contract Management

### 2.1 Tenant Registry

**Tasks:**
1. Implement tenant backend service
   - TenantService with CRUD operations
   - Validation for tax_number and id_number formats
   - Email validation
2. Implement tenant REST endpoints
   - CRUD operations for tenants
   - Search/filter capabilities (by name, email, property)
3. Implement property-tenant linking
   - Link tenant to property (updates current_property_id)
   - Unlink tenant from property
   - Record all actions in PropertyTenantHistory
4. Frontend tenant pages
   - Tenant list page with search/filter
   - Tenant detail page showing:
     - Personal information
     - Current property
     - Rental history
     - Active contracts
     - Documents
   - Tenant form (create/edit)
   - Property assignment interface

**Acceptance Criteria:**
- Email format validated
- Tax number and ID validated (optional but format-checked if provided)
- Tenant can be linked to only one property at a time
- Linking/unlinking recorded in history with timestamp and user
- User can view complete rental history for a tenant
- Soft delete implemented
- Search works across name, email, phone

**API Endpoints:**
- POST /api/tenants - Create tenant
- GET /api/tenants - List tenants (paginated, searchable)
- GET /api/tenants/{id} - Get tenant details
- PUT /api/tenants/{id} - Update tenant
- DELETE /api/tenants/{id} - Soft delete tenant
- POST /api/tenants/{id}/link-property - Link tenant to property
- POST /api/tenants/{id}/unlink-property - Unlink tenant from property
- GET /api/tenants/{id}/history - Get property history

**Request/Response Models:**
```typescript
// CreateTenantRequest
{
  name: string;
  email: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
}

// TenantResponse
{
  id: string;
  businessId: string;
  name: string;
  email: string;
  phone: string;
  taxNumber: string;
  idNumber: string;
  currentProperty?: PropertySummary;
  createdAt: string;
  updatedAt: string;
}

// PropertyTenantHistoryResponse
{
  id: string;
  property: PropertySummary;
  movedInAt: string;
  movedOutAt?: string;
  actionType: 'LINKED' | 'UNLINKED';
  performedBy: string;
  performedAt: string;
}
```

**Database Migrations:**
- V005__create_tenants.sql:
```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    tax_number VARCHAR(50),
    id_number VARCHAR(50),
    current_property_id UUID REFERENCES properties(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,
    UNIQUE(team_id, business_id)
);

CREATE TABLE property_tenant_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    moved_in_at TIMESTAMP,
    moved_out_at TIMESTAMP,
    action_type VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL REFERENCES users(id),
    performed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_team ON tenants(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_property ON tenants(current_property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenant_history_tenant ON property_tenant_history(tenant_id);
CREATE INDEX idx_tenant_history_property ON property_tenant_history(property_id);
```

---

### 2.2 Rental Agreements

**Tasks:**
1. Implement contract backend service
   - ContractService with CRUD operations
   - Validation: start_date < end_date (if end_date provided)
   - Automatic status calculation based on dates
   - Contract overlap detection for same property
2. Implement rent schedule service
   - Future rent changes linked to contracts
   - Effective date validation (must be after contract start)
3. Implement contract REST endpoints
   - CRUD operations for contracts
   - Rent schedule management
   - Contract status transitions (draft -> active -> expired/terminated)
4. Frontend contract pages
   - Contract list page (filterable by status, property, tenant)
   - Contract detail page showing:
     - Basic contract info
     - Linked property and tenant
     - Rent schedule (current and future)
     - Payment history
     - Attached documents
   - Contract form (create/edit)
   - Rent schedule form (add future rent change)

**Acceptance Criteria:**
- Cannot create overlapping contracts for same property
- Status automatically calculated: ACTIVE if today between start/end, EXPIRED if past end
- Rent schedule changes validated (effective_from >= contract start_date)
- Current rent calculated from schedule based on current date
- Contract can only be linked to one property and one tenant
- Contract changes recorded in audit log
- VIEWER role can view but not modify contracts

**API Endpoints:**
- POST /api/contracts - Create contract
- GET /api/contracts - List contracts (filterable, paginated)
- GET /api/contracts/{id} - Get contract details
- PUT /api/contracts/{id} - Update contract
- DELETE /api/contracts/{id} - Soft delete contract (ADMIN only)
- PUT /api/contracts/{id}/status - Update contract status
- POST /api/contracts/{id}/rent-schedule - Add future rent change
- GET /api/contracts/{id}/rent-schedule - Get rent schedule
- DELETE /api/contracts/{id}/rent-schedule/{scheduleId} - Delete future rent change
- GET /api/properties/{propertyId}/contracts - Get contracts for property
- GET /api/tenants/{tenantId}/contracts - Get contracts for tenant

**Request/Response Models:**
```typescript
// CreateContractRequest
{
  propertyId: string;
  tenantId: string;
  startDate: string; // ISO date
  endDate?: string; // ISO date, nullable for indefinite
  rentAmount: number;
  currency: string;
  paymentDayOfMonth: number; // 1-31
  paymentFrequency: 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
}

// ContractResponse
{
  id: string;
  businessId: string;
  property: PropertySummary;
  tenant: TenantSummary;
  startDate: string;
  endDate?: string;
  currentRent: {
    amount: number;
    currency: string;
  };
  paymentDayOfMonth: number;
  paymentFrequency: string;
  status: 'DRAFT' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED';
  createdAt: string;
  updatedAt: string;
}

// RentScheduleRequest
{
  effectiveFrom: string; // ISO date
  rentAmount: number;
  currency: string;
}

// RentScheduleResponse
{
  id: string;
  effectiveFrom: string;
  rentAmount: number;
  currency: string;
  createdBy: string;
  createdAt: string;
}
```

**Database Migrations:**
- V006__create_contracts.sql:
```sql
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    start_date DATE NOT NULL,
    end_date DATE,
    rent_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    payment_day_of_month INTEGER NOT NULL,
    payment_frequency VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,
    UNIQUE(team_id, business_id),
    CHECK (payment_day_of_month >= 1 AND payment_day_of_month <= 31),
    CHECK (end_date IS NULL OR end_date > start_date)
);

CREATE TABLE contract_rent_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    effective_from DATE NOT NULL,
    rent_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(contract_id, effective_from)
);

CREATE INDEX idx_contracts_team ON contracts(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_property ON contracts(property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_tenant ON contracts(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_status ON contracts(team_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_rent_schedule_contract ON contract_rent_schedule(contract_id);
```

---

## Phase 3: Financial Tracking

### 3.1 Rent Payment Tracking

**Tasks:**
1. Implement payment backend service
   - PaymentService with CRUD operations
   - Automatic status calculation (OVERDUE if past due_date and not paid)
   - Payment reminder logic
2. Implement payment REST endpoints
   - CRUD operations for payments
   - Bulk payment generation (create payments for all active contracts)
   - Mark payment as paid/pending
   - Attach proof of payment and generate receipt
3. Frontend payment pages
   - Payment list page (filterable by status, contract, date range)
   - Payment detail page showing:
     - Payment info
     - Contract and tenant details
     - Proof of payment documents
     - Payment receipt
   - Payment form (create/edit)
   - Mark as paid interface with proof upload
   - Payment calendar view

**Acceptance Criteria:**
- Payments automatically calculated from contracts
- Status changes from PENDING to OVERDUE if due_date passes
- Proof of payment stored as document
- Receipt PDF generated when payment marked as paid
- Reminders sent X days before due_date (configurable)
- Bulk payment generation creates one payment per active contract
- Cannot delete paid payments, only cancel
- Payment history shows all status transitions

**API Endpoints:**
- POST /api/payments - Create manual payment
- POST /api/payments/bulk-generate - Generate payments for all active contracts
- GET /api/payments - List payments (filterable, paginated)
- GET /api/payments/{id} - Get payment details
- PUT /api/payments/{id} - Update payment
- DELETE /api/payments/{id} - Cancel payment (if not paid)
- PUT /api/payments/{id}/mark-paid - Mark payment as paid
- POST /api/payments/{id}/proof - Attach proof of payment
- GET /api/payments/{id}/receipt - Download payment receipt PDF
- GET /api/contracts/{contractId}/payments - Get payments for contract
- GET /api/payments/overdue - Get all overdue payments

**Request/Response Models:**
```typescript
// CreatePaymentRequest
{
  contractId: string;
  amount: number;
  currency: string;
  dueDate: string; // ISO date
  notes?: string;
}

// PaymentResponse
{
  id: string;
  businessId: string;
  contract: ContractSummary;
  tenant: TenantSummary;
  property: PropertySummary;
  amount: number;
  currency: string;
  paymentDate?: string;
  dueDate: string;
  status: 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  notes: string;
  proofOfPayment?: DocumentResponse;
  receipt?: DocumentResponse;
  createdAt: string;
  updatedAt: string;
}

// MarkPaidRequest
{
  paymentDate: string; // ISO date
  notes?: string;
}

// BulkGenerateRequest
{
  forMonth: string; // YYYY-MM
}
```

**Database Migrations:**
- V007__create_payments.sql:
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    payment_date DATE,
    due_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,
    UNIQUE(team_id, business_id)
);

CREATE INDEX idx_payments_team ON payments(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payments_contract ON payments(contract_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payments_status ON payments(team_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_payments_due_date ON payments(team_id, due_date) WHERE deleted_at IS NULL;
```

---

### 3.2 Expense Management

**Tasks:**
1. Implement expense backend service
   - ExpenseService with CRUD operations
   - Category validation
   - Property association required
2. Implement expense REST endpoints
   - CRUD operations for expenses
   - Category-based filtering
   - Date range filtering
3. Frontend expense pages
   - Expense list page (filterable by category, property, date range)
   - Expense detail page showing:
     - Expense info
     - Linked property
     - Attached invoices/receipts
   - Expense form (create/edit)
   - Category selection with icons
   - Bulk expense import (CSV)

**Acceptance Criteria:**
- Expense must be linked to a property
- Category is required and validated against enum
- Amount must be positive
- Expense date required
- Receipts/invoices stored as documents
- Can attach multiple documents to one expense
- Export to CSV functionality
- Monthly expense reports generated

**API Endpoints:**
- POST /api/expenses - Create expense
- GET /api/expenses - List expenses (filterable, paginated)
- GET /api/expenses/{id} - Get expense details
- PUT /api/expenses/{id} - Update expense
- DELETE /api/expenses/{id} - Soft delete expense
- GET /api/properties/{propertyId}/expenses - Get expenses for property
- GET /api/expenses/summary - Get expense summary by category/period
- POST /api/expenses/import - Bulk import from CSV
- GET /api/expenses/export - Export to CSV

**Request/Response Models:**
```typescript
// CreateExpenseRequest
{
  propertyId: string;
  category: 'MAINTENANCE' | 'REPAIR' | 'UTILITY' | 'TAX' | 'INSURANCE' | 'OTHER';
  amount: number;
  currency: string;
  expenseDate: string; // ISO date
  description: string;
}

// ExpenseResponse
{
  id: string;
  businessId: string;
  property: PropertySummary;
  category: string;
  amount: number;
  currency: string;
  expenseDate: string;
  description: string;
  documents: DocumentResponse[];
  createdAt: string;
  updatedAt: string;
}

// ExpenseSummaryResponse
{
  period: string; // YYYY-MM
  byCategory: {
    category: string;
    total: number;
    count: number;
  }[];
  grandTotal: number;
  currency: string;
}
```

**Database Migrations:**
- V008__create_expenses.sql:
```sql
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    expense_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,
    UNIQUE(team_id, business_id),
    CHECK (amount > 0)
);

CREATE INDEX idx_expenses_team ON expenses(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_property ON expenses(property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_category ON expenses(team_id, category) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_date ON expenses(team_id, expense_date) WHERE deleted_at IS NULL;
```

---

### 3.3 Document Management Enhancement

**Tasks:**
1. Enhance document service
   - Generic document attachment to any entity
   - Document preview generation for common formats (PDF, images)
   - Document search across all entities
2. Implement advanced document endpoints
   - Full-text search across document titles/notes
   - Filter by entity type
   - Bulk download (zip archive)
3. Frontend document management
   - Centralized document library page
   - Document preview modal (PDF viewer, image viewer)
   - Drag-and-drop upload
   - Bulk operations (download, delete)
   - Document tagging system

**Acceptance Criteria:**
- Documents searchable by title, notes, file name
- Preview works for PDF and images (JPG, PNG)
- Maximum file size enforced (configurable, default 50MB)
- File type whitelist enforced
- Bulk download creates zip with organized folder structure
- Deleted documents removed from S3 (async job)
- Document count limits per entity (optional)

**API Endpoints:**
- GET /api/documents - List all documents (searchable, filterable)
- GET /api/documents/{id} - Get document metadata
- GET /api/documents/{id}/preview - Get preview URL (if available)
- DELETE /api/documents/{id} - Delete document
- POST /api/documents/bulk-download - Create zip archive for download
- GET /api/documents/search - Full-text search across documents

---

## Phase 4: Financial Reporting & Analytics

### 4.1 Financial Overview

**Tasks:**
1. Implement reporting service
   - Income calculation (payments received)
   - Expense calculation (by category, property)
   - Profit/loss calculation (income - expenses)
   - Period comparison (month-over-month, year-over-year)
2. Implement reporting REST endpoints
   - Financial summary by period
   - Per-property financial statements
   - Income vs expense trends
3. Frontend reporting pages
   - Financial overview dashboard
   - Summary cards (total income, total expenses, net profit)
   - Period selector (month, quarter, year, custom range)
   - Property comparison table

**Acceptance Criteria:**
- Reports calculated in real-time from payment and expense data
- Period filters work correctly (inclusive dates)
- Currency conversion handled (if multi-currency)
- Reports cached for performance (5-minute TTL)
- Export to PDF and Excel
- Comparison periods clearly labeled
- Negative profit shown in red

**API Endpoints:**
- GET /api/reports/financial-overview - Overall financial summary
- GET /api/reports/properties/{propertyId}/financial - Property financial statement
- GET /api/reports/income-vs-expenses - Time-series data for charts
- GET /api/reports/export - Export report to PDF or Excel

**Request/Response Models:**
```typescript
// FinancialOverviewRequest
{
  startDate: string; // ISO date
  endDate: string; // ISO date
  propertyIds?: string[]; // filter by properties
  currency?: string; // default to EUR
}

// FinancialOverviewResponse
{
  period: {
    startDate: string;
    endDate: string;
  };
  income: {
    total: number;
    byProperty: PropertyFinancialSummary[];
  };
  expenses: {
    total: number;
    byCategory: CategoryExpenseSummary[];
    byProperty: PropertyFinancialSummary[];
  };
  netProfit: number;
  currency: string;
}

// PropertyFinancialSummary
{
  property: PropertySummary;
  income: number;
  expenses: number;
  netProfit: number;
  occupancyDays: number; // days occupied in period
}

// CategoryExpenseSummary
{
  category: string;
  total: number;
  count: number;
  percentage: number; // of total expenses
}
```

---

### 4.2 Charts & Visualizations

**Tasks:**
1. Implement chart data endpoints
   - Income trend (monthly, for last 12 months)
   - Expense breakdown (pie chart data)
   - Property comparison (bar chart data)
   - Occupancy rate trend
2. Frontend chart components
   - Line chart for income trend (Chart.js or Recharts)
   - Pie/Donut chart for expense categories
   - Bar chart for property comparison
   - Occupancy rate gauge/progress bar
3. Dashboard integration
   - Add charts to main dashboard
   - Interactive tooltips
   - Responsive chart sizing

**Acceptance Criteria:**
- Charts render correctly on all screen sizes
- Data updates when period changes
- Tooltips show detailed information
- Charts accessible (proper alt text, keyboard navigation)
- Loading states shown while fetching data
- Empty states handled (no data available)
- Color scheme consistent with app theme

**API Endpoints:**
- GET /api/reports/charts/income-trend - Income trend data
- GET /api/reports/charts/expense-breakdown - Expense breakdown data
- GET /api/reports/charts/property-comparison - Property comparison data
- GET /api/reports/charts/occupancy-trend - Occupancy rate trend data

**Response Models:**
```typescript
// IncomeTrendResponse
{
  dataPoints: {
    period: string; // YYYY-MM
    income: number;
    expenses: number;
    netProfit: number;
  }[];
  currency: string;
}

// ExpenseBreakdownResponse
{
  categories: {
    name: string;
    value: number;
    color: string;
  }[];
  total: number;
  currency: string;
}

// PropertyComparisonResponse
{
  properties: {
    property: PropertySummary;
    income: number;
    expenses: number;
    netProfit: number;
  }[];
  currency: string;
}

// OccupancyTrendResponse
{
  dataPoints: {
    period: string; // YYYY-MM
    occupancyRate: number; // percentage
    totalUnits: number;
    occupiedUnits: number;
  }[];
}
```

---

### 4.3 Detailed Reports & Export

**Tasks:**
1. Implement report generation service
   - PDF generation using iText or similar
   - Excel generation using Apache POI
   - Report templates (income statement, expense report, property report)
2. Implement transaction history endpoint
   - Combined view of payments and expenses
   - Filterable by type, property, date range
   - Sortable columns
3. Frontend report pages
   - Transaction history table (paginated, sortable)
   - Advanced filters (date range, type, property, category)
   - Export buttons (PDF, Excel, CSV)
   - Print-friendly report view
   - Tax report generation (annual summary)

**Acceptance Criteria:**
- Transaction history shows both income and expenses
- Filtering works for all criteria combinations
- PDF reports professionally formatted with logo
- Excel exports include formulas and formatting
- CSV exports properly quoted and escaped
- Tax reports include all required information
- Reports generated asynchronously for large datasets
- Download link expires after 24 hours
- User notified when report ready (if async)

**API Endpoints:**
- GET /api/reports/transactions - Transaction history (paginated)
- POST /api/reports/generate - Generate report (async)
- GET /api/reports/{reportId}/status - Check report generation status
- GET /api/reports/{reportId}/download - Download generated report
- GET /api/reports/tax-summary - Annual tax summary

**Request/Response Models:**
```typescript
// TransactionHistoryRequest
{
  startDate: string;
  endDate: string;
  type?: 'INCOME' | 'EXPENSE' | 'ALL';
  propertyIds?: string[];
  categories?: string[]; // expense categories
  page: number;
  size: number;
  sort?: string; // e.g., "date,desc"
}

// TransactionResponse
{
  id: string;
  date: string;
  type: 'INCOME' | 'EXPENSE';
  description: string;
  property: PropertySummary;
  category?: string; // for expenses
  amount: number;
  currency: string;
  documents: DocumentResponse[];
}

// GenerateReportRequest
{
  reportType: 'INCOME_STATEMENT' | 'EXPENSE_REPORT' | 'PROPERTY_REPORT' | 'TAX_SUMMARY';
  format: 'PDF' | 'EXCEL';
  startDate: string;
  endDate: string;
  propertyIds?: string[];
  includeDocuments?: boolean; // embed document links
}

// ReportStatusResponse
{
  reportId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress?: number; // percentage
  downloadUrl?: string; // when completed
  expiresAt?: string; // when completed
  error?: string; // when failed
}

// TaxSummaryResponse
{
  year: number;
  totalIncome: number;
  totalExpenses: number;
  netIncome: number;
  expensesByCategory: CategoryExpenseSummary[];
  properties: PropertyFinancialSummary[];
  currency: string;
}
```

**Database Migrations:**
- V009__create_reports.sql:
```sql
CREATE TABLE generated_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id),
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL,
    parameters JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    file_key VARCHAR(500),
    error TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    CHECK (progress >= 0 AND progress <= 100)
);

CREATE INDEX idx_reports_team ON generated_reports(team_id);
CREATE INDEX idx_reports_status ON generated_reports(status);
CREATE INDEX idx_reports_expires ON generated_reports(expires_at) WHERE status = 'COMPLETED';
```

---

## Technical Implementation Details

### Backend Architecture

**Package Structure:**
```
com.buurman
├── config/
│   ├── SecurityConfig.java
│   ├── S3Config.java
│   ├── JooqConfig.java
│   └── SwaggerConfig.java
├── controller/
│   ├── AuthController.java
│   ├── PropertyController.java
│   ├── TenantController.java
│   ├── ContractController.java
│   ├── PaymentController.java
│   ├── ExpenseController.java
│   ├── DocumentController.java
│   ├── DashboardController.java
│   └── ReportController.java
├── service/
│   ├── auth/
│   │   ├── AuthService.java
│   │   ├── KeycloakService.java
│   │   └── TeamService.java
│   ├── property/
│   │   └── PropertyService.java
│   ├── tenant/
│   │   └── TenantService.java
│   ├── contract/
│   │   ├── ContractService.java
│   │   └── RentScheduleService.java
│   ├── financial/
│   │   ├── PaymentService.java
│   │   ├── ExpenseService.java
│   │   └── ReportService.java
│   ├── document/
│   │   ├── DocumentService.java
│   │   └── S3StorageService.java
│   └── audit/
│       └── AuditService.java
├── repository/
│   ├── TeamRepository.java
│   ├── PropertyRepository.java
│   ├── TenantRepository.java
│   ├── ContractRepository.java
│   ├── PaymentRepository.java
│   ├── ExpenseRepository.java
│   └── DocumentRepository.java
├── domain/
│   ├── Team.java
│   ├── Property.java
│   ├── Tenant.java
│   ├── Contract.java
│   ├── Payment.java
│   ├── Expense.java
│   └── Document.java
├── dto/
│   ├── request/
│   └── response/
├── mapper/
│   ├── PropertyMapper.java
│   ├── TenantMapper.java
│   └── ...
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── UserPrincipal.java
│   └── TeamContextHolder.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── ValidationException.java
└── util/
    ├── UlidGenerator.java
    └── DateUtils.java
```

**Key Components:**

1. **SecurityConfig.java**
   - Configure Spring Security with JWT
   - Define security filter chain
   - Exempt health endpoints from authentication
   - Configure CORS for frontend origin

2. **JwtAuthenticationFilter.java**
   - Extract JWT from Authorization header
   - Validate token with Keycloak public key
   - Extract user_id and team_id from claims
   - Set SecurityContext with UserPrincipal

3. **TeamContextHolder.java**
   - Thread-local storage for current team_id
   - Populated by JwtAuthenticationFilter
   - Used by services to filter data automatically

4. **Base Repository Pattern**
   - All repositories extend TeamAwareRepository
   - Automatically append WHERE team_id = ? to all queries
   - Prevent cross-team data access

5. **Audit Interceptor**
   - JPA Entity listeners for @PrePersist, @PreUpdate
   - Populate created_by, updated_by, created_at, updated_at
   - Insert audit log entries on entity changes

**Application Properties:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/buurman
    username: buurman
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    baseline-on-migrate: true

aws:
  s3:
    endpoint: ${S3_ENDPOINT:http://localhost:4566} # LocalStack
    region: eu-west-1
    bucket: buurman-documents
    access-key: ${S3_ACCESS_KEY}
    secret-key: ${S3_SECRET_KEY}

keycloak:
  realm: buurman
  auth-server-url: ${KEYCLOAK_URL:http://localhost:8080}
  resource: buurman-api
  public-client: false
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}

app:
  jwt:
    public-key-location: ${KEYCLOAK_URL}/realms/buurman/protocol/openid-connect/certs
  documents:
    max-file-size: 52428800 # 50MB
    allowed-mime-types: application/pdf,image/jpeg,image/png,image/gif
```

---

### Frontend Architecture

**Folder Structure:**
```
src/
├── api/
│   ├── client.ts (Axios instance with interceptors)
│   ├── auth.ts
│   ├── properties.ts
│   ├── tenants.ts
│   ├── contracts.ts
│   ├── payments.ts
│   ├── expenses.ts
│   ├── documents.ts
│   └── reports.ts
├── components/
│   ├── common/
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx
│   │   ├── Table.tsx
│   │   ├── Form/
│   │   └── Layout/
│   ├── auth/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   └── ProtectedRoute.tsx
│   ├── dashboard/
│   │   ├── DashboardPage.tsx
│   │   ├── StatCard.tsx
│   │   └── RecentActivities.tsx
│   ├── properties/
│   │   ├── PropertyList.tsx
│   │   ├── PropertyCard.tsx
│   │   ├── PropertyDetail.tsx
│   │   └── PropertyForm.tsx
│   ├── tenants/
│   ├── contracts/
│   ├── payments/
│   ├── expenses/
│   ├── reports/
│   └── documents/
├── hooks/
│   ├── useAuth.ts
│   ├── useProperties.ts
│   ├── useTenants.ts
│   └── ...
├── context/
│   ├── AuthContext.tsx
│   └── TeamContext.tsx
├── types/
│   ├── auth.ts
│   ├── property.ts
│   ├── tenant.ts
│   └── ...
├── utils/
│   ├── dateFormat.ts
│   ├── currencyFormat.ts
│   └── validation.ts
├── routes/
│   └── index.tsx
├── App.tsx
└── main.tsx
```

**Key Components:**

1. **Axios Client Configuration (api/client.ts)**
```typescript
import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

// Request interceptor: attach JWT token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401, refresh token
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Redirect to login or refresh token
    }
    return Promise.reject(error);
  }
);

export default client;
```

2. **React Query Setup**
```typescript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
```

3. **Custom Hooks Example (useProperties.ts)**
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getProperties, createProperty, updateProperty, deleteProperty } from '@/api/properties';

export const useProperties = () => {
  return useQuery({
    queryKey: ['properties'],
    queryFn: getProperties,
  });
};

export const useCreateProperty = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createProperty,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
  });
};
```

**Environment Variables (.env)**
```
VITE_API_URL=http://localhost:8080/api
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=buurman
VITE_KEYCLOAK_CLIENT_ID=buurman-web
```

---

### Database Schema

**Key Conventions:**
- All tables have `team_id` for multi-tenancy
- All tables have `id` (UUID) as primary key
- Business-facing IDs use `business_id` (ULID)
- Audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`
- Soft deletes: `deleted_at` column
- Foreign keys with ON DELETE CASCADE where appropriate
- Indexes on frequently queried columns (team_id, status, dates)

**Row-Level Security (Future Enhancement):**
```sql
ALTER TABLE properties ENABLE ROW LEVEL SECURITY;

CREATE POLICY properties_team_isolation ON properties
  FOR ALL
  TO authenticated
  USING (team_id = current_setting('app.current_team_id')::uuid);
```

---

### Security & Authentication

**Authentication Flow:**
1. User visits app, redirected to Keycloak login
2. User authenticates with Keycloak
3. Keycloak redirects back with authorization code
4. Frontend exchanges code for access token and refresh token
5. Frontend stores tokens (memory or secure storage)
6. All API requests include `Authorization: Bearer <token>` header
7. Backend validates token signature with Keycloak public key
8. Backend extracts claims (user_id, team_id, roles)
9. Backend filters all data by team_id

**Token Claims:**
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "team_id": "team-uuid",
  "roles": ["TEAM_ADMIN"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Role-Based Access Control:**
- TEAM_ADMIN: Full access (all CRUD operations, team management)
- TEAM_EDITOR: Create, read, update (no delete, no team management)
- TEAM_VIEWER: Read-only access

**Implementation:**
```java
@PreAuthorize("hasRole('TEAM_ADMIN')")
@DeleteMapping("/properties/{id}")
public ResponseEntity<Void> deleteProperty(@PathVariable UUID id) {
    propertyService.delete(id);
    return ResponseEntity.noContent().build();
}
```

---

### Testing Strategy

**Backend Tests:**

1. **Unit Tests (JUnit 5 + Mockito)**
   - Service layer logic
   - Mappers (MapStruct)
   - Utilities
   - Target: 80% code coverage

2. **Integration Tests (Spring Boot Test + Testcontainers)**
   - Repository tests with real database
   - Controller tests with MockMvc
   - Security tests (authentication, authorization)
   - Target: All critical paths covered

3. **API Tests (REST Assured)**
   - End-to-end API tests
   - Happy path and error scenarios
   - Multi-tenant isolation verification

**Frontend Tests:**

1. **Unit Tests (Vitest + React Testing Library)**
   - Component rendering
   - Hook behavior
   - Utility functions
   - Target: 70% code coverage

2. **Integration Tests**
   - User flows (login, create property, etc.)
   - Form validation
   - API integration (with MSW mocking)

3. **E2E Tests (Playwright - Optional)**
   - Critical user journeys
   - Cross-browser testing

**Test Data:**
- Use Faker library for generating realistic test data
- Seed database with sample data for development
- Separate test database for integration tests

---

### Deployment & DevOps

**Docker Compose (Production):**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: buurman
      POSTGRES_USER: buurman
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - buurman-network

  keycloak:
    image: quay.io/keycloak/keycloak:latest
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: buurman
      KC_DB_PASSWORD: ${DB_PASSWORD}
    command: start
    depends_on:
      - postgres
    networks:
      - buurman-network

  backend:
    image: buurman-backend:latest
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/buurman
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      KEYCLOAK_URL: http://keycloak:8080
      S3_ENDPOINT: ${S3_ENDPOINT}
      S3_ACCESS_KEY: ${S3_ACCESS_KEY}
      S3_SECRET_KEY: ${S3_SECRET_KEY}
    depends_on:
      - postgres
      - keycloak
    networks:
      - buurman-network

  frontend:
    image: buurman-frontend:latest
    build:
      context: ./frontend
      dockerfile: Dockerfile
    environment:
      VITE_API_URL: http://backend:8080/api
      VITE_KEYCLOAK_URL: http://keycloak:8080
    depends_on:
      - backend
    ports:
      - "3000:80"
    networks:
      - buurman-network

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - buurman-network

  grafana:
    image: grafana/grafana:latest
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
    volumes:
      - grafana_data:/var/lib/grafana
    ports:
      - "3001:3000"
    networks:
      - buurman-network

volumes:
  postgres_data:
  prometheus_data:
  grafana_data:

networks:
  buurman-network:
```

**GitHub Actions Workflow:**
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: mvn clean test
      - name: Build
        run: mvn clean package -DskipTests

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '22'
      - name: Install dependencies
        run: yarn install
      - name: Run tests
        run: yarn test
      - name: Build
        run: yarn build

  docker-build:
    needs: [backend-test, frontend-test]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - name: Build and push Docker images
        run: |
          docker build -t buurman-backend:latest ./backend
          docker build -t buurman-frontend:latest ./frontend
```

**Monitoring:**
- Micrometer metrics exposed at `/actuator/metrics`
- Prometheus scrapes metrics every 30 seconds
- Grafana dashboards for:
  - Application health (uptime, response times)
  - Database performance (connection pool, query times)
  - Business metrics (active users, properties created, payments processed)
  - Error rates and exceptions

---

## Non-Functional Requirements

### Performance
- API response time: < 200ms for 95th percentile
- Page load time: < 2 seconds for initial load
- Database query time: < 100ms for most queries
- File upload: Support files up to 50MB
- Concurrent users: Support 100 concurrent users per instance

### Scalability
- Horizontal scaling: Stateless backend, multiple instances behind load balancer
- Database: Connection pooling, read replicas for reporting queries
- File storage: S3 handles storage scaling

### Availability
- Uptime target: 99.5% (excludes planned maintenance)
- Database backups: Daily automated backups, retained for 30 days
- Disaster recovery: Point-in-time recovery within 24 hours

### Security
- Data encryption: HTTPS/TLS for all communications
- At-rest encryption: Database and S3 encryption enabled
- Password policy: Enforced by Keycloak (min 12 chars, complexity)
- Session management: JWT tokens expire after 1 hour, refresh tokens after 7 days
- Audit logging: All data modifications logged with user and timestamp

### Usability
- Responsive design: Works on desktop (1920x1080), tablet (768x1024), mobile (375x667)
- Browser support: Chrome, Firefox, Safari, Edge (latest 2 versions)
- Accessibility: WCAG 2.1 Level AA compliance
- Internationalization: English language (foundation for future localization)

### Maintainability
- Code quality: SonarQube analysis, no critical issues
- Documentation: OpenAPI specs for all endpoints, README for setup
- Dependency updates: Monthly security patch review
- Code review: All changes require peer review before merge

---

## Development Workflow

### Initial Setup
1. Clone repository
2. Copy `.env.example` to `.env` and fill in values
3. Run `docker-compose up` to start services
4. Run backend: `mvn spring-boot:run`
5. Run frontend: `yarn dev`
6. Access app at `http://localhost:5173`

### Database Migrations
1. Create new migration file: `V<version>__<description>.sql`
2. Place in `src/main/resources/db/migration/`
3. Restart application (Flyway runs automatically)
4. Verify migration in `flyway_schema_history` table

### Adding New Feature
1. Create feature branch from `develop`
2. Implement backend:
   - Create entity, repository, service, controller
   - Add DTO and mapper
   - Write unit and integration tests
   - Update OpenAPI annotations
3. Implement frontend:
   - Create components
   - Add API client methods
   - Create custom hooks
   - Write tests
4. Test locally with `docker-compose`
5. Create pull request
6. Code review and approval
7. Merge to `develop`
8. Deploy to staging for QA
9. Merge to `main` for production release

### Code Standards
- Backend: Google Java Style Guide
- Frontend: Airbnb JavaScript Style Guide
- Commit messages: Conventional Commits format
- Branch naming: `feature/`, `bugfix/`, `hotfix/` prefixes

---

## Future Enhancements (Post-MVP)

### Phase 5: Advanced Features
- **Maintenance Requests**: Tenant portal for submitting maintenance requests
- **Calendar Integration**: Sync contract dates, payment due dates to Google Calendar

### Phase 6: Intelligence & Automation
- **Predictive Analytics**: Forecast future income, identify at-risk tenants
- **Automated Expense Categorization**: ML-based category suggestions
- **OCR for Receipts**: Automatic data extraction from uploaded invoices
- **Smart Rent Pricing**: Market analysis and pricing recommendations

### Phase 7: Ecosystem Integration
- **Bank Account Sync**: Automatic payment matching via open banking
- **Government Reporting**: Automated tax form generation

---

## Success Metrics

### Technical Metrics
- Code coverage: > 80% for backend, > 70% for frontend
- Build time: < 5 minutes
- Deployment frequency: Multiple times per week
- Mean time to recovery: < 4 hours

### Business Metrics
- User registration conversion: > 20%
- Active users (monthly): Track growth
- Average properties per team: Track growth
- User retention (90 days): > 60%

### User Satisfaction
- System usability scale (SUS): > 70
- Net promoter score (NPS): > 40
- Support ticket volume: Decreasing trend
- Feature adoption rate: > 50% for major features

---
