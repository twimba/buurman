import { DashboardProvider } from '../context/DashboardContext';
import { DashboardTopbar } from '../components/dashboard/DashboardTopbar';
import { StatusStrip } from '../components/dashboard/StatusStrip';
import { BentoGrid } from '../components/dashboard/BentoGrid';

/** Backoffice landing page: the mission-control dashboard (always on — no feature flag). */
export const DashboardPage = () => (
  <DashboardProvider>
    <div className="space-y-4">
      <DashboardTopbar />
      <StatusStrip />
      <BentoGrid />
    </div>
  </DashboardProvider>
);
