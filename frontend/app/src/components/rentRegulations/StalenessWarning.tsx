import { Info } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export const RegulationDisclaimer = () => {
  const { t } = useTranslation('contracts');
  return (
    <div className="flex items-start gap-3 p-4 rounded-lg bg-surface-page border border-border-default">
      <Info className="h-4 w-4 text-text-secondary flex-shrink-0 mt-0.5" />
      <p className="text-xs text-text-secondary leading-relaxed">
        {t('rentRegulations.disclaimer')}
      </p>
    </div>
  );
};
