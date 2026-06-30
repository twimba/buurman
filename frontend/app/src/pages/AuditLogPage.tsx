import { useState, useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  DataList,
  LoadingSpinner,
  Pagination,
  RefreshButton,
  RichTextDisplay,
} from '@buurman/ui';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { useNavigate } from 'react-router-dom';
import { useAllAuditLogs } from '@/hooks/useDashboard';
import { usePagination } from '@/hooks/usePagination';
import type { RecentActivity } from '@/api/dashboard';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  ClipboardList,
  Search,
  Filter,
  ArrowUpDown,
  ChevronDown,
  ChevronUp,
  Eye,
} from 'lucide-react';
import { format } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';

// Moved inside component as useMemo

const getActionColor = (action: string) => {
  switch (action) {
    case 'CREATE':
      return 'bg-success-bg text-success-text';
    case 'UPDATE':
      return 'bg-info-bg text-info-text';
    case 'DELETE':
      return 'bg-error-bg text-error-text';
    case 'RESTORE':
      return 'bg-info-bg text-info-text';
    default:
      return 'bg-surface-inset text-text-primary';
  }
};

const getEntityTypeColor = (entityType: string) => {
  switch (entityType) {
    case 'PROPERTY':
      return 'bg-info-bg text-info-text';
    case 'CONTACT':
      return 'bg-info-bg text-info-text';
    case 'CONTRACT':
      return 'bg-warning-bg text-warning-text';
    case 'PAYMENT':
      return 'bg-success-bg text-success-text';
    case 'EXPENSE':
      return 'bg-warning-bg text-warning-text';
    default:
      return 'bg-surface-inset text-text-primary';
  }
};

