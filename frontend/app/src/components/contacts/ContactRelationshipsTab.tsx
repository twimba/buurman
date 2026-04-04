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
  RELATIONSHIP_TYPE_LABELS,
  CreateContactRelationshipRequest,
  UpdateContactRelationshipRequest,
  ContactRelationshipResponse,
} from '@/types/contact';
import { useTeam } from '@/context/TeamContext';
import { ContactSelector } from '@/components/common/ContactSelector';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  FormField,
  LoadingSpinner,
  ModalWrapper,
  Select,
} from '@buurman/ui';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { Avatar } from '@/components/common/Avatar';
import { Plus, Edit, Trash2, Users, ShieldOff } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

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
  const navigate = useNavigate();
  const { canEditData } = useTeam();
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
          Relationships ({relationships.length})
        </h2>
        {canEditData && (
          <Button
            variant="primary"
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={handleOpenCreate}
          >
            Add Relationship
          </Button>
        )}
      </div>

      {relationships.length === 0 ? (
        <EmptyState
          icon={<Users className="h-12 w-12" />}
          title="No relationships yet"
          description="Track how this contact relates to other contacts in your system."
          variant="section"
          actions={
            canEditData ? (
              <Button
                variant="primary"
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={handleOpenCreate}
              >
                Add First Relationship
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
                          Erased contact
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
                      Added{' '}
                      {formatDistanceToNow(new Date(rel.createdAt), {
                        addSuffix: true,
                      })}
                    </p>
                  </div>
                </div>
                {canEditData && (
                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button
                      onClick={() => handleOpenEdit(rel)}
                      className="p-1.5 rounded hover:bg-surface-inset text-text-muted hover:text-text-primary transition-colors"
                      title="Edit"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setDeleteRelationshipId(rel.identifier)}
                      className="p-1.5 rounded hover:bg-error-bg text-text-muted hover:text-error-text transition-colors"
                      title="Delete"
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
        title={editingRelationship ? 'Edit Relationship' : 'Add Relationship'}
        size="md"
        footer={
          <div className="flex gap-3 justify-end">
            <Button variant="secondary" onClick={handleCloseForm}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={createMutation.isPending || updateMutation.isPending}
              disabled={!editingRelationship && !form.targetContactIdentifier}
            >
              {editingRelationship ? 'Save Changes' : 'Create'}
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          {!editingRelationship && (
            <FormField label="Related Contact" required>
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
          <FormField label="Relationship Type" required>
            <Select
              value={form.relationshipType}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  relationshipType: e.target.value as RelationshipType,
                }))
              }
            >
              {Object.entries(RELATIONSHIP_TYPE_LABELS).map(
                ([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                )
              )}
            </Select>
          </FormField>
          <FormField label="Notes">
            <RichTextEditor
              value={form.notes}
              onChange={(value) => setForm((f) => ({ ...f, notes: value }))}
              placeholder="Additional notes about this relationship..."
            />
          </FormField>
        </div>
      </ModalWrapper>

      {/* Delete Confirmation */}
      {deleteRelationshipId && (
        <ConfirmDialog
          title="Delete Relationship"
          message="Are you sure you want to remove this relationship? This action cannot be undone."
          variant="danger"
          confirmLabel="Delete"
          isLoading={deleteMutation.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteRelationshipId(null)}
        />
      )}
    </div>
  );
};
