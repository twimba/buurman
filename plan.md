# Buurman - Property Management for Small Landlords

 Backend Implementation Activities

  1. User Profile & Security

  1.1 User Profile Management

  - Create PATCH /api/users/profile endpoint to update user profile (first name, last name)
  - Create PUT /api/users/email endpoint to update email with verification
  - Create POST /api/users/email/verify endpoint to confirm email change with token
  - Add social_profiles JSONB column to users table
  - Create PATCH /api/users/social-profiles endpoint to update social profiles (LinkedIn, GitHub, website)
  - Implement input validation for social profile URLs

  1.2 Avatar/Photo Upload

  - Create POST /api/users/avatar endpoint to upload user avatar
  - Integrate with S3/LocalStack for avatar storage
  - Create thumbnail generation for avatars (resize to 200x200, 100x100)
  - Create DELETE /api/users/avatar endpoint to remove avatar
  - Add avatar_url column to users table
  - Update audit log when avatar changes

  1.3 Password Management

  - Create POST /api/users/password endpoint to change password
  - Implement current password verification
  - Validate new password strength (min 8 chars, uppercase, lowercase, numbers)
  - Hash passwords using BCrypt
  - Add password_changed_at column to users table for tracking
  - Send email notification on password change
  - Implement password reset flow with tokens

  ---
  2. Team Settings

  2.1 Team Name Management

  - Create PATCH /api/teams/{teamId}/name endpoint
  - Add validation for team name (min 3 chars, max 100 chars)
  - Update audit log when team name changes
  - Broadcast change to all team members (notification)

  2.2 Team Member Invitations

  - Create team_invitations table (id, team_id, email, role, invited_by, token, expires_at, status)
  - Create POST /api/teams/{teamId}/invitations endpoint to invite members
  - Generate unique invitation tokens (UUID) with 7-day expiration
  - Send invitation email with link containing token
  - Create POST /api/invitations/accept endpoint to accept invitation
  - Create GET /api/teams/{teamId}/invitations endpoint to list pending invitations
  - Create DELETE /api/teams/{teamId}/invitations/{invitationId} endpoint to cancel invitation

  2.3 Team Member Management

  - Create GET /api/teams/{teamId}/members endpoint to list team members
  - Create PATCH /api/teams/{teamId}/members/{userId}/role endpoint to change member role
  - Create DELETE /api/teams/{teamId}/members/{userId} endpoint to remove member
  - Implement permission checks (only TEAM_ADMIN can manage members)
  - Add removed_at column to track when members were removed
  - Transfer ownership of user's created entities when removing member
  - Send email notification when member is removed

  ---
  3. Team Preferences

  3.1 Team Preferences Storage

  - Create team_preferences table (team_id, default_currency, default_country, date_format, fiscal_year_start, timezone)
  - Create migration with default values
  - Create GET /api/teams/{teamId}/preferences endpoint
  - Create PATCH /api/teams/{teamId}/preferences endpoint
  - Add validation for currency codes (ISO 4217)
  - Add validation for country names
  - Add validation for timezone strings (IANA format)

  3.2 Apply Preferences

  - Use team preferences as defaults when creating properties
  - Use team preferences for financial reporting
  - Format dates according to team preferences in API responses
  - Update audit log when preferences change

  ---
  4. User Preferences

  4.1 User Preferences Storage

  - Create user_preferences table (user_id, theme, language, email_notifications JSONB, in_app_notifications JSONB)
  - Create migration with default values (all notifications enabled by default)
  - Create GET /api/users/preferences endpoint
  - Create PATCH /api/users/preferences endpoint
  - Add validation for theme values (light, dark, auto)
  - Add validation for language codes (ISO 639-1)

  4.2 Notifications System

  - Implement email notification service
  - Create email templates for each notification type:
    - New contract created
    - Payment due (3 days before)
    - Payment received
    - Expense added
    - Contract expiring (30 days before)
    - Team invitation
  - Create scheduled job for payment due reminders
  - Create scheduled job for contract expiring reminders
  - Respect user's email notification preferences when sending
  - Create in-app notifications system (WebSocket or polling)
  - Create notifications table (id, user_id, type, entity_id, message, read_at, created_at)
  - Create GET /api/notifications endpoint
  - Create PATCH /api/notifications/{id}/read endpoint
  - Create POST /api/notifications/read-all endpoint

  ---
  5. Subscription Management

  5.1 Subscription Plans

  - Create subscription_plans table (id, name, price, currency, billing_period, properties_limit, features JSONB)
  - Seed initial plans (Starter, Professional, Enterprise)
  - Create GET /api/subscription-plans endpoint
  - Create subscriptions table (id, team_id, plan_id, status, current_period_start, current_period_end, cancel_at_period_end)
  - Create GET /api/teams/{teamId}/subscription endpoint

  5.2 Subscription Changes

  - Integrate with payment provider (Stripe/Mollie)
  - Create POST /api/teams/{teamId}/subscription/upgrade endpoint
  - Create POST /api/teams/{teamId}/subscription/downgrade endpoint
  - Implement prorated billing for plan changes
  - Create POST /api/teams/{teamId}/subscription/cancel endpoint
  - Implement cancellation at period end
  - Send email confirmation for plan changes
  - Update audit log for subscription changes

  5.3 Usage Limits

  - Implement property limit enforcement based on subscription
  - Create middleware to check property limits before creation
  - Return 402 Payment Required when limit exceeded
  - Add usage stats to dashboard (properties used / limit)

  ---
  6. Payment Methods

  6.1 Payment Method Management

  - Create payment_methods table (id, team_id, type, provider_id, last4, brand, expiry_month, expiry_year, is_default)
  - Integrate with Stripe/Mollie for payment method storage
  - Create GET /api/teams/{teamId}/payment-methods endpoint
  - Create POST /api/teams/{teamId}/payment-methods endpoint (via Stripe SetupIntent)
  - Create DELETE /api/teams/{teamId}/payment-methods/{id} endpoint
  - Create POST /api/teams/{teamId}/payment-methods/{id}/set-default endpoint
  - Encrypt sensitive payment data
  - Implement PCI compliance requirements

  ---
  7. Payment History & Invoices

  7.1 Invoice Storage

  - Create invoices table (id, team_id, invoice_number, date, amount, currency, status, plan_name, payment_method_id, pdf_url)
  - Create GET /api/teams/{teamId}/invoices endpoint with pagination
  - Add filters: status, date range, search
  - Implement invoice number generation (INV-YYYY-NNN format)

  7.2 Invoice Generation

  - Integrate with payment provider webhooks for invoice creation
  - Create invoice PDF generation service (using iText or similar)
  - Include team details, line items, tax info in PDF
  - Store PDF in S3/LocalStack
  - Create GET /api/invoices/{id}/download endpoint with presigned URL
  - Send invoice email after successful payment

  7.3 Payment Processing

  - Implement webhook handling for payment events:
    - payment_succeeded
    - payment_failed
    - subscription_renewed
    - subscription_cancelled
  - Update invoice status based on payment events
  - Send email notifications for payment failures
  - Implement retry logic for failed payments
  - Create POST /api/invoices/{id}/retry endpoint for failed payments

  ---
  8. Security & Permissions

  8.1 Role-Based Access Control

  - Ensure all team endpoints check TEAM_ADMIN role for:
    - Team name changes
    - Member invitations
    - Member removal
    - Role changes
    - Subscription changes
    - Payment method management
  - Allow all roles to view team preferences
  - Allow only TEAM_ADMIN to modify team preferences
  - Allow all users to manage their own profile and preferences

  8.2 Audit Logging

  - Log all settings changes to audit_log table:
    - Profile updates
    - Password changes
    - Team name changes
    - Member additions/removals
    - Role changes
    - Preference updates
    - Subscription changes
    - Payment method changes

  ---
  9. Email Service

  9.1 Email Templates

  - Create email templates using MJML or HTML:
    - Welcome email
    - Team invitation
    - Email verification
    - Password changed notification
    - Payment successful
    - Payment failed
    - Invoice ready
    - Contract expiring soon
    - Payment due reminder
    - Member removed notification
  - Implement email template rendering service
  - Configure email provider (SendGrid, AWS SES, Mailgun)

  ---
  10. Testing & Documentation

  10.1 Testing

  - Write unit tests for all new service methods
  - Write integration tests for all new endpoints
  - Test multi-tenancy isolation for all endpoints
  - Test subscription limit enforcement
  - Test payment webhook handling
  - Test email notification delivery
  - Test invitation flow end-to-end

  10.2 Documentation

  - Update OpenAPI/Swagger documentation for all new endpoints
  - Document webhook payload formats
  - Document email template variables
  - Create setup guide for payment provider integration
  - Document subscription plan configuration

  ---
  Priority Order

  Phase 1 - Core Settings (Week 1-2)
  1. User profile management (1.1)
  2. Avatar upload (1.2)
  3. Password management (1.3)
  4. Team name management (2.1)
  5. Team preferences (3.1, 3.2)
  6. User preferences (4.1)

  Phase 2 - Team Collaboration (Week 3)
  7. Team member invitations (2.2)
  8. Team member management (2.3)
  9. Email service setup (9.1)

  Phase 3 - Notifications (Week 4)
  10. Notification system (4.2)
  11. Email templates (9.1)

  Phase 4 - Subscriptions (Week 5-6)
  12. Subscription plans (5.1)
  13. Payment provider integration (5.2, 6.1)
  14. Subscription changes (5.2)
  15. Usage limits (5.3)

  Phase 5 - Billing (Week 7)
  16. Payment methods (6.1)
  17. Invoice storage (7.1)
  18. Invoice generation (7.2)
  19. Payment processing (7.3)

  Phase 6 - Polish (Week 8)
  20. Security audit (8.1)
  21. Comprehensive testing (10.1)
  22. Documentation (10.2)


