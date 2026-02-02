import { useState } from 'react';
import {
  Plus,
  MapPin,
  Edit,
  Trash2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import {
  useTenantAddresses,
  useCreateTenantAddress,
  useUpdateTenantAddress,
  useDeleteTenantAddress,
} from '@/hooks/useTenantHooks';
import {
  TenantAddressResponse,
  AddressType,
  AddressStatus,
  CreateTenantAddressRequest,
  UpdateTenantAddressRequest,
} from '@/types/tenant';
import { AddressForm } from './AddressForm';
import { AddressMap } from '../common/AddressMap';

interface TenantAddressListProps {
  tenantId: string;
}

export const TenantAddressList = ({ tenantId }: TenantAddressListProps) => {
  const { data: addresses, isLoading } = useTenantAddresses(tenantId);
  const createMutation = useCreateTenantAddress(tenantId);
  const deleteMutation = useDeleteTenantAddress(tenantId);
  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [isAddingNew, setIsAddingNew] = useState(false);
  const [expandedMapId, setExpandedMapId] = useState<string | null>(null);

  const handleCreateAddress = async (data: CreateTenantAddressRequest) => {
    await createMutation.mutateAsync(data);
    setIsAddingNew(false);
  };

  const handleUpdateAddress = async (
    addressId: string,
    data: UpdateTenantAddressRequest
  ) => {
    const updateMutation = useUpdateTenantAddress(tenantId, addressId);
    await updateMutation.mutateAsync(data);
    setEditingAddressId(null);
  };

  const handleDeleteAddress = async (addressId: string) => {
    if (
      !confirm(
        'Are you sure you want to delete this address? This action cannot be undone.'
      )
    ) {
      return;
    }
    await deleteMutation.mutateAsync(addressId);
  };

  const getTypeBadgeColor = (type: AddressType) => {
    switch (type) {
      case AddressType.CURRENT:
        return 'bg-blue-100 text-blue-800';
      case AddressType.MAILING:
        return 'bg-green-100 text-green-800';
      case AddressType.RELATIVE:
        return 'bg-purple-100 text-purple-800';
      case AddressType.WORK:
        return 'bg-orange-100 text-orange-800';
      case AddressType.HISTORIC:
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusBadgeColor = (status: AddressStatus) => {
    switch (status) {
      case AddressStatus.ACTIVE:
        return 'bg-green-100 text-green-600';
      case AddressStatus.INACTIVE:
        return 'bg-gray-100 text-gray-600';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  };

  const getAddressTypeLabel = (type: AddressType) => {
    switch (type) {
      case AddressType.CURRENT:
        return 'Current';
      case AddressType.MAILING:
        return 'Mailing';
      case AddressType.RELATIVE:
        return 'Relative/Emergency';
      case AddressType.WORK:
        return 'Work';
      case AddressType.HISTORIC:
        return 'Historic';
      default:
        return type;
    }
  };

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        <p className="mt-2 text-gray-600">Loading addresses...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Add Address Button */}
      {!isAddingNew && !editingAddressId && (
        <div>
          <button
            onClick={() => setIsAddingNew(true)}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            Add Address
          </button>
        </div>
      )}

      {/* Add New Address Form */}
      {isAddingNew && (
        <AddressForm
          onSubmit={handleCreateAddress}
          onCancel={() => setIsAddingNew(false)}
          isLoading={createMutation.isPending}
        />
      )}

      {/* Address Cards */}
      {addresses && addresses.length > 0 ? (
        <div className="space-y-4">
          {addresses.map((address) => (
            <div
              key={address.id}
              className="bg-white border border-gray-200 rounded-lg overflow-hidden"
            >
              {editingAddressId === address.id ? (
                <div className="p-6">
                  <AddressForm
                    address={address}
                    onSubmit={(data) =>
                      handleUpdateAddress(
                        address.id,
                        data as UpdateTenantAddressRequest
                      )
                    }
                    onCancel={() => setEditingAddressId(null)}
                    isLoading={false}
                  />
                </div>
              ) : (
                <div className="p-6">
                  {/* Header with badges */}
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span
                        className={`px-3 py-1 rounded-full text-sm font-medium ${getTypeBadgeColor(address.addressType)}`}
                      >
                        {getAddressTypeLabel(address.addressType)}
                      </span>
                      <span
                        className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusBadgeColor(address.status)}`}
                      >
                        {address.status}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setEditingAddressId(address.id)}
                        className="p-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded"
                        title="Edit address"
                      >
                        <Edit className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteAddress(address.id)}
                        className="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded"
                        title="Delete address"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>

                  {/* Address Details */}
                  <div className="flex items-start gap-3 mb-3">
                    <MapPin className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                    <div className="text-gray-700">
                      <div>{address.street}</div>
                      <div>
                        {address.city}
                        {address.postalCode && `, ${address.postalCode}`}
                      </div>
                      <div>{address.country}</div>
                    </div>
                  </div>

                  {/* Map Toggle */}
                  {address.latitude && address.longitude && (
                    <div className="mt-4">
                      <button
                        onClick={() =>
                          setExpandedMapId(
                            expandedMapId === address.id ? null : address.id
                          )
                        }
                        className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700"
                      >
                        {expandedMapId === address.id ? (
                          <>
                            <ChevronUp className="h-4 w-4" />
                            Hide Map
                          </>
                        ) : (
                          <>
                            <ChevronDown className="h-4 w-4" />
                            Show Map
                          </>
                        )}
                      </button>

                      {expandedMapId === address.id && (
                        <div className="mt-3">
                          <AddressMap
                            street={address.street}
                            city={address.city}
                            postalCode={address.postalCode || ''}
                            country={address.country}
                            latitude={address.latitude}
                            longitude={address.longitude}
                            height="h-64"
                          />
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        !isAddingNew && (
          <div className="text-center py-12 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
            <MapPin className="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-600 font-medium mb-1">No addresses yet</p>
            <p className="text-sm text-gray-500 mb-4">
              Add an address to get started
            </p>
            <button
              onClick={() => setIsAddingNew(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Add First Address
            </button>
          </div>
        )
      )}
    </div>
  );
};
