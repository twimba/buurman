import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileCode } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useAuth } from '@/contexts/AuthContext';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';

const apiBaseUrl = `${window.location.protocol}//api.${window.location.hostname.replace(/^app\./, '')}`;

export const AdminApiDocsPage = () => {
  const { canEditTeamSettings, isLoading } = useTeam();
  const { keycloak } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && !canEditTeamSettings) {
      navigate('/dashboard', { replace: true });
    }
  }, [isLoading, canEditTeamSettings, navigate]);

  if (isLoading || !canEditTeamSettings) {
    return null;
  }

  const token = keycloak?.token;

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <div className="px-4 pt-8 pb-4">
        <div className="flex items-center gap-3 mb-1">
          <FileCode className="h-8 w-8 text-primary-500 dark:text-primary-300" />
          <h1 className="text-3xl font-bold text-text-primary">
            API Documentation
          </h1>
        </div>
        <p className="text-text-secondary ml-11">
          Explore and test the Buurman REST API
        </p>
      </div>
      <div className="flex-1 px-4 pb-4">
        <SwaggerUI
          url={`${apiBaseUrl}/api-docs/openapi/app`}
          docExpansion="none"
          tryItOutEnabled
          requestInterceptor={(req) => {
            if (token) {
              req.headers.Authorization = `Bearer ${token}`;
            }
            return req;
          }}
        />
      </div>
    </div>
  );
};
