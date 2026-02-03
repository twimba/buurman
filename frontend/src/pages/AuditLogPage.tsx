import { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAllAuditLogs } from '@/hooks/useDashboard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  History,
  Search,
  Filter,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';

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
      return 'bg-green-100 text-green-800';
    case 'UPDATE':
      return 'bg-blue-100 text-blue-800';
    case 'DELETE':
      return 'bg-red-100 text-red-800';
    case 'RESTORE':
      return 'bg-purple-100 text-purple-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getEntityTypeColor = (entityType: string) => {
  switch (entityType) {
    case 'PROPERTY':
      return 'bg-indigo-100 text-indigo-800';
    case 'TENANT':
      return 'bg-pink-100 text-pink-800';
    case 'CONTRACT':
      return 'bg-yellow-100 text-yellow-800';
    case 'PAYMENT':
      return 'bg-emerald-100 text-emerald-800';
    case 'EXPENSE':
      return 'bg-orange-100 text-orange-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

export const AuditLogPage = () => {
  const navigate = useNavigate();
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
    }, 500);

    return () => clearTimeout(timer);
  }, [searchTerm]);

  // Reset page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [debouncedSearch, entityTypeFilter, actionFilter]);

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
      let aVal: any;
      let bVal: any;

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

  const handleRowClick = (activity: any) => {
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
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-3">
            <History className="h-8 w-8 text-blue-600" />
            <h1 className="text-2xl font-bold text-gray-900">Activity Log</h1>
          </div>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by user, property address, tenant name/email, identifier..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            />
          </div>
        </div>

        {/* Filters */}
        <div className="mb-6 bg-white rounded-lg border border-gray-200 p-4">
          {/* Entity Type Filter */}
          <div className="mb-4">
            <div className="flex items-center gap-2 mb-2">
              <Filter className="h-5 w-5 text-gray-600" />
              <h3 className="font-semibold text-gray-900">Entity Type</h3>
            </div>
            <div className="flex gap-2 flex-wrap">
              {entityTypeFilters.map((filter) => (
                <button
                  key={filter.label}
                  onClick={() => setEntityTypeFilter(filter.value)}
                  className={`px-4 py-2 rounded transition-colors text-sm ${
                    entityTypeFilter === filter.value
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
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
              <Filter className="h-5 w-5 text-gray-600" />
              <h3 className="font-semibold text-gray-900">Action</h3>
            </div>
            <div className="flex gap-2 flex-wrap">
              {actionFilters.map((filter) => (
                <button
                  key={filter.label}
                  onClick={() => setActionFilter(filter.value)}
                  className={`px-4 py-2 rounded transition-colors text-sm ${
                    actionFilter === filter.value
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Activity Count */}
        <p className="text-sm text-gray-600 mb-4">
          {sortedActivities.length}{' '}
          {sortedActivities.length === 1 ? 'activity' : 'activities'}
        </p>

        {/* Activities Table */}
        {sortedActivities.length > 0 ? (
          <>
            <div className="bg-white rounded-lg shadow overflow-hidden mb-4">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('timestamp')}
                    >
                      <div className="flex items-center gap-1">
                        Time
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('entityType')}
                    >
                      <div className="flex items-center gap-1">
                        Type
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('action')}
                    >
                      <div className="flex items-center gap-1">
                        Action
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Description
                    </th>
                    <th
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                      onClick={() => handleSort('userName')}
                    >
                      <div className="flex items-center gap-1">
                        User
                        <ArrowUpDown className="h-4 w-4" />
                      </div>
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Details
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
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
                          className="hover:bg-gray-50 cursor-pointer"
                        >
                          <td
                            className="px-6 py-4 whitespace-nowrap text-sm text-gray-900"
                            onClick={() => handleRowClick(activity)}
                          >
                            <div>
                              {format(
                                new Date(activity.timestamp),
                                'MMM d, yyyy'
                              )}
                            </div>
                            <div className="text-xs text-gray-500">
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
                            className="px-6 py-4 text-sm text-gray-900"
                            onClick={() => handleRowClick(activity)}
                          >
                            {activity.description}
                          </td>
                          <td
                            className="px-6 py-4 whitespace-nowrap text-sm text-gray-900"
                            onClick={() => handleRowClick(activity)}
                          >
                            {activity.userName}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {hasChanges && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  toggleExpanded(activity.id);
                                }}
                                className="text-blue-600 hover:text-blue-800 flex items-center gap-1"
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
                            <td colSpan={6} className="px-6 py-4 bg-gray-50">
                              <div className="space-y-2">
                                <h4 className="text-xs font-semibold text-gray-700 uppercase mb-2">
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
                                          className="bg-white rounded p-3 text-xs"
                                        >
                                          <div className="font-semibold text-gray-700 mb-1">
                                            File Name
                                          </div>
                                          <div className="text-gray-900">
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
                                        className="bg-white rounded p-3 text-xs"
                                      >
                                        <div className="font-semibold text-gray-700 mb-1 capitalize">
                                          {field.replace(/([A-Z])/g, ' $1')}
                                        </div>
                                        <div className="grid grid-cols-2 gap-4">
                                          <div>
                                            <div className="text-gray-500 mb-1">
                                              Before
                                            </div>
                                            <div className="text-gray-900">
                                              {oldValue !== null &&
                                              oldValue !== undefined
                                                ? String(oldValue)
                                                : '—'}
                                            </div>
                                          </div>
                                          <div>
                                            <div className="text-gray-500 mb-1">
                                              After
                                            </div>
                                            <div className="text-gray-900 font-semibold">
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
              <div className="flex items-center justify-between bg-white px-4 py-3 rounded-lg border border-gray-200">
                <div className="text-sm text-gray-700">
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
                    className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Previous
                  </button>
                  <span className="px-3 py-1 text-sm text-gray-700">
                    Page {currentPage} of {totalPages}
                  </span>
                  <button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                    className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                  >
                    Next
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
            <History className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No activities found
            </h3>
            <p className="text-gray-600">
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
