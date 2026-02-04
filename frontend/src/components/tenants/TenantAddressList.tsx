import { useState } from 'react';
import {
  Plus,
  MapPin,
  Edit,
  Trash2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  useTenantAddresses,
  useCreateTenantAddress,
  useDeleteTenantAddress,
} from '@/hooks/useTenantHooks';
import * as tenantsApi from '@/api/tenants';
import {
  AddressType,
  AddressStatus,
  CreateTenantAddressRequest,
  UpdateTenantAddressRequest,
} from '@/types/tenant';
import { AddressForm } from './AddressForm';
import { AddressMap } from '../common/AddressMap';
import { useTeam } from '@/context/TeamContext';

interface TenantAddressListProps {
  tenantId: string;
}

export const TenantAddressList = ({ tenantId }: TenantAddressListProps) => {
  const { canEditData } = useTeam();
  const queryClient = useQueryClient();
  const { data: addresses, isLoading } = useTenantAddresses(tenantId);
  const createMutation = useCreateTenantAddress(tenantId);
  const deleteMutation = useDeleteTenantAddress(tenantId);
  const updateMutation = useMutation({
    mutationFn: ({
      addressId,
      data,
    }: {
      addressId: string;
      data: UpdateTenantAddressRequest;
    }) => tenantsApi.updateTenantAddress(tenantId, addressId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tenantAddresses', tenantId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
    },
  });
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
    await updateMutation.mutateAsync({ addressId, data });
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
        return 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300';
      case AddressType.MAILING:
        return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300';
      case AddressType.RELATIVE:
        return 'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300';
      case AddressType.WORK:
        return 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-300';
      case AddressType.HISTORIC:
        return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db]';
      default:
        return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db]';
    }
  };

  const getStatusBadgeColor = (status: AddressStatus) => {
    switch (status) {
      case AddressStatus.ACTIVE:
        return 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400';
      case AddressStatus.INACTIVE:
        return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]';
      default:
        return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]';
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
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-[#5c7cfa]"></div>
        <p className="mt-2 text-[#6b7194] dark:text-[#8b90a8]">Loading addresses...</p>
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
            disabled={!canEditData}
            className="inline-flex items-center gap-2 px-4 py-2 bg-[#5c7cfa] text-white rounded hover:bg-[#4c6ef5] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
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
              className="bg-white dark:bg-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg overflow-hidden"
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
                    isLoading={updateMutation.isPending}
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
                    {canEditData && (
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setEditingAddressId(address.id)}
                          className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded"
                          title="Edit address"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteAddress(address.id)}
                          className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-[#1e2130] rounded"
                          title="Delete address"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Address Details */}
                  <div className="flex items-start gap-3 mb-3">
                    <MapPin className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-0.5 flex-shrink-0" />
                    <div className="text-[#3d4463] dark:text-[#c4c8db]">
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
                        className="flex items-center gap-2 text-sm text-[#5c7cfa] hover:text-[#4263eb]"
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
          <div className="text-center py-12 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg border-2 border-dashed border-[#c9cfd9] dark:border-[#3a3f54]">
            <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
            <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
              No addresses yet
            </p>
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180] mb-4">
              Add an address to get started
            </p>
            <button
              onClick={() => setIsAddingNew(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-[#5c7cfa] text-white rounded hover:bg-blue-700"
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
