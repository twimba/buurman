import { Info } from 'lucide-react';

export const RegulationDisclaimer = () => {
  return (
    <div className="flex items-start gap-3 p-4 rounded-lg bg-surface-page border border-border-default">
      <Info className="h-4 w-4 text-text-secondary flex-shrink-0 mt-0.5" />
      <p className="text-xs text-text-secondary leading-relaxed">
        Regulation data is provided for informational purposes only. Always
        verify with official legal sources and seek professional advice before
        applying rent adjustments. We are not accountable for errors or
        omissions.
      </p>
    </div>
  );
};
