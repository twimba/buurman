import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import { Layout } from "./components/Layout";
import { DashboardPage } from "./pages/DashboardPage";
import { TeamsPage } from "./pages/TeamsPage";
import { TeamDetailPage } from "./pages/TeamDetailPage";
import { UsersPage } from "./pages/UsersPage";
import { UserDetailPage } from "./pages/UserDetailPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { NotificationDetailPage } from "./pages/NotificationDetailPage";
import { ToolEmbedPage } from "./pages/ToolEmbedPage";
import { SmsPolicyPage } from "./pages/SmsPolicyPage";
import { FeatureFlagsPage } from "./pages/FeatureFlagsPage";
import { SchedulerPage } from "./pages/SchedulerPage";
import { LoggersPage } from "./pages/LoggersPage";
import { BuurmiesPage } from "./pages/BuurmiesPage";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/teams" element={<TeamsPage />} />
              <Route path="/teams/:identifier" element={<TeamDetailPage />} />
              <Route path="/users" element={<UsersPage />} />
              <Route path="/users/:identifier" element={<UserDetailPage />} />
              <Route path="/buurmies" element={<BuurmiesPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route
                path="/notifications/:identifier"
                element={<NotificationDetailPage />}
              />
              <Route path="/sms-policy" element={<SmsPolicyPage />} />
              <Route path="/feature-flags" element={<FeatureFlagsPage />} />
              <Route path="/scheduler" element={<SchedulerPage />} />
              <Route path="/loggers" element={<LoggersPage />} />
              <Route path="/tools/:toolKey" element={<ToolEmbedPage />} />
            </Route>
          </Route>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