pdated Backend Implementation Tasks

  Phase 1 - Multi-Team Foundation (Week 1-2)

  1.1 Database Schema Changes

  - Create team_members junction table:
  CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL CHECK (role IN ('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')),
    is_owner BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    invited_by UUID REFERENCES users(id),
    UNIQUE(team_id, user_id)
  );
  - Add indexes:
  CREATE INDEX idx_team_members_user ON team_members(user_id);
  CREATE INDEX idx_team_members_team ON team_members(team_id);
  CREATE INDEX idx_team_members_role ON team_members(team_id, role);
  - Add default_team_id column to users table:
  ALTER TABLE users ADD COLUMN default_team_id UUID REFERENCES teams(id);
  - Create migration to move existing team relationships to junction table
  - Update foreign key constraints

  1.2 Team Member Relationships

  - Create GET /api/users/{userId}/teams endpoint - list all teams user belongs to
  - Create GET /api/teams/{teamId}/members endpoint - list team members
  - Update user registration to create initial team and team_members entry
  - Implement cascade deletion when user leaves/removed from team

  1.3 Team Switching

  - Create POST /api/users/switch-team endpoint
    - Validates user has access to target team
    - Returns new JWT with updated team_id claim
    - Logs team switch in audit log
  - Update JWT token to include:
    - team_id - current active team
    - default_team_id - user's default team
    - teams - array of team IDs user belongs to
    - role - role in current active team
  - Create JwtTeamResolver to extract team context from JWT
  - Update TeamContextHolder to use JWT team context

  1.4 Default Team Management

  - Create PUT /api/users/default-team endpoint
    - Validates user has access to team
    - Updates user's default_team_id
    - Returns updated user info
  - Use default team on login (initial JWT generation)
  - Use default team for email notifications

  ---
  Phase 2 - Team Invitations (Week 3)

  2.1 Invitation System

  - Update team_invitations table to reference team_members role:
  ALTER TABLE team_invitations ADD COLUMN target_role VARCHAR(50) NOT NULL DEFAULT 'TEAM_VIEWER';
  - Create POST /api/teams/{teamId}/invitations endpoint
    - Only TEAM_ADMIN can invite
    - Send invitation email with token
    - Token includes team_id, email, role, invited_by
  - Create GET /api/invitations/{token} endpoint
    - Validates token and shows invitation details
    - Returns team info, inviter name, role being offered
  - Create POST /api/invitations/{token}/accept endpoint
    - Creates team_members entry
    - If user doesn't exist, triggers registration flow
    - Sets invitation status to ACCEPTED
    - Sends notification to team admin
  - Create DELETE /api/invitations/{token}/decline endpoint

  2.2 Team Member Management (Updated)

  - Update DELETE /api/teams/{teamId}/members/{userId} endpoint
    - Only TEAM_ADMIN or TEAM_OWNER can remove members
    - Cannot remove team owner
    - If user's active team is being left, switch to default team
    - Update all owned entities (properties, contracts) to team ownership
  - Create PATCH /api/teams/{teamId}/members/{userId}/role endpoint
    - Change member's role
    - Only TEAM_ADMIN can change roles
    - Cannot change owner's role
  - Create POST /api/teams/{teamId}/transfer-ownership endpoint
    - Transfer team ownership to another member
    - Only current owner can transfer
    - Previous owner becomes TEAM_ADMIN

  ---
  Phase 3 - Permission System (Week 4)

  3.1 Role-Based Access Control

  - Create @RequiresTeamRole annotation for endpoints
  - Implement TeamPermissionService:
  boolean canManageTeam(UUID userId, UUID teamId)
  boolean canEditData(UUID userId, UUID teamId)
  boolean canViewData(UUID userId, UUID teamId)
  boolean isTeamOwner(UUID userId, UUID teamId)
  - Update all endpoints to check team membership and role
  - Return 403 Forbidden for insufficient permissions
  - Add role info to all API responses

  3.2 Data Isolation

  - Update all queries to verify user has access to team
  - Add TeamMembershipInterceptor to check team access on every request
  - Prevent cross-team data access even with valid team_id
  - Add audit log entries for permission denied attempts

  ---
  Phase 4 - Settings & Preferences (Week 5)

  4.1 Team-Specific Settings

  - Settings pages return data based on active team from JWT
  - Subscription information tied to team
  - Payment methods tied to team (TEAM_ADMIN only)
  - Invoices filtered by team (TEAM_ADMIN only)
  - Team preferences loaded per active team

  4.2 User Preferences

  - Personal preferences remain user-specific (theme, notifications)
  - Notification preferences can be set per-team:
  CREATE TABLE user_team_notification_preferences (
    user_id UUID REFERENCES users(id),
    team_id UUID REFERENCES teams(id),
    notification_type VARCHAR(50),
    enabled BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (user_id, team_id, notification_type)
  );

  ---
  Phase 5 - Subscription Updates (Week 6)

  5.1 Team-Based Subscriptions

  - Subscription is owned by team, not individual user
  - Only TEAM_ADMIN can manage subscription
  - Only TEAM_OWNER can cancel subscription
  - Property limits apply per team
  - Team member limits apply per team

  5.2 Multi-Team Billing

  - Each team has separate subscription
  - Users don't pay per team they join (team owner pays)
  - When user creates new team, that team gets free plan
  - Display subscription info only for teams where user is admin

  ---
  Phase 6 - Testing & Documentation (Week 7)

  6.1 Testing

  - Test team switching functionality
  - Test permission enforcement across all endpoints
  - Test invitation flow end-to-end
  - Test multi-team data isolation
  - Test subscription limits per team
  - Test default team behavior
  - Test team member removal cascades
  - Load test with users in multiple teams

  6.2 Documentation

  - Document team switching API
  - Document permission levels
  - Document invitation flow
  - Document multi-team data model
  - Update OpenAPI specs

  ---
  Frontend-Backend Integration Points

  Endpoints needed for TeamContext:
  1. GET /api/users/me/teams - Load user's teams with roles
  2. POST /api/users/switch-team - Switch active team (returns new JWT)
  3. PUT /api/users/default-team - Set default team
  4. GET /api/teams/{teamId}/info - Get team details

  JWT Claims Structure:
  {
    "sub": "user-id",
    "email": "user@example.com",
    "team_id": "active-team-id",
    "default_team_id": "default-team-id",
    "role": "TEAM_ADMIN",
    "is_owner": true,
    "teams": ["team-id-1", "team-id-2", "team-id-3"]
  }

  The frontend is now ready and waiting for these backend endpoints! 🚀
