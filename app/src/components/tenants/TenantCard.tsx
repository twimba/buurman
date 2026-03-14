import { TenantResponse } from '@/types/tenant';
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

interface TenantCardProps {
  tenant: TenantResponse;
}

export const TenantCard = ({ tenant }: TenantCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-surface-card rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
      onClick={() => navigate(`/tenants/${tenant.identifier}`)}
    >
      {/* Tenant Name with Avatar */}
      <div className="flex items-center gap-3 mb-4">
        <Avatar
          firstName={tenant.firstName}
          lastName={tenant.lastName}
          photoUrl={tenant.mainPhotoThumbnailUrl ?? tenant.mainPhotoUrl}
          size="md"
        />
        <div>
          <h3 className="text-lg font-semibold text-text-primary">
            {tenant.firstName} {tenant.lastName}
          </h3>
          <p className="text-sm text-text-secondary">#{tenant.identifier}</p>
        </div>
      </div>

      {/* Contact Info */}
      <div className="space-y-2 mb-4">
        <div className="flex items-center gap-2 text-text-secondary">
          <Mail className="h-4 w-4" />
          <span className="text-sm">{tenant.email}</span>
        </div>
        {tenant.phone && (
          <div className="flex items-center gap-2 text-text-secondary">
            <Phone className="h-4 w-4" />
            <span className="text-sm">{tenant.phone}</span>
          </div>
        )}
      </div>

      {/* Properties & Roles */}
      <div className="space-y-2">
        {tenant.activeProperties && tenant.activeProperties.length > 0 ? (
          tenant.activeProperties.map((assignment) => (
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
