import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useContracts } from '@/hooks/useContractHooks';
import {
  useOccupancyPeriods,
  useDeleteOccupancyPeriod,
} from '@/hooks/useOccupancyPeriodHooks';
import { useFinancings } from '@/hooks/usePropertyFinancialsHooks';
import { OCCUPANCY_TYPE_LABELS } from '@/types/occupancyPeriod';
import { SelfOccupancyModal } from '@/components/properties/SelfOccupancyModal';
import { EndSelfOccupancyModal } from '@/components/properties/EndSelfOccupancyModal';
import { EditSelfOccupancyModal } from '@/components/properties/EditSelfOccupancyModal';
import { FinancingFormModal } from '@/components/properties/financials/modals/FinancingFormModal';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Button, LoadingSpinner } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  FileText,
  Plus,
  Search,
  ChevronUp,
  ChevronDown,
  Home,
  Edit,
  Square,
  Trash2,
} from 'lucide-react';

interface PropertyContractsTabProps {
  propertyId: string;
}

type ContractSortField =
  | 'startDate'
  | 'rentAmount'
  | 'status'
  | 'contact'
  | 'contractType';

export const PropertyContractsTab = ({
  propertyId,
}: PropertyContractsTabProps) => {
  const navigate = useNavigate();
  const { canEditData, canManageMembers } = useTeam();
  const { formatDate } = useFormatDate();

  // Contracts table state
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<ContractSortField>('startDate');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const perPage = 10;

  // Self-occupancy modal state
  const [showSelfOccupancyModal, setShowSelfOccupancyModal] = useState(false);
  const [showEndOccupancyModal, setShowEndOccupancyModal] = useState(false);
  const [endOccupancyPeriodId, setEndOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [deleteOccupancyPeriodId, setDeleteOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [editOccupancyPeriodId, setEditOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [editFinancingId, setEditFinancingId] = useState<string | null>(null);

  // Data fetching
  const {
    data: contractsData,
    isLoading: contractsLoading,
    error: contractsError,
  } = useContracts(
    propertyId ? { propertyIdentifier: propertyId } : undefined
  );
  const contracts = useMemo(
    () => contractsData?.content ?? [],
    [contractsData]
  );
  const { data: occupancyPeriods = [] } = useOccupancyPeriods(propertyId);
  const { data: financings = [] } = useFinancings(propertyId);
  const deleteOccupancyMutation = useDeleteOccupancyPeriod(propertyId);

  // Filtering, sorting, and pagination
  const filteredAndSortedContracts = useMemo(() => {
    if (!contracts) {
      return [];
    }

    let filtered = [...contracts];

    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (contract) =>
          contract.identifier.toLowerCase().includes(search) ||
          `${contract.primaryContact.firstName} ${contract.primaryContact.lastName}`
            .toLowerCase()
            .includes(search) ||
          contract.contractType.toLowerCase().includes(search)
      );
    }

    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

      switch (sortField) {
        case 'startDate':
          aVal = new Date(a.startDate).getTime();
          bVal = new Date(b.startDate).getTime();
          break;
        case 'rentAmount':
          aVal = a.rentAmount;
          bVal = b.rentAmount;
          break;
        case 'status':
          aVal = a.status;
          bVal = b.status;
          break;
        case 'contact':
          aVal = `${a.primaryContact.firstName} ${a.primaryContact.lastName}`;
          bVal = `${b.primaryContact.firstName} ${b.primaryContact.lastName}`;
          break;
        case 'contractType':
          aVal = a.contractType;
          bVal = b.contractType;
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
  }, [contracts, searchTerm, sortField, sortOrder]);

  const paginatedContracts = useMemo(() => {
    const startIndex = (currentPage - 1) * perPage;
    const endIndex = startIndex + perPage;
    return filteredAndSortedContracts.slice(startIndex, endIndex);
  }, [filteredAndSortedContracts, currentPage]);

  const totalPages = Math.ceil(filteredAndSortedContracts.length / perPage);

  const handleSort = (field: ContractSortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const renderSortIcon = (field: ContractSortField) => {
    if (sortField !== field) {
      return null;
    }
    return sortOrder === 'asc' ? (
      <ChevronUp className="h-4 w-4" />
    ) : (
      <ChevronDown className="h-4 w-4" />
    );
  };

  return (
    <>
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-text-primary">
            Contracts ({filteredAndSortedContracts.length})
          </h2>
          <button
            onClick={() => navigate(`/contracts/new?propertyId=${propertyId}`)}
            disabled={!canEditData}
            className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
          >
            <Plus className="h-4 w-4" />
            Add Contract
          </button>
        </div>

        {contractsLoading ? (
          <LoadingSpinner />
        ) : contractsError ? (
          <ErrorMessage message="Failed to load contracts" />
        ) : contracts.length === 0 ? (
          <div className="text-center py-12">
            <FileText className="h-12 w-12 text-text-disabled mx-auto mb-3" />
            <p className="text-text-secondary mb-4">
              No contracts for this property
            </p>
            <button
              onClick={() =>
                navigate(`/contracts/new?propertyId=${propertyId}`)
              }
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-4 w-4" />
              Create First Contract
            </button>
          </div>
        ) : (
          <>
            {/* Search Bar */}
            <div className="mb-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted " />
                <input
                  type="text"
                  placeholder="Search by contract #, contact, type..."
                  value={searchTerm}
                  onChange={(e) => {
                    setSearchTerm(e.target.value);
                    setCurrentPage(1);
                  }}
                  className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Table */}
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-border-default">
                <thead className="bg-surface-page">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSort('startDate')}
                    >
                      <div className="flex items-center gap-1">
                        Contract #
                        {renderSortIcon('startDate')}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSort('contact')}
                    >
                      <div className="flex items-center gap-1">
                        Contact
                        {renderSortIcon('contact')}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSort('contractType')}
                    >
                      <div className="flex items-center gap-1">
                        Type
                        {renderSortIcon('contractType')}
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSort('startDate')}
                    >
                      <div className="flex items-center gap-1">
                        Start Date
                        {renderSortIcon('startDate')}
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      End Date
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                      onClick={() => handleSort('rentAmount')}
                    >
                      <div className="flex items-center gap-1">
                        Rent Amount
                        {renderSortIcon('rentAmount')}
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
                  </tr>
                </thead>
                <tbody className="bg-surface-card divide-y divide-border-default">
                  {paginatedContracts.length === 0 ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="px-6 py-12 text-center text-text-secondary"
                      >
                        No contracts found matching your search
                      </td>
                    </tr>
                  ) : (
                    paginatedContracts.map((contract) => (
                      <tr
                        key={contract.identifier}
                        onClick={() =>
                          navigate(`/contracts/${contract.identifier}`)
                        }
                        className="hover:bg-primary-50 cursor-pointer transition-colors"
                      >
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-primary-500 dark:text-primary-300">
                            #{contract.identifier}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-text-primary">
                            {contract.primaryContact.firstName}{' '}
                            {contract.primaryContact.lastName}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-text-primary">
                            {contract.contractType.replace('_', '')}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-text-primary">
                            {formatDate(contract.startDate)}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-text-primary">
                            {(contract.effectiveEndDate ?? contract.endDate)
                              ? formatDate(
                                  (contract.effectiveEndDate ??
                                    contract.endDate) as string
                                )
                              : '-'}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-text-primary">
                            {contract.rentAmountCurrency}{' '}
                            {contract.rentAmount.toFixed(2)}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ContractStatusBadge status={contract.status} />
                        </td>
                      </tr>
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
                  {Math.min(
                    currentPage * perPage,
                    filteredAndSortedContracts.length
                  )}{' '}
                  of {filteredAndSortedContracts.length} contracts
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
        )}

        {/* Self-Occupancy Periods Section */}
        <div className="mt-8 pt-6 border-t border-border-default">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Home className="h-5 w-5 text-info-text" />
              <h2 className="text-lg font-semibold text-text-primary">
                Self-Occupancy Periods
              </h2>
              {occupancyPeriods.length > 0 && (
                <span className="text-sm text-text-secondary">
                  ({occupancyPeriods.length})
                </span>
              )}
            </div>
            {canEditData && (
              <button
                onClick={() => setShowSelfOccupancyModal(true)}
                className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 text-sm"
              >
                <Plus className="h-4 w-4" />
                Add Self-Occupancy
              </button>
            )}
          </div>

          {occupancyPeriods.length === 0 ? (
            <div className="text-center py-8 border border-dashed border-border-default rounded-lg">
              <Home className="h-10 w-10 text-text-disabled mx-auto mb-3" />
              <p className="text-text-secondary text-sm mb-3">
                No self-occupancy periods recorded
              </p>
              {canEditData && (
                <button
                  onClick={() => setShowSelfOccupancyModal(true)}
                  className="text-primary-500 hover:underline text-sm font-medium inline-flex items-center gap-1"
                >
                  <Plus className="h-3.5 w-3.5" />
                  Record a self-occupancy period
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-border-default">
                <thead className="bg-surface-page">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Period
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Type
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Occupant
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-surface-card divide-y divide-border-default">
                  {occupancyPeriods.map((period) => {
                    const isPeriodActive =
                      !period.endDate ||
                      new Date(period.endDate) >= new Date();
                    return (
                      <tr
                        key={period.identifier}
                        className="hover:bg-surface-page transition-colors"
                      >
                        <td className="px-4 py-3 text-sm text-text-primary">
                          {formatDate(period.startDate)} &mdash;{' '}
                          {period.endDate
                            ? formatDate(period.endDate)
                            : 'Ongoing'}
                        </td>
                        <td className="px-4 py-3 text-sm text-text-secondary">
                          {OCCUPANCY_TYPE_LABELS[period.type]}
                        </td>
                        <td className="px-4 py-3 text-sm text-text-secondary">
                          {period.occupantName ?? (
                            <span className="text-text-muted">—</span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          {isPeriodActive ? (
                            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-info-text">
                              <span className="w-1.5 h-1.5 rounded-full bg-primary-500 animate-pulse" />
                              Active
                            </span>
                          ) : (
                            <span className="text-xs font-medium text-text-secondary">
                              Ended
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-1">
                            <button
                              onClick={() =>
                                setEditOccupancyPeriodId(period.identifier)
                              }
                              title="Edit"
                              className="p-1.5 rounded-lg hover:bg-surface-inset text-text-secondary hover:text-primary-500 transition-colors"
                            >
                              <Edit className="h-4 w-4" />
                            </button>
                            {isPeriodActive && canEditData && (
                              <button
                                onClick={() => {
                                  setEndOccupancyPeriodId(period.identifier);
                                  setShowEndOccupancyModal(true);
                                }}
                                title="End occupancy"
                                className="p-1.5 rounded-lg hover:bg-warning-bg text-text-secondary hover:text-warning-text transition-colors"
                              >
                                <Square className="h-4 w-4" />
                              </button>
                            )}
                            {canManageMembers && (
                              <button
                                onClick={() =>
                                  setDeleteOccupancyPeriodId(period.identifier)
                                }
                                title="Delete"
                                className="p-1.5 rounded-lg hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      {showSelfOccupancyModal && (
        <SelfOccupancyModal
          propertyIdentifier={propertyId}
          onClose={() => setShowSelfOccupancyModal(false)}
        />
      )}

      {showEndOccupancyModal && endOccupancyPeriodId && (
        <EndSelfOccupancyModal
          propertyIdentifier={propertyId}
          periodIdentifier={endOccupancyPeriodId}
          onClose={() => {
            setShowEndOccupancyModal(false);
            setEndOccupancyPeriodId(null);
          }}
        />
      )}

      {editOccupancyPeriodId &&
        (() => {
          const editPeriod = occupancyPeriods.find(
            (p) => p.identifier === editOccupancyPeriodId
          );
          return editPeriod ? (
            <EditSelfOccupancyModal
              propertyIdentifier={propertyId}
              period={editPeriod}
              onClose={() => setEditOccupancyPeriodId(null)}
            />
          ) : null;
        })()}

      {editFinancingId &&
        (() => {
          const editFinancing = financings.find(
            (f) => f.identifier === editFinancingId
          );
          return editFinancing ? (
            <FinancingFormModal
              propertyId={propertyId}
              existing={editFinancing}
              onClose={() => setEditFinancingId(null)}
            />
          ) : null;
        })()}

      {deleteOccupancyPeriodId && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-text-primary mb-4">
              Delete Self-Occupancy Period
            </h3>
            <p className="text-text-secondary mb-2">
              Are you sure you want to delete this self-occupancy period?
            </p>
            <p className="text-sm text-error-text mb-6">
              This action cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <Button
                variant="secondary"
                onClick={() => setDeleteOccupancyPeriodId(null)}
                disabled={deleteOccupancyMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                leftIcon={<Trash2 />}
                onClick={() => {
                  deleteOccupancyMutation.mutate(deleteOccupancyPeriodId, {
                    onSuccess: () => setDeleteOccupancyPeriodId(null),
                  });
                }}
                isLoading={deleteOccupancyMutation.isPending}
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
