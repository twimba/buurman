import { TenantResponse } from '@/types/tenant';
import { Mail, Phone, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/common/Avatar';

interface TenantCardProps {
  tenant: TenantResponse;
}

export const TenantCard = ({ tenant }: TenantCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
      onClick={() => navigate(`/tenants/${tenant.id}`)}
    >
      {/* Tenant Name with Avatar */}
      <div className="flex items-center gap-3 mb-4">
        <Avatar
          firstName={tenant.firstName}
          lastName={tenant.lastName}
          photoUrl={tenant.mainPhotoUrl}
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

      {/* Current Property */}
      {tenant.currentProperty ? (
        <div className="flex items-center gap-2 bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-3 py-2 rounded">
          <Home className="h-4 w-4" />
          <span className="text-sm font-medium">
            {tenant.currentProperty.street}, {tenant.currentProperty.city}
          </span>
        </div>
      ) : (
        <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] px-3 py-2">
          <Home className="h-4 w-4" />
          <span className="text-sm">No property assigned</span>
        </div>
      )}
    </div>
  );
};
