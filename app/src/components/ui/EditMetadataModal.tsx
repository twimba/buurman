import { useState, useEffect } from 'react';
import { X, Save, Loader2 } from 'lucide-react';
import { RichTextEditor } from '../common/RichTextEditor';

interface EditMetadataModalProps {
  title: string;
  currentTitle: string | null;
  currentNotes: string | null;
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg p-6 max-w-lg w-full">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            {title}
          </h3>
          <button
            onClick={onCancel}
            className="p-2 hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
            disabled={isLoading}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Title
            </label>
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded px-3 py-2"
              placeholder="Enter a title"
              disabled={isLoading}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Notes
            </label>
            <RichTextEditor
              value={editNotes}
              onChange={setEditNotes}
              placeholder="Add notes"
              readOnly={isLoading}
              onSubmit={handleSave}
            />
          </div>
        </div>

        <div className="flex gap-2 justify-end mt-6">
          <button
            onClick={onCancel}
            className="border border-[#c9cfd9] dark:border-[#3a3f54] dark:text-[#c4c8db] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
            disabled={isLoading}
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 flex items-center gap-2"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                Save
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
