import { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAllAuditLogs } from '@/hooks/useDashboard';
import type { RecentActivity } from '@/api/dashboard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  ClipboardList,
  Search,
  Filter,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';

const entityTypeFilters = [
  { value: undefined, label: 'All Types' },
  { value: 'PROPERTY', label: 'Properties' },
  { value: 'TENANT', label: 'Tenants' },
  { value: 'CONTRACT', label: 'Contracts' },
  { value: 'PAYMENT', label: 'Payments' },
  { value: 'EXPENSE', label: 'Expenses' },
];

const actionFilters = [
  { value: undefined, label: 'All Actions' },
  { value: 'CREATE', label: 'Created' },
  { value: 'UPDATE', label: 'Updated' },
  { value: 'DELETE', label: 'Deleted' },
  { value: 'RESTORE', label: 'Restored' },
];

const ITEMS_PER_PAGE = 25;

type SortField = 'timestamp' | 'entityType' | 'action' | 'userName';
type SortOrder = 'asc' | 'desc';

const getActionColor = (action: string) => {
  switch (action) {
    case 'CREATE':
      return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200';
    case 'UPDATE':
      return 'bg-primary-100 dark:bg-primary-500/10 text-blue-800 dark:text-blue-200';
    case 'DELETE':
      return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200';
    case 'RESTORE':
      return 'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-200';
    default:
      return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db]';
  }
};

const getEntityTypeColor = (entityType: string) => {
  switch (entityType) {
    case 'PROPERTY':
      return 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-800 dark:text-indigo-200';
    case 'TENANT':
      return 'bg-pink-100 dark:bg-pink-900/30 text-pink-800 dark:text-pink-200';
    case 'CONTRACT':
      return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-200';
    case 'PAYMENT':
      return 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-200';
    case 'EXPENSE':
      return 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200';
    default:
      return 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db]';
  }
};

