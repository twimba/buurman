import { useHealth, useInfo } from '@/hooks/useHealth';
import { LoadingSpinner } from './LoadingSpinner';
import { ErrorMessage } from './ErrorMessage';
import { Activity, Info, Server } from 'lucide-react';

export const DashboardPage = () => {
  const { data: health, isLoading: healthLoading, error: healthError } = useHealth();
  const { data: info, isLoading: infoLoading } = useInfo();

  if (healthLoading) {
    return <LoadingSpinner />;
  }

  if (healthError) {
    return (
      <div className="p-8">
        <ErrorMessage message="Failed to connect to backend. Make sure the backend is running on port 8081." />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">
          Buurman Dashboard
        </h1>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Health Status Card */}
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-700">Backend Status</h3>
              <Server className="h-6 w-6 text-primary-500" />
            </div>
            <div className="text-2xl font-bold text-green-600">
              {health?.status || 'UNKNOWN'}
            </div>
            <div className="text-sm text-gray-500 mt-2">
              Last checked: {health?.timestamp ? new Date(health.timestamp).toLocaleTimeString() : 'N/A'}
            </div>
          </div>

          {/* Version Info Card */}
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-700">Version Info</h3>
              <Info className="h-6 w-6 text-primary-500" />
            </div>
            {infoLoading ? (
              <LoadingSpinner />
            ) : (
              <>
                <div className="text-2xl font-bold text-gray-900">
                  v{info?.version || '0.1.0'}
                </div>
                <div className="text-sm text-gray-500 mt-2">
                  Environment: {info?.environment || 'default'}
                </div>
              </>
            )}
          </div>

          {/* Phase Status Card */}
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-700">Phase Status</h3>
              <Activity className="h-6 w-6 text-primary-500" />
            </div>
            <div className="text-2xl font-bold text-blue-600">
              Phase 1.1
            </div>
            <div className="text-sm text-gray-500 mt-2">
              Infrastructure Setup Complete
            </div>
          </div>
        </div>

        {/* Information Panel */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">
            Welcome to Buurman
          </h2>
          <p className="text-gray-600 mb-4">
            The infrastructure setup is complete! You now have:
          </p>
          <ul className="list-disc list-inside space-y-2 text-gray-600 mb-6">
            <li>Spring Boot backend running with database migrations</li>
            <li>React frontend with TypeScript and TailwindCSS</li>
            <li>PostgreSQL database with complete schema</li>
            <li>LocalStack for S3 simulation</li>
            <li>Keycloak ready for authentication (Phase 1.2)</li>
          </ul>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h3 className="font-semibold text-blue-900 mb-2">Next Steps (Phase 1.2):</h3>
            <ul className="list-disc list-inside space-y-1 text-blue-800 text-sm">
              <li>Implement JWT authentication with Keycloak</li>
              <li>Add user registration and team creation</li>
              <li>Create protected routes and role-based access</li>
              <li>Build team invitation system</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
