import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Bookmark, Trash2 } from 'lucide-react';
import {
  useSavedContractFilters,
  useCreateSavedContractFilter,
  useDeleteSavedContractFilter,
} from '@/hooks/useSavedContractFilterHooks';

interface SavedFiltersDropdownProps {
  currentCriteria: Record<string, unknown>;
  onApply: (criteria: Record<string, unknown>) => void;
}

export const SavedFiltersDropdown = ({
  currentCriteria,
  onApply,
}: SavedFiltersDropdownProps) => {
  const { t } = useTranslation('contracts');
  const { data: filters = [] } = useSavedContractFilters();
  const createMutation = useCreateSavedContractFilter();
  const deleteMutation = useDeleteSavedContractFilter();
  const [isOpen, setIsOpen] = useState(false);
  const [nameInput, setNameInput] = useState('');

  const handleSave = async () => {
    if (!nameInput.trim()) {
      return;
    }
    await createMutation.mutateAsync({
      name: nameInput.trim(),
      criteria: currentCriteria,
    });
    setNameInput('');
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors"
      >
        <Bookmark className="h-4 w-4" />
        {t('list.savedFilters.button')}
      </button>
      {isOpen && (
        <div className="absolute z-10 mt-1 w-64 bg-surface-card border border-border-default rounded-md shadow-lg p-2">
          <ul className="space-y-1 max-h-48 overflow-y-auto">
            {filters.map((filter) => (
              <li
                key={filter.identifier}
                className="flex items-center justify-between gap-2"
              >
                <button
                  type="button"
                  onClick={() => {
                    onApply(filter.criteria);
                    setIsOpen(false);
                  }}
                  className="flex-1 text-left px-2 py-1 text-sm rounded hover:bg-surface-inset"
                >
                  {filter.name}
                </button>
                <button
                  type="button"
                  onClick={() => deleteMutation.mutate(filter.identifier)}
                  className="p-1 text-error-text hover:bg-error-bg rounded"
                  aria-label={t('list.savedFilters.delete', {
                    name: filter.name,
                  })}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </li>
            ))}
          </ul>
          <div className="mt-2 pt-2 border-t border-border-default flex gap-1.5">
            <input
              type="text"
              value={nameInput}
              onChange={(e) => setNameInput(e.target.value)}
              placeholder={t('list.savedFilters.namePlaceholder')}
              aria-label={t('list.savedFilters.namePlaceholder')}
              className="flex-1 border border-border-strong rounded px-2 py-1 text-sm"
            />
            <button
              type="button"
              onClick={handleSave}
              disabled={createMutation.isPending || !nameInput.trim()}
              className="px-2 py-1 text-sm bg-primary-500 text-white rounded disabled:opacity-50"
            >
              {t('list.savedFilters.save')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