export const AuditLogPage = () => {
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const [entityTypeFilter, setEntityTypeFilter] = useState<string | undefined>(
    undefined
  );
  const [actionFilter, setActionFilter] = useState<string | undefined>(
    undefined
  );
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [sortField, setSortField] = useState<SortField>('timestamp');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setCurrentPage(1);
    }, 500);

    return () => clearTimeout(timer);
  }, [searchTerm]);

  const {
    data: activities,
    isLoading,
    error,
  } = useAllAuditLogs({
    entityType: entityTypeFilter,
    action: actionFilter,
    search: debouncedSearch,
  });

  const sortedActivities = useMemo(() => {
    if (!activities) return [];

    const sorted = [...activities];

    sorted.sort((a, b) => {
      let aVal: string | number;
      let bVal: string | number;

      switch (sortField) {
        case 'timestamp':
          aVal = new Date(a.timestamp).getTime();
          bVal = new Date(b.timestamp).getTime();
          break;
        case 'entityType':
          aVal = a.entityType;
          bVal = b.entityType;
          break;
        case 'action':
          aVal = a.action;
          bVal = b.action;
          break;
        case 'userName':
          aVal = a.userName;
          bVal = b.userName;
          break;
        default:
          return 0;
      }

      if (sortOrder === 'asc') {
        return aVal > bVal ? 1 : -1;
      } else {
        return aVal < bVal ? 1 : -1;
      }
    });

    return sorted;
  }, [activities, sortField, sortOrder]);

  const paginatedActivities = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    return sortedActivities.slice(startIndex, startIndex + ITEMS_PER_PAGE);
  }, [sortedActivities, currentPage]);

  const totalPages = Math.ceil(sortedActivities.length / ITEMS_PER_PAGE);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('desc');
    }
  };

  const toggleExpanded = (id: string) => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedItems(newExpanded);
  };

  const handleRowClick = (activity: RecentActivity) => {
    // Navigate to the entity detail page
    const entityType = activity.entityType.toLowerCase();
    if (entityType === 'property') {
      navigate(`/properties/${activity.entityId}`);
    } else if (entityType === 'tenant') {
      navigate(`/tenants/${activity.entityId}`);
    } else if (entityType === 'contract') {
      navigate(`/contracts/${activity.entityId}`);
    } else if (entityType === 'payment') {
      navigate(`/payments/${activity.entityId}`);
    } else if (entityType === 'expense') {
      navigate(`/expenses/${activity.entityId}`);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load activity logs" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <ClipboardList className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Activity Log
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
              Track all changes and actions across your data
            </p>
          </div>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by user, property address, tenant name/email, identifier..."
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
            />
          </div>
        </div>

        {/* Filters */}
        <div className="mb-6 bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Entity Type Filter */}
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
                <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Entity Type
                </h3>
              </div>
              <div className="flex gap-2 flex-wrap">
                {entityTypeFilters.map((filter) => (
                  <button
                    key={filter.label}
                    onClick={() => {
                      setEntityTypeFilter(filter.value);
                      setCurrentPage(1);
                    }}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      entityTypeFilter === filter.value
                        ? 'bg-[#5c7cfa] text-white'
                        : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54]'
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Action Filter */}
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Filter className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
                <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Action
                </h3>
              </div>
              <div className="flex gap-2 flex-wrap">
                {actionFilters.map((filter) => (
                  <button
                    key={filter.label}
                    onClick={() => {
                      setActionFilter(filter.value);
                      setCurrentPage(1);
                    }}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      actionFilter === filter.value
                        ? 'bg-[#5c7cfa] text-white'
                        : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:bg-[#1e2130] dark:hover:bg-[#3a3f54]'
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Activity Count */}
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
          {sortedActivities.length}{' '}
          {sortedActivities.length === 1 ? 'activity' : 'activities'}
        </p>

        {/* Activities Table */}
        {sortedActivities.length > 0 ? (
          <>
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
                <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('timestamp')}
                    >
                      <div className="flex items-center gap-1">
                        Time
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('entityType')}
                    >
                      <div className="flex items-center gap-1">
                        Type
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('action')}
                    >
                      <div className="flex items-center gap-1">
                        Action
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Description
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                      onClick={() => handleSort('userName')}
                    >
                      <div className="flex items-center gap-1">
                        User
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                      Details
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
                  {paginatedActivities.map((activity) => {
                    const isExpanded = expandedItems.has(activity.id);
                    const hasChanges =
                      activity.action === 'UPDATE' &&
                      activity.changedFields &&
                      Object.keys(activity.changedFields).length > 0;

                    return (
                      <>
                        <tr
                          key={activity.id}
                          className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                        >
                          <td
                            className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                            onClick={() => handleRowClick(activity)}
                          >
                            <div>{formatDate(activity.timestamp)}</div>
                            <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                              {format(new Date(activity.timestamp), 'HH:mm:ss')}
                            </div>
                          </td>
                          <td
                            className="px-6 py-4 whitespace-nowrap"
                            onClick={() => handleRowClick(activity)}
                          >
                            <span
                              className={`px-2 py-1 text-xs font-semibold rounded ${getEntityTypeColor(activity.entityType)}`}
                            >
                              {activity.entityType}
                            </span>
                          </td>
                          <td
                            className="px-6 py-4 whitespace-nowrap"
                            onClick={() => handleRowClick(activity)}
                          >
                            <span
                              className={`px-2 py-1 text-xs font-semibold rounded ${getActionColor(activity.action)}`}
                            >
                              {activity.action}
                            </span>
                          </td>
                          <td
                            className="px-6 py-4 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                            onClick={() => handleRowClick(activity)}
                          >
                            {activity.description}
                          </td>
                          <td
                            className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                            onClick={() => handleRowClick(activity)}
                          >
                            {activity.userName}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
                            {hasChanges && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  toggleExpanded(activity.id);
                                }}
                                className="text-primary-500 dark:text-primary-300 hover:text-blue-800 dark:hover:text-blue-300 flex items-center gap-1"
                              >
                                {isExpanded ? (
                                  <>
                                    <ChevronUp className="h-4 w-4" />
                                    Hide
                                  </>
                                ) : (
                                  <>
                                    <ChevronDown className="h-4 w-4" />
                                    Show
                                  </>
                                )}
                              </button>
                            )}
                          </td>
                        </tr>
                        {isExpanded && hasChanges && (
                          <tr key={`${activity.id}-details`}>
                            <td
                              colSpan={6}
                              className="px-6 py-4 bg-[#f8f9fc] dark:bg-[#0c0d14]"
                            >
                              <div className="space-y-2">
                                <h4 className="text-xs font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase mb-2">
                                  Changed Fields
                                </h4>
                                {Object.entries(activity.changedFields!).map(
                                  ([field, value]) => {
                                    // Skip internal fields
                                    if (field === 'documentCount') return null;

                                    // Special handling for document operations
                                    if (
                                      field === 'documentAdded' ||
                                      field === 'documentRemoved'
                                    ) {
                                      return (
                                        <div
                                          key={field}
                                          className="bg-white dark:bg-[#14161f] rounded p-3 text-xs"
                                        >
                                          <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1">
                                            File Name
                                          </div>
                                          <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                            {String(value)}
                                          </div>
                                        </div>
                                      );
                                    }

                                    const oldValue =
                                      activity.oldValues?.[field];
                                    const newValue =
                                      activity.newValues?.[field];

                                    return (
                                      <div
                                        key={field}
                                        className="bg-white dark:bg-[#14161f] rounded p-3 text-xs"
                                      >
                                        <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1 capitalize">
                                          {field.replace(/([A-Z])/g, ' $1')}
                                        </div>
                                        <div className="grid grid-cols-2 gap-4">
                                          <div>
                                            <div className="text-[#6b7194] dark:text-[#8b90a8] mb-1">
                                              Before
                                            </div>
                                            <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                              {oldValue !== null &&
                                              oldValue !== undefined
                                                ? String(oldValue)
                                                : '—'}
                                            </div>
                                          </div>
                                          <div>
                                            <div className="text-[#6b7194] dark:text-[#8b90a8] mb-1">
                                              After
                                            </div>
                                            <div className="text-[#1a1d2e] dark:text-[#eef0f6] font-semibold">
                                              {newValue !== null &&
                                              newValue !== undefined
                                                ? String(newValue)
                                                : '—'}
                                            </div>
                                          </div>
                                        </div>
                                      </div>
                                    );
                                  }
                                )}
                              </div>
                            </td>
                          </tr>
                        )}
                      </>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between bg-white dark:bg-[#14161f] px-4 py-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
                <div className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                  Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} to{' '}
                  {Math.min(
                    currentPage * ITEMS_PER_PAGE,
                    sortedActivities.length
                  )}{' '}
                  of {sortedActivities.length} activities
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Previous
                  </button>
                  <span className="px-3 py-1 text-sm text-[#3d4463] dark:text-[#c4c8db]">
                    Page {currentPage} of {totalPages}
                  </span>
                  <button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                    className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1 text-[#3d4463] dark:text-[#c4c8db]"
                  >
                    Next
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-12 text-center">
            <ClipboardList className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No activities found
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8]">
              {entityTypeFilter || actionFilter || searchTerm
                ? 'Try adjusting your filters or search'
                : 'No activity has been logged yet'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
