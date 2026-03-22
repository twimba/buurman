import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { TeamProvider } from './context/TeamContext';
import { ImpersonationProvider } from './context/ImpersonationContext';
import { ToastProvider } from './context/ToastContext';
import { ThemeProvider } from './context/ThemeContext';
import { FeatureFlagProvider } from './context/FeatureFlagContext';
import { AnalyticsInitializer } from './components/AnalyticsInitializer';
import { FeatureGate } from './components/FeatureGate';
import { FeatureFlags } from './constants/featureFlags';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary from './components/ErrorBoundary';
import { Layout } from './components/Layout';
import { LoadingSpinner } from './components/LoadingSpinner';
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
                    <ImpersonationBanner />
                    <Suspense fallback={<LoadingSpinner />}>
                      <Routes>
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
                        <Route
                          path="/verify-email"
                          element={
                            <ProtectedRoute requireVerification={false}>
                              <VerifyEmailPage />
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/dashboard"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <DashboardPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/properties"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PropertyListPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/properties/new"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PropertyCreatePage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/properties/:id"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PropertyDetailPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/properties/:id/edit"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PropertyEditPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contacts"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContactListPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contacts/new"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContactCreatePage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contacts/:id"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContactDetailPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contacts/:id/edit"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContactEditPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        {/* Backwards-compat redirect for old tenant URLs */}
                        <Route
                          path="/tenants/*"
                          element={<Navigate to="/contacts" replace />}
                        />
                        <Route
                          path="/contracts"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContractsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contracts/new"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContractCreatePage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contracts/:id"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContractDetailPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/contracts/:id/edit"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ContractEditPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/rent-regulations"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <RentRegulationsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/rent-increases/apply"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <RentIncreaseWizardPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/payments/new"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PaymentCreatePage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/payments/:id"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PaymentDetailPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/payments"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PaymentsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/expenses/new"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ExpenseCreatePage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/expenses/:id"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ExpenseDetailPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/expenses"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <ExpensesPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/documents"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <DocumentsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/photos"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <PhotosPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/reports"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <FeatureGate
                                  flag={FeatureFlags.REPORTS}
                                  fallback={
                                    <Navigate to="/dashboard" replace />
                                  }
                                >
                                  <FinancialReportsPage />
                                </FeatureGate>
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/reports/transactions"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <FeatureGate
                                  flag={FeatureFlags.REPORTS}
                                  fallback={
                                    <Navigate to="/dashboard" replace />
                                  }
                                >
                                  <TransactionHistoryPage />
                                </FeatureGate>
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        {/* Personal Settings */}
                        <Route
                          path="/settings"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <SettingsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />

                        {/* Administration Pages (Admin Only) */}
                        <Route
                          path="/admin/team-members"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminTeamMembersPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/preferences"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminPreferencesPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/payment-instructions"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminPaymentInstructionsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/calendar-feeds"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminCalendarFeedsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/data-export"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminDataExportPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/notifications"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminNotificationsPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/billing"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AdminBillingPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/activity-log"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <AuditLogPage />
                              </Layout>
                            </ProtectedRoute>
                          }
                        />
                        <Route
                          path="/admin/api-docs"
                          element={
                            <ProtectedRoute>
                              <Layout>
                                <FeatureGate
                                  flag={FeatureFlags.SWAGGER}
                                  fallback={
                                    <Navigate to="/dashboard" replace />
                                  }
                                >
                                  <AdminApiDocsPage />
                                </FeatureGate>
                              </Layout>
                            </ProtectedRoute>
                          }
                        />

                        {/* Backwards-compat redirects */}
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
                          path="/"
                          element={<Navigate to="/dashboard" replace />}
                        />
                      </Routes>
                    </Suspense>
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
