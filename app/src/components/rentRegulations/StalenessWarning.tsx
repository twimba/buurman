import { Info } from 'lucide-react';

export const RegulationDisclaimer = () => {
  return (
    <div className="flex items-start gap-3 p-4 rounded-lg bg-[#f8f9fc] dark:bg-[#0c0d14] border border-[#e2e6f0] dark:border-[#2a2e3f]">
      <Info className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8] flex-shrink-0 mt-0.5" />
      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] leading-relaxed">
        Regulation data is provided for informational purposes only. Always
        verify with official legal sources and seek professional advice before
        applying rent adjustments. We are not accountable for errors or
        omissions.
      </p>
    </div>
  );
};
