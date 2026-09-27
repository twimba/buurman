import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { TeamProvider } from './context/TeamContext';
import { ImpersonationProvider } from './context/ImpersonationContext';
import { ThemeProvider } from './context/ThemeContext';
import { LocaleProvider } from './context/LocaleContext';
import { FeatureFlagProvider } from './context/FeatureFlagContext';
import { AnalyticsInitializer } from './components/AnalyticsInitializer';
import { FeatureGate } from './components/FeatureGate';
import { FeatureFlags } from './constants/featureFlags';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary from './components/ErrorBoundary';
import { Layout } from './components/Layout';
import { LoadingSpinner, ToastProvider } from '@buurman/ui';
import { EnvironmentBanner } from './components/common/EnvironmentBanner';
import { ImpersonationBanner } from './components/ImpersonationBanner';

// Auth pages (small, keep near-instant)
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const InvitationPage = lazy(() =>
  import('./pages/InvitationPage').then((m) => ({ default: m.InvitationPage }))
);
const VerifyEmailPage = lazy(() =>
  import('./pages/VerifyEmailPage').then((m) => ({
    default: m.VerifyEmailPage,
  }))
);

// Main pages
const DashboardPage = lazy(() =>
  import('./components/DashboardPage').then((m) => ({
    default: m.DashboardPage,
  }))
);
const PropertyListPage = lazy(() =>
  import('./pages/PropertyListPage').then((m) => ({
    default: m.PropertyListPage,
  }))
);
const PropertyDetailPage = lazy(() =>
  import('./pages/PropertyDetailPage').then((m) => ({
    default: m.PropertyDetailPage,
  }))
);
const PropertyCreatePage = lazy(() =>
  import('./pages/PropertyCreatePage').then((m) => ({
    default: m.PropertyCreatePage,
  }))
);
const PropertyEditPage = lazy(() =>
  import('./pages/PropertyEditPage').then((m) => ({
    default: m.PropertyEditPage,
  }))
);
const UnitDetailPage = lazy(() =>
  import('./pages/UnitDetailPage').then((m) => ({
    default: m.UnitDetailPage,
  }))
);
const ContactListPage = lazy(() =>
  import('./pages/ContactListPage').then((m) => ({
    default: m.ContactListPage,
  }))
);
const ContactDetailPage = lazy(() =>
  import('./pages/ContactDetailPage').then((m) => ({
    default: m.ContactDetailPage,
  }))
);
const ContactCreatePage = lazy(() =>
  import('./pages/ContactCreatePage').then((m) => ({
    default: m.ContactCreatePage,
  }))
);
const ContactEditPage = lazy(() =>
  import('./pages/ContactEditPage').then((m) => ({
    default: m.ContactEditPage,
  }))
);
const ImportHistoryPage = lazy(() =>
  import('./pages/ImportHistoryPage').then((m) => ({
    default: m.ImportHistoryPage,
  }))
);
const ContractsPage = lazy(() =>
  import('./pages/ContractsPage').then((m) => ({ default: m.ContractsPage }))
);
const ContractDetailPage = lazy(() =>
  import('./pages/ContractDetailPage').then((m) => ({
    default: m.ContractDetailPage,
  }))
);
const ContractCreatePage = lazy(() =>
  import('./pages/ContractCreatePage').then((m) => ({
    default: m.ContractCreatePage,
  }))
);
const ContractEditPage = lazy(() =>
  import('./pages/ContractEditPage').then((m) => ({
    default: m.ContractEditPage,
  }))
);
const PaymentsPage = lazy(() =>
  import('./pages/PaymentsPage').then((m) => ({ default: m.PaymentsPage }))
);
const PaymentCreatePage = lazy(() =>
  import('./pages/PaymentCreatePage').then((m) => ({
    default: m.PaymentCreatePage,
  }))
);
const PaymentDetailPage = lazy(() =>
  import('./pages/PaymentDetailPage').then((m) => ({
    default: m.PaymentDetailPage,
  }))
);
const ExpensesPage = lazy(() =>
  import('./pages/ExpensesPage').then((m) => ({ default: m.ExpensesPage }))
);
const ExpenseCreatePage = lazy(() =>
  import('./pages/ExpenseCreatePage').then((m) => ({
    default: m.ExpenseCreatePage,
  }))
);
const ExpenseDetailPage = lazy(() =>
  import('./pages/ExpenseDetailPage').then((m) => ({
    default: m.ExpenseDetailPage,
  }))
);
const DocumentsPage = lazy(() =>
  import('./pages/DocumentsPage').then((m) => ({ default: m.DocumentsPage }))
);
const PhotosPage = lazy(() =>
  import('./pages/PhotosPage').then((m) => ({ default: m.PhotosPage }))
);
const FinancialReportsPage = lazy(() =>
  import('./pages/FinancialReportsPage').then((m) => ({
    default: m.FinancialReportsPage,
  }))
);
const TransactionHistoryPage = lazy(() =>
  import('./pages/TransactionHistoryPage').then((m) => ({
    default: m.TransactionHistoryPage,
  }))
);
const SettingsPage = lazy(() =>
  import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage }))
);
const AuditLogPage = lazy(() =>
  import('./pages/AuditLogPage').then((m) => ({ default: m.AuditLogPage }))
);
const RentRegulationsPage = lazy(() =>
  import('./pages/RentRegulationsPage').then((m) => ({
    default: m.RentRegulationsPage,
  }))
);
const RentIncreaseWizardPage = lazy(() =>
  import('./pages/RentIncreaseWizardPage').then((m) => ({
    default: m.RentIncreaseWizardPage,
  }))
);
const ImpersonatePage = lazy(() =>
  import('./pages/ImpersonatePage').then((m) => ({
    default: m.ImpersonatePage,
  }))
);

