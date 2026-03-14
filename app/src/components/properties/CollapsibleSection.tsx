import { useState, ReactNode } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';

interface CollapsibleSectionProps {
  title: string;
  filledCount: number;
  totalCount: number;
  children: ReactNode;
  defaultOpen?: boolean;
}

export const CollapsibleSection = ({
  title,
  filledCount,
  totalCount,
  children,
  defaultOpen = false,
}: CollapsibleSectionProps) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const panelId = `section-${title.replace(/\s+/g, '-').toLowerCase()}`;

  return (
    <div className="border border-border-default rounded-lg overflow-hidden">
      <button
        type="button"
        aria-expanded={isOpen}
        aria-controls={panelId}
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between px-4 py-3 bg-surface-page hover:bg-surface-inset focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:ring-offset-1 transition-colors text-left"
      >
        <div className="flex items-center gap-2">
          {isOpen ? (
            <ChevronDown className="h-4 w-4 text-text-secondary " />
          ) : (
            <ChevronRight className="h-4 w-4 text-text-secondary " />
          )}
          <span className="font-medium text-text-primary">{title}</span>
        </div>
        <span className="text-sm text-text-muted">
          {filledCount} of {totalCount} filled
        </span>
      </button>
      {isOpen && (
        <div id={panelId} role="region" className="px-4 py-4 space-y-4">
          {children}
        </div>
      )}
    </div>
  );
};
