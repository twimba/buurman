import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { useTranslation } from 'react-i18next';
import { TeamSettingsSection } from '@/components/settings/TeamSettingsSection';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';

export const AdminTeamMembersPage = () => {
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
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-1">
            <Users className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-text-primary">
              {t('teamMembers.title')}
            </h1>
          </div>
          <p className="text-text-secondary ml-11">
            {t('teamMembers.subtitle')}
          </p>
        </div>
        <ImpersonationGuard
          blockAlways
          fallback={
            <div className="text-center py-12 text-text-secondary">
              {t('teamMembers.impersonationBlocked')}
            </div>
          }
        >
          <TeamSettingsSection />
        </ImpersonationGuard>
      </div>
    </div>
  );
};
