import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, ChevronUp, ChevronDown } from 'lucide-react';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { ContractPartyRole, PARTY_ROLE_LABELS } from '@/types/contract';
import type { ContractResponse } from '@/types/contract';
import { useFormatDate } from '@/hooks/useFormatDate';
import { getCurrencySymbol } from '@/utils/currencies';

const ROLE_COLORS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]: 'bg-info-bg text-info-text',
  [ContractPartyRole.GUARANTOR]: 'bg-warning-bg text-warning-text',
  [ContractPartyRole.COSIGNER]: 'bg-info-bg text-info-text',
  [ContractPartyRole.EXTRA_TENANT]: 'bg-success-bg text-success-text',
};

const RoleBadge = ({ role }: { role: ContractPartyRole }) => (
  <span
    className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_COLORS[role] ?? 'bg-surface-inset text-text-primary'}`}
  >
    {PARTY_ROLE_LABELS[role] ?? role}
  </span>
);

type SortField =
  | 'startDate'
  | 'rentAmount'
  | 'status'
  | 'property'
  | 'contractType';

interface ContactContractsTableProps {
  contracts: ContractResponse[];
  contactIdentifier: string;
}

export const ContactContractsTable = ({
  contracts,
  contactIdentifier,
}: ContactContractsTableProps) => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();

  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState<SortField>('startDate');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const perPage = 10;

  const filteredAndSorted = useMemo(() => {
    let filtered = [...contracts];
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (contract) =>
          contract.identifier.toLowerCase().includes(search) ||
          contract.property.street.toLowerCase().includes(search) ||
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
        case 'property':
          aVal = a.property.street;
          bVal = b.property.street;
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

  const paginated = useMemo(() => {
    const startIndex = (currentPage - 1) * perPage;
    return filteredAndSorted.slice(startIndex, startIndex + perPage);
  }, [filteredAndSorted, currentPage]);

  const totalPages = Math.ceil(filteredAndSorted.length / perPage);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  if (contracts.length === 0) {
    return null;
  }

  return (
    <div className="mt-6 pt-4 border-t border-border-default">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-text-secondary uppercase">
          All Contracts ({contracts.length})
        </h3>
        {contracts.length > 5 && (
          <div className="relative">
            <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
            <input
              type="text"
              placeholder="Search..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(1);
              }}
              className="pl-8 pr-3 py-1.5 text-sm border border-border-strong rounded focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
        )}
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-border-default text-sm">
          <thead className="bg-surface-page">
            <tr>
              <th
                className="px-4 py-2 text-left text-xs font-medium text-text-secondary uppercase cursor-pointer hover:bg-surface-inset"
                onClick={() => handleSort('property')}
              >
                Property
              </th>
              <th className="px-4 py-2 text-left text-xs font-medium text-text-secondary uppercase">
                Role
              </th>
              <th
                className="px-4 py-2 text-left text-xs font-medium text-text-secondary uppercase cursor-pointer hover:bg-surface-inset"
                onClick={() => handleSort('startDate')}
              >
                <div className="flex items-center gap-1">
                  Period
                  {sortField === 'startDate' &&
                    (sortOrder === 'asc' ? (
                      <ChevronUp className="h-3 w-3" />
                    ) : (
                      <ChevronDown className="h-3 w-3" />
                    ))}
                </div>
              </th>
              <th
                className="px-4 py-2 text-left text-xs font-medium text-text-secondary uppercase cursor-pointer hover:bg-surface-inset"
                onClick={() => handleSort('rentAmount')}
              >
                Rent
              </th>
              <th
                className="px-4 py-2 text-left text-xs font-medium text-text-secondary uppercase cursor-pointer hover:bg-surface-inset"
                onClick={() => handleSort('status')}
              >
                Status
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border-default">
            {paginated.map((contract) => {
              const party = contract.parties?.find(
                (p) => p.contact.identifier === contactIdentifier
              );
              return (
                <tr
                  key={contract.identifier}
                  onClick={() => navigate(`/contracts/${contract.identifier}`)}
                  className="hover:bg-primary-50 dark:hover:bg-primary-950 cursor-pointer transition-colors"
                >
                  <td className="px-4 py-3">{contract.property.street}</td>
                  <td className="px-4 py-3">
                    {party?.role ? (
                      <RoleBadge role={party.role} />
                    ) : (
                      <span className="text-text-muted">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-text-secondary">
                    {formatDate(contract.startDate)}
                    {' — '}
                    {(contract.effectiveEndDate ?? contract.endDate)
                      ? formatDate(
                          (contract.effectiveEndDate ??
                            contract.endDate) as string
                        )
                      : 'Ongoing'}
                  </td>
                  <td className="px-4 py-3 font-medium">
                    {getCurrencySymbol(contract.rentAmountCurrency)}{' '}
                    {contract.rentAmount.toFixed(2)}
                  </td>
                  <td className="px-4 py-3">
                    <ContractStatusBadge status={contract.status} />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-3 text-sm">
          <span className="text-text-secondary">
            Page {currentPage} of {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setCurrentPage(currentPage - 1)}
              disabled={currentPage === 1}
              className="px-2 py-1 border border-border-strong rounded text-xs disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
            >
              Previous
            </button>
            <button
              onClick={() => setCurrentPage(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="px-2 py-1 border border-border-strong rounded text-xs disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
