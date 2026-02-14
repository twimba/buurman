import { ChevronUp, ChevronDown, ArrowUpDown } from "lucide-react";

interface SortableHeaderProps {
  field: string;
  label: string;
  sort: string;
  direction: string;
  onSortChange: (field: string) => void;
}

export const SortableHeader = ({
  field,
  label,
  sort,
  direction,
  onSortChange,
}: SortableHeaderProps) => (
  <th
    onClick={() => onSortChange(field)}
    className="cursor-pointer select-none text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] transition-colors"
  >
    <div style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
      {label}
      {sort === field ? (
        direction === "asc" ? (
          <ChevronUp className="h-3.5 w-3.5" />
        ) : (
          <ChevronDown className="h-3.5 w-3.5" />
        )
      ) : (
        <ArrowUpDown className="h-3.5 w-3.5 opacity-30" />
      )}
    </div>
  </th>
);