// Admin pages
const AdminTeamMembersPage = lazy(() =>
  import('./pages/admin/AdminTeamMembersPage').then((m) => ({
    default: m.AdminTeamMembersPage,
  }))
);
const AdminPreferencesPage = lazy(() =>
  import('./pages/admin/AdminPreferencesPage').then((m) => ({
    default: m.AdminPreferencesPage,
  }))
);
const AdminPaymentInstructionsPage = lazy(() =>
  import('./pages/admin/AdminPaymentInstructionsPage').then((m) => ({
    default: m.AdminPaymentInstructionsPage,
  }))
);
const AdminCalendarFeedsPage = lazy(() =>
  import('./pages/admin/AdminCalendarFeedsPage').then((m) => ({
    default: m.AdminCalendarFeedsPage,
  }))
);
const AdminDataExportPage = lazy(() =>
  import('./pages/admin/AdminDataExportPage').then((m) => ({
    default: m.AdminDataExportPage,
  }))
);
const AdminNotificationsPage = lazy(() =>
  import('./pages/admin/AdminNotificationsPage').then((m) => ({
    default: m.AdminNotificationsPage,
  }))
);
const AdminBillingPage = lazy(() =>
  import('./pages/admin/AdminBillingPage').then((m) => ({
    default: m.AdminBillingPage,
  }))
);
const AdminApiDocsPage = lazy(() =>
  import('./pages/admin/AdminApiDocsPage').then((m) => ({
    default: m.AdminApiDocsPage,
  }))
);

