import {
  ContactListItemResponse,
  ContactTag,
  CONTACT_TAG_LABELS,
  ContactType,
  CONTACT_TYPE_LABELS,
} from '@/types/contact';
import { Mail, Phone, FileText, Calendar } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/common/Avatar';
import { StatusBadge } from '@buurman/ui';
import type { BadgeColorVariant } from '@buurman/ui';

const CONTACT_TYPE_COLORS: Record<ContactType, BadgeColorVariant> = {
  INDIVIDUAL: 'blue',
  COMPANY: 'amber',
  SERVICE_PROVIDER: 'green',
};

const TAG_COLORS: Record<ContactTag, BadgeColorVariant> = {
  [ContactTag.VIP]: 'amber',
  [ContactTag.PROSPECT]: 'blue',
  [ContactTag.LATE_PAYER]: 'red',
  [ContactTag.LONG_TERM]: 'green',
  [ContactTag.KEY_HOLDER]: 'cyan',
  [ContactTag.DO_NOT_CONTACT]: 'red',
  [ContactTag.FORMER_TENANT]: 'gray',
  [ContactTag.REFERRED]: 'teal',
};

const MAX_VISIBLE_TAGS = 3;

interface ContactCardProps {
  contact: ContactListItemResponse;
}

export const ContactCard = ({ contact }: ContactCardProps) => {
  const navigate = useNavigate();
  const tags = contact.tags ?? [];
  const visibleTags = tags.slice(0, MAX_VISIBLE_TAGS);
  const overflowCount = tags.length - MAX_VISIBLE_TAGS;

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
      onClick={() => navigate(`/contacts/${contact.identifier}`)}
    >
      {/* Header: Avatar + Name + Type badge */}
      <div className="flex items-center gap-3 mb-3">
        <Avatar
          firstName={contact.firstName ?? contact.displayName}
          lastName={contact.lastName}
          photoUrl={contact.mainPhotoThumbnailUrl}
          size="md"
        />
        <div className="min-w-0 flex-1">
          <h3 className="text-lg font-semibold text-text-primary truncate">
            {contact.displayName}
          </h3>
          <div className="flex items-center gap-2 mt-0.5">
            <StatusBadge
              label={
                CONTACT_TYPE_LABELS[contact.contactType] ?? contact.contactType
              }
              color={CONTACT_TYPE_COLORS[contact.contactType] ?? 'gray'}
              size="sm"
            />
          </div>
        </div>
      </div>

      {/* Tags */}
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3">
          {visibleTags.map((tag) => (
            <StatusBadge
              key={tag}
              label={CONTACT_TAG_LABELS[tag] ?? tag}
              color={TAG_COLORS[tag] ?? 'gray'}
              size="sm"
            />
          ))}
          {overflowCount > 0 && (
            <span className="text-xs text-text-muted px-1.5 py-0.5">
              +{overflowCount}
            </span>
          )}
        </div>
      )}

      {/* Contact Info */}
      <div className="flex items-center gap-3 mb-4">
        {contact.email && (
          <a
            href={`mailto:${contact.email}`}
            className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-primary-500 transition-colors"
            onClick={(e) => e.stopPropagation()}
            title={contact.email}
          >
            <Mail className="h-4 w-4" />
            <span className="truncate max-w-[160px]">{contact.email}</span>
          </a>
        )}
        {contact.phone && (
          <a
            href={`tel:${contact.phone}`}
            className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-primary-500 transition-colors"
            onClick={(e) => e.stopPropagation()}
            title={contact.phone}
          >
            <Phone className="h-4 w-4" />
            <span>{contact.phone}</span>
          </a>
        )}
      </div>

      {/* Footer: Active Contracts + Created Date */}
      <div className="flex items-center justify-between gap-2 text-text-secondary px-3 py-2 bg-surface-inset rounded">
        <div className="flex items-center gap-2">
          <FileText className="h-4 w-4" />
          <span className="text-sm">
            {contact.activeContractCount > 0
              ? `${contact.activeContractCount} active contract${contact.activeContractCount !== 1 ? 's' : ''}`
              : 'No active contracts'}
          </span>
        </div>
        <div
          className="flex items-center gap-1.5 text-text-muted"
          title={new Date(contact.createdAt).toLocaleDateString()}
        >
          <Calendar className="h-3.5 w-3.5" />
          <span className="text-xs">
            {formatDistanceToNow(new Date(contact.createdAt), {
              addSuffix: true,
            })}
          </span>
        </div>
      </div>
    </div>
  );
};
