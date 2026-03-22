import { ContactResponse, ContactTag, CONTACT_TAG_LABELS } from '@/types/contact';
import { ContractPartyRole, PARTY_ROLE_LABELS } from '@/types/contract';
import { Mail, Phone, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/common/Avatar';
import { StatusBadge } from '@buurman/ui';

const ROLE_COLORS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]: 'bg-info-bg text-info-text',
  [ContractPartyRole.GUARANTOR]: 'bg-warning-bg text-warning-text',
  [ContractPartyRole.COSIGNER]: 'bg-info-bg text-info-text',
  [ContractPartyRole.EXTRA_TENANT]: 'bg-success-bg text-success-text',
};

const CONTACT_TYPE_LABELS: Record<string, string> = {
  INDIVIDUAL: 'Individual',
  COMPANY: 'Company',
  SERVICE_PROVIDER: 'Service Provider',
};

const CONTACT_TYPE_COLORS: Record<string, 'info' | 'warning' | 'success'> = {
  INDIVIDUAL: 'info',
  COMPANY: 'warning',
  SERVICE_PROVIDER: 'success',
};

const TAG_COLORS: Record<ContactTag, 'info' | 'warning' | 'error' | 'success' | 'neutral'> = {
  [ContactTag.VIP]: 'warning',
  [ContactTag.PROSPECT]: 'info',
  [ContactTag.LATE_PAYER]: 'error',
  [ContactTag.LONG_TERM]: 'success',
  [ContactTag.KEY_HOLDER]: 'info',
  [ContactTag.DO_NOT_CONTACT]: 'error',
  [ContactTag.FORMER_TENANT]: 'neutral',
  [ContactTag.REFERRED]: 'success',
};

const MAX_VISIBLE_TAGS = 3;

interface ContactCardProps {
  contact: ContactResponse;
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
          firstName={contact.firstName}
          lastName={contact.lastName}
          photoUrl={contact.mainPhotoThumbnailUrl ?? contact.mainPhotoUrl}
          size="md"
        />
        <div className="min-w-0 flex-1">
          <h3 className="text-lg font-semibold text-text-primary truncate">
            {contact.displayName ?? `${contact.firstName} ${contact.lastName ?? ''}`.trim()}
          </h3>
          <div className="flex items-center gap-2 mt-0.5">
            {contact.contactType && (
              <StatusBadge
                variant={CONTACT_TYPE_COLORS[contact.contactType] ?? 'neutral'}
                size="sm"
              >
                {CONTACT_TYPE_LABELS[contact.contactType] ?? contact.contactType}
              </StatusBadge>
            )}
            <span className="text-xs text-text-muted">#{contact.identifier}</span>
          </div>
        </div>
      </div>

      {/* Tags */}
      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3">
          {visibleTags.map((tag) => (
            <StatusBadge
              key={tag}
              variant={TAG_COLORS[tag] ?? 'neutral'}
              size="sm"
            >
              {CONTACT_TAG_LABELS[tag] ?? tag}
            </StatusBadge>
          ))}
          {overflowCount > 0 && (
            <span className="text-xs text-text-muted px-1.5 py-0.5">
              +{overflowCount}
            </span>
          )}
        </div>
      )}

      {/* Contact Info — clickable icon buttons */}
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

      {/* Properties & Roles */}
      <div className="space-y-2">
        {contact.activeProperties && contact.activeProperties.length > 0 ? (
          contact.activeProperties.map((assignment) => (
            <div
              key={assignment.property.identifier}
              className="flex items-center gap-2 bg-success-bg text-success-text px-3 py-2 rounded"
            >
              <Home className="h-4 w-4 flex-shrink-0" />
              <span className="text-sm font-medium truncate">
                {assignment.property.street}, {assignment.property.city}
              </span>
              {assignment.role && (
                <span
                  className={`ml-auto flex-shrink-0 text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_COLORS[assignment.role as ContractPartyRole] ?? 'bg-surface-inset text-text-secondary'}`}
                >
                  {PARTY_ROLE_LABELS[assignment.role as ContractPartyRole] ??
                    assignment.role}
                </span>
              )}
            </div>
          ))
        ) : (
          <div className="flex items-center gap-2 text-text-secondary px-3 py-2">
            <Home className="h-4 w-4" />
            <span className="text-sm">No property assigned</span>
          </div>
        )}
      </div>
    </div>
  );
};
