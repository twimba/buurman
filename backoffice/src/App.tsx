import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import { Layout } from "./components/Layout";
import { LoadingSpinner } from "./components/LoadingSpinner";
import { EnvironmentBanner } from "./components/EnvironmentBanner";

const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then((m) => ({ default: m.DashboardPage })),
);
const TeamsPage = lazy(() =>
  import("./pages/TeamsPage").then((m) => ({ default: m.TeamsPage })),
);
const TeamDetailPage = lazy(() =>
  import("./pages/TeamDetailPage").then((m) => ({ default: m.TeamDetailPage })),
);
const UsersPage = lazy(() =>
  import("./pages/UsersPage").then((m) => ({ default: m.UsersPage })),
);
const UserDetailPage = lazy(() =>
  import("./pages/UserDetailPage").then((m) => ({ default: m.UserDetailPage })),
);
const NotificationsPage = lazy(() =>
  import("./pages/NotificationsPage").then((m) => ({
    default: m.NotificationsPage,
  })),
);
const NotificationDetailPage = lazy(() =>
  import("./pages/NotificationDetailPage").then((m) => ({
    default: m.NotificationDetailPage,
  })),
);
const ToolEmbedPage = lazy(() =>
  import("./pages/ToolEmbedPage").then((m) => ({ default: m.ToolEmbedPage })),
);
const SmsPolicyPage = lazy(() =>
  import("./pages/SmsPolicyPage").then((m) => ({ default: m.SmsPolicyPage })),
);
const FeatureFlagsPage = lazy(() =>
  import("./pages/FeatureFlagsPage").then((m) => ({
    default: m.FeatureFlagsPage,
  })),
);
const SchedulerPage = lazy(() =>
  import("./pages/SchedulerPage").then((m) => ({ default: m.SchedulerPage })),
);
const LoggersPage = lazy(() =>
  import("./pages/LoggersPage").then((m) => ({ default: m.LoggersPage })),
);
const BuurmiesPage = lazy(() =>
  import("./pages/BuurmiesPage").then((m) => ({ default: m.BuurmiesPage })),
);
const SystemInfoPage = lazy(() =>
  import("./pages/SystemInfoPage").then((m) => ({ default: m.SystemInfoPage })),
);
const RegistrationInvitationsPage = lazy(() =>
  import("./pages/RegistrationInvitationsPage").then((m) => ({
    default: m.RegistrationInvitationsPage,
  })),
);
const RegistrationInvitationDetailPage = lazy(() =>
  import("./pages/RegistrationInvitationDetailPage").then((m) => ({
    default: m.RegistrationInvitationDetailPage,
  })),
);

function App() {
  return (
    <>
      <EnvironmentBanner />
      <BrowserRouter>
      <AuthProvider>
        <Suspense fallback={<LoadingSpinner />}>
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
                <Route
                  path="/registration-invitations"
                  element={<RegistrationInvitationsPage />}
                />
                <Route
                  path="/registration-invitations/:identifier"
                  element={<RegistrationInvitationDetailPage />}
                />
                <Route path="/feature-flags" element={<FeatureFlagsPage />} />
                <Route path="/scheduler" element={<SchedulerPage />} />
                <Route path="/loggers" element={<LoggersPage />} />
                <Route path="/system" element={<SystemInfoPage />} />
                <Route path="/tools/:toolKey" element={<ToolEmbedPage />} />
              </Route>
            </Route>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </AuthProvider>
      </BrowserRouter>
    </>
  );
}

export default App;
