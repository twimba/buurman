import { useState, useMemo, Fragment } from 'react';
import {
  Plus,
  MapPin,
  Edit,
  Trash2,
  ChevronDown,
  ChevronUp,
  Search,
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
import { InteractiveMap } from '../common/InteractiveMap';
import { useTeam } from '@/context/TeamContext';
import { useToast } from '@/context/ToastContext';
import { getErrorMessage } from '@/utils/errorMessages';

interface TenantAddressListProps {
  tenantId: string;
}

export const TenantAddressList = ({ tenantId }: TenantAddressListProps) => {
  const { canEditData } = useTeam();
  const { showToast } = useToast();
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
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [isAddingNew, setIsAddingNew] = useState(false);
  const [expandedMapId, setExpandedMapId] = useState<string | null>(null);

  // Table state
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<
    'street' | 'city' | 'countryCode' | 'addressType' | 'status'
  >('street');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [currentPage, setCurrentPage] = useState(1);
  const perPage = 10;

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

  // Filtering, sorting, pagination
  const filteredAndSorted = useMemo(() => {
    if (!addresses) {
      return [];
    }
    let filtered = [...addresses];
    if (searchTerm) {
      const s = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (a) =>
          a.street.toLowerCase().includes(s) ||
          a.city.toLowerCase().includes(s) ||
          a.countryCode.toLowerCase().includes(s) ||
          a.addressType.toLowerCase().includes(s) ||
          (a.postalCode && a.postalCode.toLowerCase().includes(s))
      );
    }
    filtered.sort((a, b) => {
      let aVal: string, bVal: string;
      switch (sortField) {
        case 'street':
          aVal = a.street;
          bVal = b.street;
          break;
        case 'city':
          aVal = a.city;
          bVal = b.city;
          break;
        case 'countryCode':
          aVal = a.countryCode;
          bVal = b.countryCode;
          break;
        case 'addressType':
          aVal = a.addressType;
          bVal = b.addressType;
          break;
        case 'status':
          aVal = a.status;
          bVal = b.status;
          break;
        default:
          return 0;
      }
      if (aVal < bVal) {
        return sortOrder === 'asc' ? -1 : 1;
      }
      if (aVal > bVal) {
        return sortOrder === 'asc' ? 1 : -1;
      }
      return 0;
    });
    return filtered;
  }, [addresses, searchTerm, sortField, sortOrder]);

  const paginated = useMemo(() => {
    const start = (currentPage - 1) * perPage;
    return filteredAndSorted.slice(start, start + perPage);
  }, [filteredAndSorted, currentPage]);

  const totalPages = Math.ceil(filteredAndSorted.length / perPage);

  const handleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const renderSortIcon = (field: typeof sortField) =>
    sortField === field ? (
      sortOrder === 'asc' ? (
        <ChevronUp className="h-4 w-4" />
      ) : (
        <ChevronDown className="h-4 w-4" />
      )
    ) : null;

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-[#5c7cfa]"></div>
        <p className="mt-2 text-[#6b7194] dark:text-[#8b90a8]">
          Loading addresses...
        </p>
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

      {/* Edit Form (shown above table) */}
      {editingAddressId && addresses && (
        <div className="border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg p-6 bg-white dark:bg-[#14161f]">
          <AddressForm
            address={addresses.find((a) => a.identifier === editingAddressId)}
            onSubmit={(data) =>
              handleUpdateAddress(
                editingAddressId,
                data as UpdateTenantAddressRequest
              )
            }
            onCancel={() => setEditingAddressId(null)}
            isLoading={updateMutation.isPending}
          />
        </div>
      )}

      {/* Address Table */}
      {addresses && addresses.length > 0 ? (
        <>
          {/* Search Bar */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              placeholder="Search by street, city, country, type..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
              <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                <tr>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSort('street')}
                  >
                    <div className="flex items-center gap-1">
                      Street
                      {renderSortIcon('street')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSort('city')}
                  >
                    <div className="flex items-center gap-1">
                      City
                      {renderSortIcon('city')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSort('countryCode')}
                  >
                    <div className="flex items-center gap-1">
                      Country
                      {renderSortIcon('countryCode')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSort('addressType')}
                  >
                    <div className="flex items-center gap-1">
                      Type
                      {renderSortIcon('addressType')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                    onClick={() => handleSort('status')}
                  >
                    <div className="flex items-center gap-1">
                      Status
                      {renderSortIcon('status')}
                    </div>
                  </th>
                  {canEditData && (
                    <th className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                {paginated.length === 0 ? (
                  <tr>
                    <td
                      colSpan={canEditData ? 6 : 5}
                      className="px-6 py-12 text-center text-[#6b7194] dark:text-[#8b90a8]"
                    >
                      No addresses found matching your search
                    </td>
                  </tr>
                ) : (
                  paginated.map((address) => (
                    <Fragment key={address.identifier}>
                      <tr className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                            {address.street}
                          </div>
                          {address.postalCode && (
                            <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                              {address.postalCode}
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                          {address.city}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                          {address.countryCode}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2.5 py-1 rounded-full text-xs font-medium ${getTypeBadgeColor(address.addressType)}`}
                          >
                            {getAddressTypeLabel(address.addressType)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2.5 py-1 rounded-full text-xs font-medium ${getStatusBadgeColor(address.status)}`}
                          >
                            {address.status}
                          </span>
                        </td>
                        {canEditData && (
                          <td className="px-6 py-4 whitespace-nowrap text-right">
                            <div className="flex items-center justify-end gap-1">
                              {address.latitude && address.longitude && (
                                <button
                                  onClick={() =>
                                    setExpandedMapId(
                                      expandedMapId === address.identifier
                                        ? null
                                        : address.identifier
                                    )
                                  }
                                  className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded"
                                  title="Show map"
                                >
                                  <MapPin className="h-4 w-4" />
                                </button>
                              )}
                              <button
                                onClick={() =>
                                  setEditingAddressId(address.identifier)
                                }
                                className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-[#5c7cfa] dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-[#1e2130] rounded"
                                title="Edit address"
                              >
                                <Edit className="h-4 w-4" />
                              </button>
                              <button
                                onClick={() =>
                                  handleDeleteAddress(address.identifier)
                                }
                                className="p-2 text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-[#1e2130] rounded"
                                title="Delete address"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </td>
                        )}
                      </tr>
                      {/* Expanded map row */}
                      {expandedMapId === address.identifier &&
                        address.latitude &&
                        address.longitude && (
                          <tr key={`${address.identifier}-map`}>
                            <td colSpan={canEditData ? 6 : 5} className="p-4">
                              <InteractiveMap
                                street={address.street}
                                city={address.city}
                                latitude={address.latitude}
                                longitude={address.longitude}
                                height="h-64"
                              />
                            </td>
                          </tr>
                        )}
                    </Fragment>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
              <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Showing {(currentPage - 1) * perPage + 1} to{' '}
                {Math.min(currentPage * perPage, filteredAndSorted.length)} of{' '}
                {filteredAndSorted.length} addresses
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:text-[#c4c8db]"
                >
                  Previous
                </button>
                <span className="px-3 py-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:text-[#c4c8db]"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      ) : (
        !isAddingNew && (
          <div className="text-center py-12 bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg border-2 border-dashed border-[#c9cfd9] dark:border-[#3a3f54]">
            <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
            <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
              No addresses yet
            </p>
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
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
