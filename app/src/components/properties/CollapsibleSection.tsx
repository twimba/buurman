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

  return (
    <div className="border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg overflow-hidden">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between px-4 py-3 bg-[#f8f9fc] dark:bg-[#1a1d28] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors text-left"
      >
        <div className="flex items-center gap-2">
          {isOpen ? (
            <ChevronDown className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
          ) : (
            <ChevronRight className="h-4 w-4 text-[#6b7194] dark:text-[#8b90a8]" />
          )}
          <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
            {title}
          </span>
        </div>
        <span className="text-sm text-[#9ca0b8] dark:text-[#5c6180]">
          {filledCount} of {totalCount} filled
        </span>
      </button>
      {isOpen && <div className="px-4 py-4 space-y-4">{children}</div>}
    </div>
  );
};