export const AuditLogPage = () => {
  const { t } = useTranslation('admin');
  const navigate = useNavigate();
  const { formatDate } = useFormatDate();
  const { canEditTeamSettings, isLoading: isTeamLoading } = useTeam();

  const entityTypeFilters = useMemo(
    () => [
      { value: undefined, label: t('common:auditLog.allTypes') },
      { value: 'PROPERTY', label: t('common:auditLog.entityTypes.property') },
      { value: 'CONTACT', label: t('common:auditLog.entityTypes.contact') },
      { value: 'CONTRACT', label: t('common:auditLog.entityTypes.contract') },
      { value: 'PAYMENT', label: t('common:auditLog.entityTypes.payment') },
      { value: 'EXPENSE', label: t('common:auditLog.entityTypes.expense') },
    ],
    [t]
  );

  const actionFilters = useMemo(
    () => [
      { value: undefined, label: t('common:auditLog.allActions') },
      { value: 'CREATE', label: t('common:auditLog.actions.created') },
      { value: 'UPDATE', label: t('common:auditLog.actions.updated') },
      { value: 'DELETE', label: t('common:auditLog.actions.deleted') },
      { value: 'RESTORE', label: t('common:auditLog.actions.restored') },
    ],
    [t]
  );

  useEffect(() => {
    if (!isTeamLoading && !canEditTeamSettings) {
      navigate('/dashboard', { replace: true });
    }
  }, [isTeamLoading, canEditTeamSettings, navigate]);
  const [entityTypeFilter, setEntityTypeFilter] = useState<string | undefined>(
    undefined
  );
  const [actionFilter, setActionFilter] = useState<string | undefined>(
    undefined
  );
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({ defaultSort: 'timestamp' });

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      resetPage();
    }, 500);

    return () => clearTimeout(timer);
  }, [searchTerm, resetPage]);

  const {
    data: activitiesData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = useAllAuditLogs({
    entityType: entityTypeFilter,
    action: actionFilter,
    search: debouncedSearch,
    ...pageParams,
  });

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
      navigate(`/properties/${activity.entityIdentifier}`);
    } else if (entityType === 'contact') {
      navigate(`/contacts/${activity.entityIdentifier}`);
    } else if (entityType === 'contract') {
      navigate(`/contracts/${activity.entityIdentifier}`);
    } else if (entityType === 'payment') {
      navigate(`/payments/${activity.entityIdentifier}`);
    } else if (entityType === 'expense') {
      navigate(`/expenses/${activity.entityIdentifier}`);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-full bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-full bg-background p-8">
        <ErrorMessage message={t('auditLog.failedToLoad')} />
      </div>
    );
  }

  return (
    <div className="min-h-full bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <ClipboardList className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">
                {t('common:auditLog.title')}
              </h1>
            </div>
            <p className="text-text-secondary ml-11">
              {t('common:auditLog.subtitle')}
            </p>
          </div>
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-text-muted " />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={t('common:auditLog.searchPlaceholder')}
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-lg bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
          </div>
        </div>

        {/* Filters */}
        <div className="mb-6 bg-surface-card rounded-lg border border-border-default p-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Entity Type Filter */}
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Filter className="h-5 w-5 text-text-secondary " />
                <h3 className="font-semibold text-text-primary">
                  {t('common:auditLog.entityType')}
                </h3>
              </div>
              <div className="flex gap-2 flex-wrap">
                {entityTypeFilters.map((filter) => (
                  <button
                    key={filter.label}
                    onClick={() => {
                      setEntityTypeFilter(filter.value);
                      resetPage();
                    }}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      entityTypeFilter === filter.value
                        ? 'bg-primary-500 text-white'
                        : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
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
                <Filter className="h-5 w-5 text-text-secondary " />
                <h3 className="font-semibold text-text-primary">
                  {t('common:auditLog.action')}
                </h3>
              </div>
              <div className="flex gap-2 flex-wrap">
                {actionFilters.map((filter) => (
                  <button
                    key={filter.label}
                    onClick={() => {
                      setActionFilter(filter.value);
                      resetPage();
                    }}
                    className={`px-4 py-2 rounded transition-colors text-sm ${
                      actionFilter === filter.value
                        ? 'bg-primary-500 text-white'
                        : 'bg-surface-inset text-text-secondary hover:bg-surface-raised'
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
        <p className="text-sm text-text-secondary mb-4">
          {t('common:auditLog.activityCount', {
            count: activitiesData?.totalElements ?? 0,
          })}
        </p>

        {/* Activities — Mobile card list (<md). md+ shows the existing table below. */}
        {activitiesData?.content && activitiesData.content.length > 0 && (
          <ul className="md:hidden space-y-3 mb-4">
            {activitiesData.content.map((activity) => {
              const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}-m`;
              return (
                <li key={activityKey}>
                  <button
                    type="button"
                    onClick={() => handleRowClick(activity)}
                    className="block w-full text-left bg-surface-card rounded-lg border border-border-default p-4 min-h-touch hover:border-primary-300 transition-colors focus-ring"
                  >
                    <DataList
                      title={activity.description}
                      trailing={
                        <span
                          className={`px-2 py-1 text-xs font-semibold rounded ${getActionColor(activity.action)}`}
                        >
                          {t(
                            `common:auditLog.actions.${activity.action.toLowerCase()}d`,
                            { defaultValue: activity.action }
                          )}
                        </span>
                      }
                      items={[
                        {
                          label: t('common:auditLog.table.time'),
                          value: `${formatDate(activity.timestamp)} ${format(
                            new Date(activity.timestamp),
                            'HH:mm'
                          )}`,
                        },
                        {
                          label: t('common:auditLog.table.type'),
                          value: t(
                            `common:auditLog.entityTypes.${activity.entityType.toLowerCase()}`,
                            { defaultValue: activity.entityType }
                          ),
                        },
                        {
                          label: t('common:auditLog.table.user'),
                          value: activity.userName,
                        },
                      ]}
                    />
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        {/* Activities Table — md+ */}
        {activitiesData?.content && activitiesData.content.length > 0 ? (
          <>
            <div className="hidden md:block bg-surface-card rounded-lg shadow-sm overflow-hidden mb-4">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-border-default">
                  <thead className="bg-surface-page">
                    <tr>
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('timestamp')}
                      >
                        <div className="flex items-center gap-1">
                          {t('common:auditLog.table.time')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('entityType')}
                      >
                        <div className="flex items-center gap-1">
                          {t('common:auditLog.table.type')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th
                        className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                        onClick={() => handleSortChange('action')}
                      >
                        <div className="flex items-center gap-1">
                          {t('common:auditLog.table.action')}
                          <ArrowUpDown className="h-4 w-4" />
                        </div>
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                        {t('common:auditLog.table.description')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                        {t('common:auditLog.table.user')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                        {t('common:auditLog.table.details')}
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-surface-card divide-y divide-border-default">
                    {activitiesData.content.map((activity) => {
                      const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`;
                      const isExpanded = expandedItems.has(activityKey);
                      const hasChanges =
                        activity.action === 'UPDATE' &&
                        activity.changedFields &&
                        Object.keys(activity.changedFields).length > 0;

                      return (
                        <>
                          <tr
                            key={activityKey}
                            className="hover:bg-primary-50 cursor-pointer"
                          >
                            <td
                              className="px-6 py-4 whitespace-nowrap text-sm text-text-primary"
                              onClick={() => handleRowClick(activity)}
                            >
                              <div>{formatDate(activity.timestamp)}</div>
                              <div className="flex items-center gap-2">
                                <div className="text-xs text-text-secondary">
                                  {format(
                                    new Date(activity.timestamp),
                                    'HH:mm:ss'
                                  )}
                                </div>
                                {activity.impersonatedBy && (
                                  <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-warning-bg text-warning-text">
                                    <Eye className="h-3 w-3" />
                                    {t('common:auditLog.table.impersonated')}
                                  </span>
                                )}
                              </div>
                            </td>
                            <td
                              className="px-6 py-4 whitespace-nowrap"
                              onClick={() => handleRowClick(activity)}
                            >
                              <span
                                className={`px-2 py-1 text-xs font-semibold rounded ${getEntityTypeColor(activity.entityType)}`}
                              >
                                {t(
                                  `common:auditLog.entityTypes.${activity.entityType.toLowerCase()}`,
                                  { defaultValue: activity.entityType }
                                )}
                              </span>
                            </td>
                            <td
                              className="px-6 py-4 whitespace-nowrap"
                              onClick={() => handleRowClick(activity)}
                            >
                              <span
                                className={`px-2 py-1 text-xs font-semibold rounded ${getActionColor(activity.action)}`}
                              >
                                {t(
                                  `common:auditLog.actions.${activity.action.toLowerCase()}d`,
                                  { defaultValue: activity.action }
                                )}
                              </span>
                            </td>
                            <td
                              className="px-6 py-4 text-sm text-text-primary"
                              onClick={() => handleRowClick(activity)}
                            >
                              {activity.description}
                            </td>
                            <td
                              className="px-6 py-4 whitespace-nowrap text-sm text-text-primary"
                              onClick={() => handleRowClick(activity)}
                            >
                              {activity.userName}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
                              {hasChanges && (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    toggleExpanded(activityKey);
                                  }}
                                  className="text-primary-500 hover:text-primary-600 flex items-center gap-1"
                                >
                                  {isExpanded ? (
                                    <>
                                      <ChevronUp className="h-4 w-4" />
                                      {t('common:auditLog.table.hide')}
                                    </>
                                  ) : (
                                    <>
                                      <ChevronDown className="h-4 w-4" />
                                      {t('common:auditLog.table.show')}
                                    </>
                                  )}
                                </button>
                              )}
                            </td>
                          </tr>
                          {isExpanded && hasChanges && (
                            <tr key={`${activityKey}-details`}>
                              <td
                                colSpan={6}
                                className="px-6 py-4 bg-surface-page"
                              >
                                <div className="space-y-2">
                                  <h4 className="text-xs font-semibold text-text-secondary uppercase mb-2">
                                    {t('common:auditLog.changedFields')}
                                  </h4>
                                  {Object.entries(
                                    activity.changedFields ?? {}
                                  ).map(([field, value]) => {
                                    // Skip internal fields
                                    if (field === 'documentCount') {
                                      return null;
                                    }

                                    // Special handling for document operations
                                    if (
                                      field === 'documentAdded' ||
                                      field === 'documentRemoved'
                                    ) {
                                      return (
                                        <div
                                          key={field}
                                          className="bg-surface-card rounded p-3 text-xs"
                                        >
                                          <div className="font-semibold text-text-secondary mb-1">
                                            {t('common:auditLog.fileName')}
                                          </div>
                                          <div className="text-text-primary">
                                            {formatAuditValue(value)}
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
                                        className="bg-surface-card rounded p-3 text-xs"
                                      >
                                        <div className="font-semibold text-text-secondary mb-1 capitalize">
                                          {field.replace(/([A-Z])/g, ' $1')}
                                        </div>
                                        <div className="grid grid-cols-2 gap-4">
                                          <div>
                                            <div className="text-text-secondary mb-1">
                                              {t('common:auditLog.before')}
                                            </div>
                                            <div className="text-text-primary">
                                              {oldValue !== null &&
                                              oldValue !== undefined ? (
                                                typeof oldValue === 'string' &&
                                                /<[a-z][\s\S]*>/i.test(
                                                  oldValue
                                                ) ? (
                                                  <RichTextDisplay
                                                    content={oldValue}
                                                    className="text-xs [&_p]:m-0"
                                                  />
                                                ) : (
                                                  formatAuditValue(oldValue)
                                                )
                                              ) : (
                                                '\u2014'
                                              )}
                                            </div>
                                          </div>
                                          <div>
                                            <div className="text-text-secondary mb-1">
                                              {t('common:auditLog.after')}
                                            </div>
                                            <div className="text-text-primary font-semibold">
                                              {newValue !== null &&
                                              newValue !== undefined ? (
                                                typeof newValue === 'string' &&
                                                /<[a-z][\s\S]*>/i.test(
                                                  newValue
                                                ) ? (
                                                  <RichTextDisplay
                                                    content={newValue}
                                                    className="text-xs [&_p]:m-0"
                                                  />
                                                ) : (
                                                  formatAuditValue(newValue)
                                                )
                                              ) : (
                                                '\u2014'
                                              )}
                                            </div>
                                          </div>
                                        </div>
                                      </div>
                                    );
                                  })}
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
            </div>

            {activitiesData && (
              <Pagination
                page={page}
                totalPages={activitiesData.totalPages}
                totalElements={activitiesData.totalElements}
                size={size}
                onPageChange={handlePageChange}
                onSizeChange={handleSizeChange}
              />
            )}
          </>
        ) : (
          <div className="bg-surface-card rounded-lg border border-border-default p-12 text-center">
            <ClipboardList className="h-12 w-12 text-text-muted mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              {t('common:auditLog.noActivities')}
            </h3>
            <p className="text-text-secondary">
              {entityTypeFilter || actionFilter || searchTerm
                ? t('common:auditLog.adjustFilters')
                : t('common:auditLog.noActivityYet')}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
