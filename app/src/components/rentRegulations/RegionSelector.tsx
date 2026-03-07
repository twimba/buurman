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

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
      <h3 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
        Regions
      </h3>
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => onSelect(undefined)}
          className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
            !selectedRegion
              ? 'bg-[#5c7cfa] text-white'
              : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
          }`}
        >
          National
        </button>
        {regions.map((region) => (
          <button
            key={region.regionCode}
            onClick={() => onSelect(region.regionCode)}
            className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
              selectedRegion === region.regionCode
                ? 'bg-[#5c7cfa] text-white'
                : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
            }`}
          >
            {region.regionName}
          </button>
        ))}
      </div>
    </div>
  );
};
