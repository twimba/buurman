import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Settings } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useTranslation } from 'react-i18next';
import { TeamPreferencesSection } from '@/components/settings/TeamPreferencesSection';

export const AdminPreferencesPage = () => {
  const { t } = useTranslation('admin');
  const { canEditTeamSettings, isLoading } = useTeam();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && !canEditTeamSettings) {
      navigate('/dashboard', { replace: true });
    }
  }, [isLoading, canEditTeamSettings, navigate]);

  if (isLoading || !canEditTeamSettings) {
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <Settings className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('preferences.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('preferences.subtitle')}
          </p>
        </div>
        <TeamPreferencesSection />
      </div>
    </div>
  );
};
