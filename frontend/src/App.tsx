import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { TeamProvider } from './context/TeamContext';
import { ToastProvider } from './context/ToastContext';
import { ThemeProvider } from './context/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import { InvitationPage } from './pages/InvitationPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import { DashboardPage } from './components/DashboardPage';
import { PropertyListPage } from './pages/PropertyListPage';
import { PropertyDetailPage } from './pages/PropertyDetailPage';
import { PropertyCreatePage } from './pages/PropertyCreatePage';
import { PropertyEditPage } from './pages/PropertyEditPage';
import { TenantListPage } from './pages/TenantListPage';
import { TenantDetailPage } from './pages/TenantDetailPage';
import { TenantCreatePage } from './pages/TenantCreatePage';
import { TenantEditPage } from './pages/TenantEditPage';
import { ContractsPage } from './pages/ContractsPage';
import { ContractDetailPage } from './pages/ContractDetailPage';
import { ContractCreatePage } from './pages/ContractCreatePage';
import { ContractEditPage } from './pages/ContractEditPage';
import { PaymentsPage } from './pages/PaymentsPage';
import { PaymentCreatePage } from './pages/PaymentCreatePage';
import { PaymentDetailPage } from './pages/PaymentDetailPage';
import { ExpensesPage } from './pages/ExpensesPage';
import { ExpenseCreatePage } from './pages/ExpenseCreatePage';
import { ExpenseDetailPage } from './pages/ExpenseDetailPage';
import { DocumentsPage } from './pages/DocumentsPage';
import { PhotosPage } from './pages/PhotosPage';
import { FinancialReportsPage } from './pages/FinancialReportsPage';
import { TransactionHistoryPage } from './pages/TransactionHistoryPage';
import { SettingsPage } from './pages/SettingsPage';
import { AuditLogPage } from './pages/AuditLogPage';
import { AdminTeamMembersPage } from './pages/admin/AdminTeamMembersPage';
import { AdminPreferencesPage } from './pages/admin/AdminPreferencesPage';
import { AdminPaymentInstructionsPage } from './pages/admin/AdminPaymentInstructionsPage';
import { AdminCalendarFeedsPage } from './pages/admin/AdminCalendarFeedsPage';
import { AdminNotificationsPage } from './pages/admin/AdminNotificationsPage';
import { AdminBillingPage } from './pages/admin/AdminBillingPage';
import ErrorBoundary from './components/ErrorBoundary';
import { Layout } from './components/Layout';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <TeamProvider>
            <ThemeProvider>
              <ToastProvider>
                <Routes>
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
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
                    path="/tenants"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <TenantListPage />
                        </Layout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/tenants/new"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <TenantCreatePage />
                        </Layout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/tenants/:id"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <TenantDetailPage />
                        </Layout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/tenants/:id/edit"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <TenantEditPage />
                        </Layout>
                      </ProtectedRoute>
                    }
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
                          <FinancialReportsPage />
                        </Layout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/reports/transactions"
                    element={
                      <ProtectedRoute>
                        <Layout>
                          <TransactionHistoryPage />
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
                    path="/admin/payment-instructions"
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

                  {/* Backwards-compat redirects */}
                  <Route
                    path="/team-settings"
                    element={<Navigate to="/admin/team-members" replace />}
                  />
                  <Route
                    path="/audit-log"
                    element={<Navigate to="/admin/activity-log" replace />}
                  />

                  <Route
                    path="/"
                    element={<Navigate to="/dashboard" replace />}
                  />
                </Routes>
              </ToastProvider>
            </ThemeProvider>
          </TeamProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
