import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
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
import { ReportsPage } from './pages/ReportsPage';
import { SettingsPage } from './pages/SettingsPage';
import { AuditLogPage } from './pages/AuditLogPage';
import ErrorBoundary from './components/ErrorBoundary';
import { Layout } from './components/Layout';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
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
                path="/reports"
                element={
                  <ProtectedRoute>
                    <Layout>
                      <ReportsPage />
                    </Layout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/audit-log"
                element={
                  <ProtectedRoute>
                    <Layout>
                      <AuditLogPage />
                    </Layout>
                  </ProtectedRoute>
                }
              />
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
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
