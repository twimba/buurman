import { useState } from 'react';
import { useAddContactTag, useRemoveContactTag } from '@/hooks/useContactHooks';
import {
  ContactTag,
  CONTACT_TAG_LABELS,
  TAG_COLORS,
  ContactResponse,
} from '@/types/contact';
import { useTeam } from '@/context/TeamContext';
import { Button, StatusBadge } from '@buurman/ui';
import { Plus, X, Tag } from 'lucide-react';

interface ContactTagsTabProps {
  contactId: string;
  contact: ContactResponse;
}

export const ContactTagsTab = ({ contactId, contact }: ContactTagsTabProps) => {
  const { canEditData } = useTeam();
  const addTagMutation = useAddContactTag(contactId);
  const removeTagMutation = useRemoveContactTag(contactId);
  const [showDropdown, setShowDropdown] = useState(false);

  const currentTags = contact.tags ?? [];
  const availableTags = Object.values(ContactTag).filter(
    (tag) => !currentTags.includes(tag)
  );

  const handleAddTag = async (tag: ContactTag) => {
    await addTagMutation.mutateAsync({ tag });
    setShowDropdown(false);
  };

  const handleRemoveTag = (tag: ContactTag) => {
    removeTagMutation.mutate(tag);
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-text-primary">
          Tags ({currentTags.length})
        </h2>
        {canEditData && availableTags.length > 0 && (
          <div className="relative">
            <Button
              variant="primary"
              leftIcon={<Plus className="h-4 w-4" />}
              onClick={() => setShowDropdown(!showDropdown)}
            >
              Add Tag
            </Button>
            {showDropdown && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setShowDropdown(false)}
                />
                <div className="absolute right-0 mt-2 w-56 bg-surface-card border border-border-strong rounded-md shadow-lg z-50 py-1">
                  {availableTags.map((tag) => (
                    <button
                      key={tag}
                      onClick={() => handleAddTag(tag)}
                      className="w-full text-left px-3 py-2 text-sm hover:bg-surface-inset flex items-center gap-2"
                    >
                      <StatusBadge
                        label={CONTACT_TAG_LABELS[tag]}
                        color={TAG_COLORS[tag]}
                        size="xs"
                        shape="pill"
                      />
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>

      {currentTags.length === 0 ? (
        <div className="text-center py-8">
          <Tag className="h-12 w-12 text-text-disabled mx-auto mb-3" />
          <p className="text-text-secondary">No tags assigned</p>
          <p className="text-sm text-text-muted mt-1">
            Use tags to categorize and quickly identify contacts
          </p>
        </div>
      ) : (
        <div className="flex flex-wrap gap-2">
          {currentTags.map((tag) => (
            <div key={tag} className="inline-flex items-center gap-1.5">
              <StatusBadge
                label={CONTACT_TAG_LABELS[tag]}
                color={TAG_COLORS[tag]}
                size="sm"
                shape="pill"
              />
              {canEditData && (
                <button
                  onClick={() => handleRemoveTag(tag)}
                  disabled={removeTagMutation.isPending}
                  className="p-0.5 rounded-full hover:bg-error-bg text-text-muted hover:text-error-text transition-colors disabled:opacity-50"
                  title={`Remove ${CONTACT_TAG_LABELS[tag]} tag`}
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
