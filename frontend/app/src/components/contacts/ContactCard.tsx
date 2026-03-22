import { ContactResponse } from '@/types/contact';
import { ContractPartyRole, PARTY_ROLE_LABELS } from '@/types/contract';
import { Mail, Phone, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/common/Avatar';

const ROLE_COLORS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]: 'bg-info-bg text-info-text',
  [ContractPartyRole.GUARANTOR]: 'bg-warning-bg text-warning-text',
  [ContractPartyRole.COSIGNER]: 'bg-info-bg text-info-text',
  [ContractPartyRole.EXTRA_TENANT]: 'bg-success-bg text-success-text',
};

interface ContactCardProps {
  contact: ContactResponse;
}

export const ContactCard = ({ contact }: ContactCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
      onClick={() => navigate(`/contacts/${contact.identifier}`)}
    >
      {/* Contact Name with Avatar */}
      <div className="flex items-center gap-3 mb-4">
        <Avatar
          firstName={contact.firstName}
          lastName={contact.lastName}
          photoUrl={contact.mainPhotoThumbnailUrl ?? contact.mainPhotoUrl}
          size="md"
        />
        <div>
          <h3 className="text-lg font-semibold text-text-primary">
            {contact.firstName} {contact.lastName}
          </h3>
          <p className="text-sm text-text-secondary">#{contact.identifier}</p>
        </div>
      </div>

      {/* Contact Info */}
      <div className="space-y-2 mb-4">
        <div className="flex items-center gap-2 text-text-secondary">
          <Mail className="h-4 w-4" />
          <span className="text-sm">{contact.email}</span>
        </div>
        {contact.phone && (
          <div className="flex items-center gap-2 text-text-secondary">
            <Phone className="h-4 w-4" />
            <span className="text-sm">{contact.phone}</span>
          </div>
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
