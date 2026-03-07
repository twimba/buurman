import { MapPin } from 'lucide-react';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
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
  if (regions.length === 0) {
    return null;
  }

  const activeRegion = regions.find((r) => r.regionCode === selectedRegion);

  return (
    <div className="space-y-3">
      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
        <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-3">
          <MapPin className="h-3.5 w-3.5" />
          Regions
        </div>
        <div className="flex flex-wrap gap-1.5">
          <button
            onClick={() => onSelect(undefined)}
            className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 ${
              !selectedRegion
                ? 'bg-[#5c7cfa] text-white shadow-sm shadow-[#5c7cfa]/20'
                : 'bg-[#f1f3f9] dark:bg-[#0c0d14] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#1e2130]'
            }`}
          >
            National
          </button>
          {regions.map((region) => (
            <button
              key={region.regionCode}
              onClick={() => onSelect(region.regionCode)}
              className={`px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                selectedRegion === region.regionCode
                  ? 'bg-[#5c7cfa] text-white shadow-sm shadow-[#5c7cfa]/20'
                  : 'bg-[#f1f3f9] dark:bg-[#0c0d14] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#1e2130]'
              }`}
            >
              {region.regionName}
            </button>
          ))}
        </div>
      </div>

      {/* Region summary — rich text */}
      {activeRegion?.summary && (
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] px-6 py-4">
          <RichTextDisplay
            content={activeRegion.summary}
            className="text-sm text-[#3d4463] dark:text-[#c4c8db] leading-relaxed prose-sm dark:prose-invert max-w-none"
          />
        </div>
      )}
    </div>
  );
};
