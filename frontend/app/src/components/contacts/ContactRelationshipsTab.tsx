import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useContactRelationships,
  useCreateContactRelationship,
  useUpdateContactRelationship,
  useDeleteContactRelationship,
} from '@/hooks/useContactHooks';
import {
  RelationshipType,
  CreateContactRelationshipRequest,
  UpdateContactRelationshipRequest,
  ContactRelationshipResponse,
} from '@/types/contact';
import { useTeam } from '@/context/TeamContext';
import { ContactSelector } from '@/components/common/ContactSelector';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  FormField,
  LoadingSpinner,
  ModalWrapper,
  RichTextDisplay,
  RichTextEditor,
  Select,
} from '@buurman/ui';
import { Avatar } from '@/components/common/Avatar';
import { Plus, Edit, Trash2, Users, ShieldOff } from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTranslation } from 'react-i18next';

interface RelationshipFormData {
  targetContactIdentifier: string;
  relationshipType: RelationshipType;
  notes: string;
}

const emptyForm = (): RelationshipFormData => ({
  targetContactIdentifier: '',
  relationshipType: RelationshipType.OTHER,
  notes: '',
});

interface ContactRelationshipsTabProps {
  contactId: string;
}

export const ContactRelationshipsTab = ({
  contactId,
}: ContactRelationshipsTabProps) => {
  const { t } = useTranslation('tenants');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatRelative } = useFormatDate();
  const { data: relationships = [], isLoading } =
    useContactRelationships(contactId);
  const createMutation = useCreateContactRelationship(contactId);
  const deleteMutation = useDeleteContactRelationship(contactId);

  const [showFormModal, setShowFormModal] = useState(false);
  const [editingRelationship, setEditingRelationship] =
    useState<ContactRelationshipResponse | null>(null);
  const [deleteRelationshipId, setDeleteRelationshipId] = useState<
    string | null
  >(null);
  const [form, setForm] = useState<RelationshipFormData>(emptyForm());

  const updateMutation = useUpdateContactRelationship(
    contactId,
    editingRelationship?.identifier ?? ''
  );

  const handleOpenCreate = () => {
    setEditingRelationship(null);
    setForm(emptyForm());
    setShowFormModal(true);
  };

  const handleOpenEdit = (rel: ContactRelationshipResponse) => {
    setEditingRelationship(rel);
    setForm({
      targetContactIdentifier: rel.relatedContact.identifier,
      relationshipType: rel.relationshipType,
      notes: rel.notes ?? '',
    });
    setShowFormModal(true);
  };

  const handleCloseForm = () => {
    setShowFormModal(false);
    setEditingRelationship(null);
    setForm(emptyForm());
  };

  const handleSubmit = async () => {
    if (editingRelationship) {
      const payload: UpdateContactRelationshipRequest = {
        relationshipType: form.relationshipType,
        notes: form.notes || undefined,
      };
      await updateMutation.mutateAsync(payload);
    } else {
      const payload: CreateContactRelationshipRequest = {
        targetContactIdentifier: form.targetContactIdentifier,
        relationshipType: form.relationshipType,
        notes: form.notes || undefined,
      };
      await createMutation.mutateAsync(payload);
    }
    handleCloseForm();
  };

  const handleDelete = async () => {
    if (deleteRelationshipId) {
      await deleteMutation.mutateAsync(deleteRelationshipId);
      setDeleteRelationshipId(null);
    }
  };

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
          {t('relationships.title')} ({relationships.length})
        </h2>
        {canEditData && (
          <Button
            variant="primary"
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={handleOpenCreate}
          >
            {t('relationships.addRelationship')}
          </Button>
        )}
      </div>

      {relationships.length === 0 ? (
        <EmptyState
          icon={<Users className="h-12 w-12" />}
          title={t('relationships.empty')}
          description={t('relationships.emptyDescription')}
          variant="section"
          actions={
            canEditData ? (
              <Button
                variant="primary"
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={handleOpenCreate}
              >
                {t('relationships.addFirstRelationship')}
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3">
          {relationships.map((rel) => (
            <div
              key={rel.identifier}
              className="border border-border-default rounded-lg p-4"
            >
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 flex-1 min-w-0">
                  {rel.relatedContact.dataRetentionStatus === 'ANONYMIZED' ? (
                    <div className="h-10 w-10 rounded-full bg-surface-inset flex items-center justify-center">
                      <ShieldOff className="h-5 w-5 text-text-muted" />
                    </div>
                  ) : (
                    <Avatar
                      firstName={
                        rel.relatedContact.firstName ??
                        rel.relatedContact.displayName
                      }
                      lastName={rel.relatedContact.lastName}
                      size="md"
                    />
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      {rel.relatedContact.dataRetentionStatus ===
                      'ANONYMIZED' ? (
                        <span className="text-sm font-medium text-text-muted italic">
                          {t('relationships.erasedContact')}
                        </span>
                      ) : (
                        <button
                          onClick={() =>
                            navigate(
                              `/contacts/${rel.relatedContact.identifier}`
                            )
                          }
                          className="text-sm font-medium text-primary-500 hover:text-primary-600 hover:underline"
                        >
                          {rel.relatedContact.displayName}
                        </button>
                      )}
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-surface-inset text-text-secondary">
                        {rel.displayLabel}
                      </span>
                    </div>
                    {rel.notes && (
                      <div className="text-sm text-text-secondary mt-1 line-clamp-2">
                        <RichTextDisplay content={rel.notes} />
                      </div>
                    )}
                    <p className="text-xs text-text-muted mt-1">
                      {t('relationships.added', {
                        time: formatRelative(rel.createdAt),
                      })}
                    </p>
                  </div>
                </div>
                {canEditData && (
                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button
                      onClick={() => handleOpenEdit(rel)}
                      className="p-1.5 rounded hover:bg-surface-inset text-text-muted hover:text-text-primary transition-colors"
                      title={t('common:buttons.edit')}
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setDeleteRelationshipId(rel.identifier)}
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

      {/* Relationship Form Modal */}
      <ModalWrapper
        open={showFormModal}
        onClose={handleCloseForm}
        title={
          editingRelationship
            ? t('relationships.editRelationship')
            : t('relationships.addRelationship')
        }
        size="md"
        footer={
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={handleCloseForm}>
              {t('common:buttons.cancel')}
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={createMutation.isPending || updateMutation.isPending}
              disabled={!editingRelationship && !form.targetContactIdentifier}
            >
              {editingRelationship
                ? t('relationships.saveChanges')
                : t('common:buttons.create')}
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          {!editingRelationship && (
            <FormField label={t('relationships.relatedContact')} required>
              <ContactSelector
                value={form.targetContactIdentifier}
                onChange={(value) =>
                  setForm((f) => ({
                    ...f,
                    targetContactIdentifier: value,
                  }))
                }
              />
            </FormField>
          )}
          <FormField label={t('relationships.relationshipType')} required>
            <Select
              value={form.relationshipType}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  relationshipType: e.target.value as RelationshipType,
                }))
              }
            >
              {Object.values(RelationshipType).map((value) => (
                <option key={value} value={value}>
                  {t(`enums.relationshipTypes.${value}`)}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label={t('relationships.notes')}>
            <RichTextEditor
              value={form.notes}
              onChange={(value) => setForm((f) => ({ ...f, notes: value }))}
              placeholder={t('relationships.notesPlaceholder')}
            />
          </FormField>
        </div>
      </ModalWrapper>

      {/* Delete Confirmation */}
      {deleteRelationshipId && (
        <ConfirmDialog
          title={t('relationships.deleteTitle')}
          message={t('relationships.deleteMessage')}
          variant="danger"
          confirmLabel={t('common:buttons.delete')}
          isLoading={deleteMutation.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteRelationshipId(null)}
        />
      )}
    </div>
  );
};
