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
  useContactAddresses,
  useCreateContactAddress,
  useDeleteContactAddress,
} from '@/hooks/useContactHooks';
import * as contactsApi from '@/api/contacts';
import {
  AddressType,
  AddressStatus,
  CreateContactAddressRequest,
  UpdateContactAddressRequest,
} from '@/types/contact';
import { AddressForm } from './AddressForm';
import { InteractiveMap } from '../common/InteractiveMap';
import { useTeam } from '@/context/TeamContext';
import { getErrorMessage } from '@/utils/errorMessages';
import { ConfirmDialog, useToast } from '@buurman/ui';
import { useTranslation } from 'react-i18next';

interface ContactAddressListProps {
  contactId: string;
}

export const ContactAddressList = ({ contactId }: ContactAddressListProps) => {
  const { t } = useTranslation('tenants');
  const { canEditData } = useTeam();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const { data: addresses, isLoading } = useContactAddresses(contactId);
  const createMutation = useCreateContactAddress(contactId);
  const deleteMutation = useDeleteContactAddress(contactId);
  const updateMutation = useMutation({
    mutationFn: ({
      addressId,
      data,
    }: {
      addressId: string;
      data: UpdateContactAddressRequest;
    }) => contactsApi.updateContactAddress(contactId, addressId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['contactAddresses', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
  const [editingAddressId, setEditingAddressId] = useState<string | null>(null);
  const [isAddingNew, setIsAddingNew] = useState(false);
  const [expandedMapId, setExpandedMapId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  // Table state
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<
    'street' | 'city' | 'countryCode' | 'addressType' | 'status'
  >('street');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [currentPage, setCurrentPage] = useState(1);
  const perPage = 10;

  const handleCreateAddress = async (data: CreateContactAddressRequest) => {
    await createMutation.mutateAsync(data);
    setIsAddingNew(false);
  };

  const handleUpdateAddress = async (
    addressId: string,
    data: UpdateContactAddressRequest
  ) => {
    await updateMutation.mutateAsync({ addressId, data });
    setEditingAddressId(null);
  };

  const handleDeleteAddress = async () => {
    if (!pendingDeleteId) {
      return;
    }
    await deleteMutation.mutateAsync(pendingDeleteId);
    setPendingDeleteId(null);
  };

  const getTypeBadgeColor = (type: AddressType) => {
    switch (type) {
      case AddressType.CURRENT:
        return 'bg-info-bg text-info-text';
      case AddressType.MAILING:
        return 'bg-success-bg text-success-text';
      case AddressType.RELATIVE:
        return 'bg-info-bg text-info-text';
      case AddressType.WORK:
        return 'bg-warning-bg text-warning-text';
      case AddressType.HISTORIC:
        return 'bg-surface-inset text-text-primary';
      default:
        return 'bg-surface-inset text-text-primary';
    }
  };

  const getStatusBadgeColor = (status: AddressStatus) => {
    switch (status) {
      case AddressStatus.ACTIVE:
        return 'bg-success-bg text-success-text';
      case AddressStatus.INACTIVE:
        return 'bg-surface-inset text-text-secondary';
      default:
        return 'bg-surface-inset text-text-secondary';
    }
  };

  const getAddressTypeLabel = (type: AddressType) => {
    switch (type) {
      case AddressType.CURRENT:
        return t('addresses.typeLabels.CURRENT');
      case AddressType.MAILING:
        return t('addresses.typeLabels.MAILING');
      case AddressType.RELATIVE:
        return t('addresses.typeLabels.RELATIVE');
      case AddressType.WORK:
        return t('addresses.typeLabels.WORK');
      case AddressType.HISTORIC:
        return t('addresses.typeLabels.HISTORIC');
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
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
        <p className="mt-2 text-text-secondary">
          {t('addresses.loadingAddresses')}
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
            className="inline-flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
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
        <div className="border border-border-default rounded-lg p-6 bg-surface-card">
          <AddressForm
            address={addresses.find((a) => a.identifier === editingAddressId)}
            onSubmit={(data) =>
              handleUpdateAddress(
                editingAddressId,
                data as UpdateContactAddressRequest
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
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted " />
            <input
              type="text"
              placeholder={t('addresses.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-border-default">
              <thead className="bg-surface-page">
                <tr>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('street')}
                  >
                    <div className="flex items-center gap-1">
                      Street
                      {renderSortIcon('street')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('city')}
                  >
                    <div className="flex items-center gap-1">
                      City
                      {renderSortIcon('city')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('countryCode')}
                  >
                    <div className="flex items-center gap-1">
                      Country
                      {renderSortIcon('countryCode')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('addressType')}
                  >
                    <div className="flex items-center gap-1">
                      Type
                      {renderSortIcon('addressType')}
                    </div>
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                    onClick={() => handleSort('status')}
                  >
                    <div className="flex items-center gap-1">
                      Status
                      {renderSortIcon('status')}
                    </div>
                  </th>
                  {canEditData && (
                    <th className="px-6 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody className="bg-surface-card divide-y divide-border-default">
                {paginated.length === 0 ? (
                  <tr>
                    <td
                      colSpan={canEditData ? 6 : 5}
                      className="px-6 py-12 text-center text-text-secondary"
                    >
                      {t('addresses.noMatch')}
                    </td>
                  </tr>
                ) : (
                  paginated.map((address) => (
                    <Fragment key={address.identifier}>
                      <tr className="hover:bg-surface-inset transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-text-primary">
                            {address.street}
                          </div>
                          {address.postalCode && (
                            <div className="text-xs text-text-secondary">
                              {address.postalCode}
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                          {address.city}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
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
                                  className="p-2 text-text-secondary hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-950 rounded"
                                  title={t('addresses.showMap')}
                                >
                                  <MapPin className="h-4 w-4" />
                                </button>
                              )}
                              <button
                                onClick={() =>
                                  setEditingAddressId(address.identifier)
                                }
                                className="p-2 text-text-secondary hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-950 rounded"
                                title={t('addresses.editAddressTitle')}
                              >
                                <Edit className="h-4 w-4" />
                              </button>
                              <button
                                onClick={() =>
                                  setPendingDeleteId(address.identifier)
                                }
                                className="p-2 text-text-secondary hover:text-error-text hover:bg-error-bg rounded"
                                title={t('addresses.deleteAddressTitle')}
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
            <div className="flex items-center justify-between mt-4 pt-4 border-t border-border-default">
              <div className="text-sm text-text-secondary">
                Showing {(currentPage - 1) * perPage + 1} to{' '}
                {Math.min(currentPage * perPage, filteredAndSorted.length)} of{' '}
                {filteredAndSorted.length} addresses
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                >
                  Previous
                </button>
                <span className="px-3 py-1 text-sm text-text-secondary">
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      ) : (
        !isAddingNew && (
          <div className="text-center py-12 bg-surface-page rounded-lg border-2 border-dashed border-border-strong">
            <MapPin className="h-12 w-12 text-text-disabled mx-auto mb-3" />
            <p className="text-text-secondary font-medium mb-1">
              {t('addresses.empty')}
            </p>
            <p className="text-sm text-text-secondary mb-4">
              {t('addresses.emptyDescription')}
            </p>
            <button
              onClick={() => setIsAddingNew(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded hover:bg-primary-600"
            >
              <Plus className="h-4 w-4" />
              Add First Address
            </button>
          </div>
        )
      )}

      {pendingDeleteId !== null && (
        <ConfirmDialog
          title={t('addresses.deleteTitle')}
          message={t('addresses.deleteMessage')}
          confirmLabel={t('common:buttons.delete')}
          variant="danger"
          isLoading={deleteMutation.isPending}
          onConfirm={handleDeleteAddress}
          onCancel={() => setPendingDeleteId(null)}
        />
      )}
    </div>
  );
};
