import { TenantResponse } from '@/types/tenant';
import { ContractPartyRole, PARTY_ROLE_LABELS } from '@/types/contract';
import { Mail, Phone, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/common/Avatar';

const ROLE_COLORS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]:
    'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  [ContractPartyRole.GUARANTOR]:
    'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  [ContractPartyRole.COSIGNER]:
    'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
  [ContractPartyRole.EXTRA_TENANT]:
    'bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-300',
};

interface TenantCardProps {
  tenant: TenantResponse;
}

export const TenantCard = ({ tenant }: TenantCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
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
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            {tenant.firstName} {tenant.lastName}
          </h3>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
            #{tenant.identifier}
          </p>
        </div>
      </div>

      {/* Contact Info */}
      <div className="space-y-2 mb-4">
        <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8]">
          <Mail className="h-4 w-4" />
          <span className="text-sm">{tenant.email}</span>
        </div>
        {tenant.phone && (
          <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8]">
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
              className="flex items-center gap-2 bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-3 py-2 rounded"
            >
              <Home className="h-4 w-4 flex-shrink-0" />
              <span className="text-sm font-medium truncate">
                {assignment.property.street}, {assignment.property.city}
              </span>
              {assignment.role && (
                <span
                  className={`ml-auto flex-shrink-0 text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_COLORS[assignment.role as ContractPartyRole] ?? 'bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-300'}`}
                >
                  {PARTY_ROLE_LABELS[assignment.role as ContractPartyRole] ??
                    assignment.role}
                </span>
              )}
            </div>
          ))
        ) : (
          <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] px-3 py-2">
            <Home className="h-4 w-4" />
            <span className="text-sm">No property assigned</span>
          </div>
        )}
      </div>
    </div>
  );
};
