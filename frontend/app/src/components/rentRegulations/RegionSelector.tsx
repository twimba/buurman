import { MapPin } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { RichTextDisplay } from '@buurman/ui';
import type { RentRegulationRegionResponse } from '@/types/rentRegulation';

interface RegionSelectorProps {
  regions: RentRegulationRegionResponse[];
  selectedRegion?: string;
  onSelect: (regionCode: string | undefined) => void;
}

export const RegionSelector = ({
  regions,
  selectedRegion,
  onSelect,
}: RegionSelectorProps) => {
  const { t } = useTranslation('contracts');

  if (regions.length === 0) {
    return null;
  }

  const activeRegion = regions.find((r) => r.regionCode === selectedRegion);

  return (
    <div className="space-y-3">
      <div className="bg-surface-card rounded-lg border border-border-default p-4">
        <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-muted mb-3">
          <MapPin className="h-3.5 w-3.5" />
          {t('rentRegulations.regions')}
        </div>
        <div className="flex flex-wrap gap-1.5">
          <button
            onClick={() => onSelect(undefined)}
            className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 ${
              !selectedRegion
                ? 'bg-primary-500 text-white shadow-sm shadow-primary-500/20'
                : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
            }`}
          >
            {t('rentRegulations.national')}
          </button>
          {regions.map((region) => (
            <button
              key={region.regionCode}
              onClick={() => onSelect(region.regionCode)}
              className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                selectedRegion === region.regionCode
                  ? 'bg-primary-500 text-white shadow-sm shadow-primary-500/20'
                  : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
              }`}
            >
              {region.regionName}
            </button>
          ))}
        </div>
      </div>

      {/* Region summary — rich text */}
      {activeRegion?.summary && (
        <div className="bg-surface-card rounded-lg border border-border-default px-6 py-4">
          <RichTextDisplay
            content={activeRegion.summary}
            className="text-sm text-text-secondary leading-relaxed prose-sm dark:prose-invert max-w-none"
          />
        </div>
      )}
    </div>
  );
};
