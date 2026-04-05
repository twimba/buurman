import { useState } from 'react';
import {
  useContactNotes,
  useCreateContactNote,
  useUpdateContactNote,
  useDeleteContactNote,
  usePinContactNote,
  useUnpinContactNote,
} from '@/hooks/useContactHooks';
import {
  InteractionType,
  INTERACTION_TYPE_LABELS,
  CreateContactNoteRequest,
  UpdateContactNoteRequest,
  ContactNoteResponse,
} from '@/types/contact';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  FormField,
  Input,
  LoadingSpinner,
  ModalWrapper,
  RichTextDisplay,
  RichTextEditor,
  Select,
} from '@buurman/ui';
import {
  Plus,
  Pin,
  PinOff,
  Edit,
  Trash2,
  Phone,
  Users,
  Eye,
  Key,
  Search as SearchIcon,
  StickyNote,
  MoreHorizontal,
  Calendar,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';

const INTERACTION_TYPE_ICONS: Record<InteractionType, React.ReactNode> = {
  [InteractionType.PHONE_CALL]: <Phone className="h-4 w-4" />,
  [InteractionType.MEETING]: <Users className="h-4 w-4" />,
  [InteractionType.VIEWING]: <Eye className="h-4 w-4" />,
  [InteractionType.KEY_HANDOVER]: <Key className="h-4 w-4" />,
  [InteractionType.INSPECTION]: <SearchIcon className="h-4 w-4" />,
  [InteractionType.NOTE]: <StickyNote className="h-4 w-4" />,
  [InteractionType.OTHER]: <MoreHorizontal className="h-4 w-4" />,
};

interface NoteFormData {
  interactionType: InteractionType;
  subject: string;
  body: string;
  occurredAt: string;
  followUpDate: string;
}

const emptyForm = (): NoteFormData => ({
  interactionType: InteractionType.NOTE,
  subject: '',
  body: '',
  occurredAt: new Date().toISOString().slice(0, 16),
  followUpDate: '',
});

const noteToForm = (note: ContactNoteResponse): NoteFormData => ({
  interactionType: note.interactionType,
  subject: note.subject ?? '',
  body: note.body,
  occurredAt: note.occurredAt.slice(0, 16),
  followUpDate: note.followUpDate ?? '',
});

interface ContactNotesTabProps {
  contactId: string;
}

export const ContactNotesTab = ({ contactId }: ContactNotesTabProps) => {
  const { t } = useTranslation('tenants');
  const { canEditData } = useTeam();
  const { formatDate, formatRelative } = useFormatDate();
  const { data: notes = [], isLoading } = useContactNotes(contactId);
  const createMutation = useCreateContactNote(contactId);
  const deleteMutation = useDeleteContactNote(contactId);
  const pinMutation = usePinContactNote(contactId);
  const unpinMutation = useUnpinContactNote(contactId);

  const [showFormModal, setShowFormModal] = useState(false);
  const [editingNote, setEditingNote] = useState<ContactNoteResponse | null>(
    null
  );
  const [deleteNoteId, setDeleteNoteId] = useState<string | null>(null);
  const [form, setForm] = useState<NoteFormData>(emptyForm());
  const [showDateSection, setShowDateSection] = useState(false);

  const handleOpenCreate = () => {
    setEditingNote(null);
    setForm(emptyForm());
    setShowDateSection(false);
    setShowFormModal(true);
  };

  const handleOpenEdit = (note: ContactNoteResponse) => {
    setEditingNote(note);
    setForm(noteToForm(note));
    setShowDateSection(!!note.followUpDate);
    setShowFormModal(true);
  };

  const handleCloseForm = () => {
    setShowFormModal(false);
    setEditingNote(null);
    setForm(emptyForm());
    setShowDateSection(false);
  };

  const handleSubmit = async () => {
    const payload: CreateContactNoteRequest | UpdateContactNoteRequest = {
      interactionType: form.interactionType,
      subject: form.subject || undefined,
      body: form.body,
      occurredAt: new Date(form.occurredAt).toISOString(),
      followUpDate: form.followUpDate || undefined,
    };
    if (editingNote) {
      await updateNoteMutation.mutateAsync(payload as UpdateContactNoteRequest);
    } else {
      await createMutation.mutateAsync(payload as CreateContactNoteRequest);
    }
    handleCloseForm();
  };

  const handleDelete = async () => {
    if (deleteNoteId) {
      await deleteMutation.mutateAsync(deleteNoteId);
      setDeleteNoteId(null);
    }
  };

  const handleTogglePin = (note: ContactNoteResponse) => {
    if (note.pinned) {
      unpinMutation.mutate(note.identifier);
    } else {
      pinMutation.mutate(note.identifier);
    }
  };

  const updateNoteMutation = useUpdateContactNote(
    contactId,
    editingNote?.identifier ?? ''
  );

  // Sort: pinned first, then by occurredAt desc
  const sortedNotes = [...notes].sort((a, b) => {
    if (a.pinned !== b.pinned) {
      return a.pinned ? -1 : 1;
    }
    return new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime();
  });

  if (isLoading) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-text-primary">
          {t('notes.title')} ({notes.length})
        </h2>
        {canEditData && (
          <Button
            variant="primary"
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={handleOpenCreate}
          >
            Add Note
          </Button>
        )}
      </div>

      {sortedNotes.length === 0 ? (
        <EmptyState
          icon={<StickyNote className="h-12 w-12" />}
          title={t('notes.empty')}
          description={t('notes.emptyDescription')}
          variant="section"
          actions={
            canEditData ? (
              <Button
                variant="primary"
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={handleOpenCreate}
              >
                Add First Note
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3">
          {sortedNotes.map((note) => (
            <div
              key={note.identifier}
              className={`border rounded-lg p-4 ${
                note.pinned
                  ? 'border-primary-300 bg-primary-50/50 dark:border-primary-700 dark:bg-primary-950/50'
                  : 'border-border-default'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3 flex-1 min-w-0">
                  <div className="flex-shrink-0 w-8 h-8 rounded-full bg-surface-inset flex items-center justify-center text-text-secondary">
                    {INTERACTION_TYPE_ICONS[note.interactionType]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-surface-inset text-text-secondary">
                        {INTERACTION_TYPE_LABELS[note.interactionType]}
                      </span>
                      {note.pinned && (
                        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-primary-100 text-primary-700 dark:bg-primary-900 dark:text-primary-300">
                          <Pin className="h-3 w-3 inline mr-1" />
                          Pinned
                        </span>
                      )}
                      {note.followUpDate && (
                        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-warning-bg text-warning-text">
                          <Calendar className="h-3 w-3 inline mr-1" />
                          {t('notes.followUp', {
                            date: formatDate(note.followUpDate),
                          })}
                        </span>
                      )}
                    </div>
                    {note.subject && (
                      <h3 className="text-sm font-semibold text-text-primary mt-1">
                        {note.subject}
                      </h3>
                    )}
                    <div className="mt-1 text-sm text-text-primary">
                      <RichTextDisplay content={note.body} />
                    </div>
                    <div className="flex items-center gap-2 mt-2 text-xs text-text-muted">
                      <span>{formatRelative(note.occurredAt)}</span>
                      {note.createdByName && (
                        <>
                          <span>·</span>
                          <span>{note.createdByName}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
                {canEditData && (
                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button
                      onClick={() => handleTogglePin(note)}
                      className="p-1.5 rounded hover:bg-surface-inset text-text-muted hover:text-text-primary transition-colors"
                      title={note.pinned ? t('notes.unpin') : t('notes.pin')}
                    >
                      {note.pinned ? (
                        <PinOff className="h-4 w-4" />
                      ) : (
                        <Pin className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      onClick={() => handleOpenEdit(note)}
                      className="p-1.5 rounded hover:bg-surface-inset text-text-muted hover:text-text-primary transition-colors"
                      title={t('common:buttons.edit')}
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setDeleteNoteId(note.identifier)}
                      className="p-1.5 rounded hover:bg-error-bg text-text-muted hover:text-error-text transition-colors"
                      title={t('common:buttons.delete')}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Note Form Modal */}
      <ModalWrapper
        open={showFormModal}
        onClose={handleCloseForm}
        title={editingNote ? t('notes.editNote') : t('notes.addNote')}
        size="lg"
        footer={
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={handleCloseForm}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={
                createMutation.isPending || updateNoteMutation.isPending
              }
              disabled={!form.body.trim() || !form.occurredAt}
            >
              {editingNote ? t('notes.saveChanges') : t('notes.createNote')}
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          <FormField label={t('notes.interactionType')} required>
            <Select
              value={form.interactionType}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  interactionType: e.target.value as InteractionType,
                }))
              }
            >
              {Object.entries(INTERACTION_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label={t('notes.subject')}>
            <Input
              value={form.subject}
              onChange={(e) =>
                setForm((f) => ({ ...f, subject: e.target.value }))
              }
              placeholder={t('notes.subjectPlaceholder')}
            />
          </FormField>
          <FormField label={t('notes.body')} required>
            <RichTextEditor
              value={form.body}
              onChange={(value) => setForm((f) => ({ ...f, body: value }))}
              placeholder={t('notes.placeholder')}
            />
          </FormField>

          {/* Collapsed date section */}
          <button
            type="button"
            onClick={() => setShowDateSection(!showDateSection)}
            className="flex items-center gap-1.5 text-sm text-primary-500 hover:text-primary-600 transition-colors"
          >
            <Calendar className="h-4 w-4" />
            {t('notes.setDateFollowUp')}
            {showDateSection ? (
              <ChevronUp className="h-3.5 w-3.5" />
            ) : (
              <ChevronDown className="h-3.5 w-3.5" />
            )}
          </button>
          {showDateSection && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pl-6 border-l-2 border-primary-200">
              <FormField label={t('notes.dateOfInteraction')} required>
                <Input
                  type="datetime-local"
                  value={form.occurredAt}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, occurredAt: e.target.value }))
                  }
                />
              </FormField>
              <FormField label={t('notes.followUpDate')}>
                <Input
                  type="date"
                  value={form.followUpDate}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, followUpDate: e.target.value }))
                  }
                />
              </FormField>
            </div>
          )}
        </div>
      </ModalWrapper>

      {/* Delete Confirmation */}
      {deleteNoteId && (
        <ConfirmDialog
          title={t('notes.deleteTitle')}
          message={t('notes.deleteMessage')}
          variant="danger"
          confirmLabel={t('common:buttons.delete')}
          isLoading={deleteMutation.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteNoteId(null)}
        />
      )}
    </div>
  );
};
