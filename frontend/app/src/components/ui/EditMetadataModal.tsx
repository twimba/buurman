import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Save, Loader2 } from 'lucide-react';
import { RichTextEditor } from '@buurman/ui';

interface EditMetadataModalProps {
  title: string;
  currentTitle?: string;
  currentNotes?: string;
  onSave: (title: string | null, notes: string | null) => void;
  onCancel: () => void;
  isLoading: boolean;
}

export const EditMetadataModal = ({
  title,
  currentTitle,
  currentNotes,
  onSave,
  onCancel,
  isLoading,
}: EditMetadataModalProps) => {
  const { t } = useTranslation('common');
  const [editTitle, setEditTitle] = useState(currentTitle ?? '');
  const [editNotes, setEditNotes] = useState(currentNotes ?? '');

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onCancel]);

  const handleSave = () => {
    if (isLoading) {
      return;
    }
    const cleanNotes = editNotes.trim();
    // Treat empty editor content as null
    const notesValue =
      !cleanNotes || cleanNotes === '<p></p>' ? null : cleanNotes;
    onSave(editTitle.trim() || null, notesValue);
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSave();
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4"
      onKeyDown={handleCmdEnter}
    >
      <div className="bg-surface-card rounded-lg p-6 max-w-lg w-full">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-text-primary">{title}</h3>
          <button
            onClick={onCancel}
            className="p-2 hover:bg-surface-inset rounded transition-colors"
            disabled={isLoading}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('editMetadata.title')}
            </label>
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2"
              placeholder={t('editMetadata.titlePlaceholder')}
              disabled={isLoading}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('editMetadata.notes')}
            </label>
            <RichTextEditor
              value={editNotes}
              onChange={setEditNotes}
              placeholder={t('editMetadata.notesPlaceholder')}
              readOnly={isLoading}
              onSubmit={handleSave}
            />
          </div>
        </div>

        <div className="flex gap-2 justify-end mt-6">
          <button
            onClick={onCancel}
            className="border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors"
            disabled={isLoading}
          >
            {t('common:buttons.cancel')}
          </button>
          <button
            onClick={handleSave}
            className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                {t('editMetadata.saving')}
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                {t('common:buttons.save')}
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
