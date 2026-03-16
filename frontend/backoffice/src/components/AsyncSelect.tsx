import { useState, useRef, useEffect, useCallback } from "react";
import { X, ChevronDown, Loader2 } from "lucide-react";

export interface AsyncSelectOption {
  value: string;
  label: string;
  sublabel?: string;
}

interface AsyncSelectProps {
  selected: AsyncSelectOption[];
  onSelect: (options: AsyncSelectOption[]) => void;
  search: (query: string) => Promise<AsyncSelectOption[]>;
  placeholder?: string;
  className?: string;
}

export const AsyncSelect = ({
  selected,
  onSelect,
  search,
  placeholder = "Search...",
  className = "",
}: AsyncSelectProps) => {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<AsyncSelectOption[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [highlightIndex, setHighlightIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  const doSearch = useCallback(
    async (q: string) => {
      if (!q.trim()) {
        setResults([]);
        return;
      }
      setIsLoading(true);
      try {
        const res = await search(q);
        const selectedValues = new Set(selected.map((s) => s.value));
        setResults(res.filter((r) => !selectedValues.has(r.value)));
      } catch {
        setResults([]);
      } finally {
        setIsLoading(false);
      }
    },
    [search, selected],
  );

  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    debounceRef.current = setTimeout(() => doSearch(query), 300);
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [query, doSearch]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (option: AsyncSelectOption) => {
    onSelect([...selected, option]);
    setQuery("");
    setResults([]);
    setHighlightIndex(-1);
    inputRef.current?.focus();
  };

  const handleRemove = (value: string) => {
    onSelect(selected.filter((s) => s.value !== value));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlightIndex((i) => Math.min(i + 1, results.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlightIndex((i) => Math.max(i - 1, 0));
    } else if (
      e.key === "Enter" &&
      highlightIndex >= 0 &&
      results[highlightIndex]
    ) {
      e.preventDefault();
      handleSelect(results[highlightIndex]);
    } else if (e.key === "Escape") {
      setIsOpen(false);
    } else if (
      e.key === "Backspace" &&
      query === "" &&
      selected.length > 0
    ) {
      handleRemove(selected[selected.length - 1].value);
    }
  };

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <div className="flex flex-nowrap gap-1.5 items-center px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus-within:border-primary-500 focus-within:ring-2 focus-within:ring-primary-500/20 transition-colors overflow-x-auto">
        {selected.map((s) => (
          <span
            key={s.value}
            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-primary-50 text-primary-700 text-xs font-medium"
          >
            {s.label}
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                handleRemove(s.value);
              }}
              className="hover:text-primary-900"
            >
              <X className="h-3 w-3" />
            </button>
          </span>
        ))}
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
            setHighlightIndex(-1);
          }}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder={selected.length === 0 ? placeholder : ""}
          className="flex-1 min-w-[100px] bg-transparent outline-none placeholder-text-muted text-sm"
        />
        {isLoading ? (
          <Loader2 className="h-4 w-4 text-text-muted animate-spin flex-shrink-0" />
        ) : (
          <ChevronDown className="h-4 w-4 text-text-muted flex-shrink-0" />
        )}
      </div>

      {isOpen && results.length > 0 && (
        <div className="absolute z-50 mt-1 w-full max-h-60 overflow-auto rounded-lg border border-border-default bg-surface-card shadow-lg">
          {results.map((option, idx) => (
            <button
              key={option.value}
              type="button"
              onClick={() => handleSelect(option)}
              className={`w-full text-left px-3 py-2 text-sm transition-colors ${
                idx === highlightIndex
                  ? "bg-primary-50 text-primary-700"
                  : "text-text-primary hover:bg-surface-inset"
              }`}
            >
              <span className="font-medium">{option.label}</span>
              {option.sublabel && (
                <span className="ml-2 text-text-muted text-xs">
                  {option.sublabel}
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      {isOpen && query.trim() && !isLoading && results.length === 0 && (
        <div className="absolute z-50 mt-1 w-full rounded-lg border border-border-default bg-surface-card shadow-lg">
          <div className="px-3 py-2 text-sm text-text-muted">
            No results found
          </div>
        </div>
      )}
    </div>
  );
};