function App() {
  return (
    <ErrorBoundary>
      <EnvironmentBanner />
      <BrowserRouter>
        <AuthProvider>
          <ImpersonationProvider>
            <TeamProvider>
              <AnalyticsInitializer />
              <FeatureFlagProvider>
                <ThemeProvider>
                  <ToastProvider>
                    <LocaleProvider>
                      <ImpersonationBanner />
                      <Suspense fallback={<LoadingSpinner />}>
                        <Routes>
                          {/* Public routes (no auth required) */}
                          <Route path="/login" element={<LoginPage />} />
                          <Route path="/register" element={<RegisterPage />} />
                          <Route
                            path="/impersonate"
                            element={<ImpersonatePage />}
                          />
                          <Route
                            path="/invitation/:token"
                            element={<InvitationPage />}
                          />

                          {/* Verify email — auth required but no layout */}
                          <Route
                            element={
                              <ProtectedRoute requireVerification={false} />
                            }
                          >
                            <Route
                              path="/verify-email"
                              element={<VerifyEmailPage />}
                            />
                          </Route>

                          {/* All protected routes with layout */}
                          <Route element={<ProtectedRoute />}>
                            <Route element={<Layout />}>
                              {/* Main pages */}
                              <Route
                                path="/dashboard"
                                element={<DashboardPage />}
                              />
                              <Route
                                path="/properties"
                                element={<PropertyListPage />}
                              />
                              <Route
                                path="/properties/new"
                                element={<PropertyCreatePage />}
                              />
                              <Route
                                path="/properties/:id"
                                element={<PropertyDetailPage />}
                              />
                              <Route
                                path="/properties/:id/edit"
                                element={<PropertyEditPage />}
                              />
                              <Route
                                path="/properties/:id/units/:unitId"
                                element={<UnitDetailPage />}
                              />
                              <Route
                                path="/contacts"
                                element={<ContactListPage />}
                              />
                              <Route
                                path="/contacts/imports"
                                element={<ImportHistoryPage />}
                              />
                              <Route
                                path="/contacts/new"
                                element={<ContactCreatePage />}
                              />
                              <Route
                                path="/contacts/:id"
                                element={<ContactDetailPage />}
                              />
                              <Route
                                path="/contacts/:id/edit"
                                element={<ContactEditPage />}
                              />
                              <Route
                                path="/contracts"
                                element={<ContractsPage />}
                              />
                              <Route
                                path="/contracts/new"
                                element={<ContractCreatePage />}
                              />
                              <Route
                                path="/contracts/:id"
                                element={<ContractDetailPage />}
                              />
                              <Route
                                path="/contracts/:id/edit"
                                element={<ContractEditPage />}
                              />
                              <Route
                                path="/rent-regulations"
                                element={<RentRegulationsPage />}
                              />
                              <Route
                                path="/rent-increases/apply"
                                element={<RentIncreaseWizardPage />}
                              />
                              <Route
                                path="/payments"
                                element={<PaymentsPage />}
                              />
                              <Route
                                path="/payments/new"
                                element={<PaymentCreatePage />}
                              />
                              <Route
                                path="/payments/:id"
                                element={<PaymentDetailPage />}
                              />
                              <Route
                                path="/expenses"
                                element={<ExpensesPage />}
                              />
                              <Route
                                path="/expenses/new"
                                element={<ExpenseCreatePage />}
                              />
                              <Route
                                path="/expenses/:id"
                                element={<ExpenseDetailPage />}
                              />
                              <Route
                                path="/documents"
                                element={<DocumentsPage />}
                              />
                              <Route path="/photos" element={<PhotosPage />} />

                              {/* Feature-gated routes */}
                              <Route
                                path="/reports"
                                element={
                                  <FeatureGate
                                    flag={FeatureFlags.REPORTS}
                                    fallback={
                                      <Navigate to="/dashboard" replace />
                                    }
                                  >
                                    <FinancialReportsPage />
                                  </FeatureGate>
                                }
                              />
                              <Route
                                path="/reports/transactions"
                                element={
                                  <FeatureGate
                                    flag={FeatureFlags.REPORTS}
                                    fallback={
                                      <Navigate to="/dashboard" replace />
                                    }
                                  >
                                    <TransactionHistoryPage />
                                  </FeatureGate>
                                }
                              />

                              {/* Personal Settings */}
                              <Route
                                path="/settings"
                                element={<SettingsPage />}
                              />

                              {/* Administration Pages (Admin Only) */}
                              <Route
                                path="/admin/team-members"
                                element={<AdminTeamMembersPage />}
                              />
                              <Route
                                path="/admin/preferences"
                                element={<AdminPreferencesPage />}
                              />
                              <Route
                                path="/admin/payment-instructions"
                                element={<AdminPaymentInstructionsPage />}
                              />
                              <Route
                                path="/admin/calendar-feeds"
                                element={<AdminCalendarFeedsPage />}
                              />
                              <Route
                                path="/admin/data-export"
                                element={<AdminDataExportPage />}
                              />
                              <Route
                                path="/admin/notifications"
                                element={<AdminNotificationsPage />}
                              />
                              <Route
                                path="/admin/billing"
                                element={<AdminBillingPage />}
                              />
                              <Route
                                path="/admin/activity-log"
                                element={<AuditLogPage />}
                              />
                              <Route
                                path="/admin/api-docs"
                                element={
                                  <FeatureGate
                                    flag={FeatureFlags.SWAGGER}
                                    fallback={
                                      <Navigate to="/dashboard" replace />
                                    }
                                  >
                                    <AdminApiDocsPage />
                                  </FeatureGate>
                                }
                              />
                            </Route>
                          </Route>

                          {/* Backwards-compat redirects */}
                          <Route
                            path="/tenants/*"
                            element={<Navigate to="/contacts" replace />}
                          />
                          <Route
                            path="/team-settings"
                            element={
                              <Navigate to="/admin/team-members" replace />
                            }
                          />
                          <Route
                            path="/audit-log"
                            element={
                              <Navigate to="/admin/activity-log" replace />
                            }
                          />
                          <Route
                            path="/payment-instructions"
                            element={
                              <Navigate
                                to="/admin/payment-instructions"
                                replace
                              />
                            }
                          />
                          <Route
                            path="/"
                            element={<Navigate to="/dashboard" replace />}
                          />
                        </Routes>
                      </Suspense>
                    </LocaleProvider>
                  </ToastProvider>
                </ThemeProvider>
              </FeatureFlagProvider>
            </TeamProvider>
          </ImpersonationProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
