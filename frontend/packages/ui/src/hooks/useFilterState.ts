import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

type FilterValues = Record<string, string | undefined>;

interface UseFilterStateOptions {
  /** Filter keys to sync with URL search params */
  keys: string[];
  /** Default values for filters */
  defaults?: FilterValues;
}

export function useFilterState({ keys, defaults = {} }: UseFilterStateOptions) {
  const [searchParams, setSearchParams] = useSearchParams();

  const values = useMemo(() => {
    const result: FilterValues = {};
    for (const key of keys) {
      result[key] = searchParams.get(key) || defaults[key] || undefined;
    }
    return result;
  }, [searchParams, keys, defaults]);

  const onChange = useCallback(
    (next: FilterValues) => {
      setSearchParams((prev) => {
        const updated = new URLSearchParams(prev);
        for (const key of keys) {
          const val = next[key];
          if (val) {
            updated.set(key, val);
          } else {
            updated.delete(key);
          }
        }
        return updated;
      });
    },
    [keys, setSearchParams],
  );

  const onReset = useCallback(() => {
    setSearchParams((prev) => {
      const updated = new URLSearchParams(prev);
      for (const key of keys) {
        updated.delete(key);
      }
      return updated;
    });
  }, [keys, setSearchParams]);

  return { values, onChange, onReset };
}
