import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { usePropertyAuditLog } from '@/hooks/usePropertyHooks';
import { ErrorMessage } from '@/components/ErrorMessage';
import { LoadingSpinner, RichTextDisplay } from '@buurman/ui';
import { useFormatDate } from '@/hooks/useFormatDate';
import { History, Eye } from 'lucide-react';

interface PropertyAuditTabProps {
  propertyId: string;
}

const formatFieldName = (field: string): string => {
  if (field === 'documentAdded') {
    return 'Document Added';
  }
  if (field === 'documentRemoved') {
    return 'Document Removed';
  }
  if (field === 'documentCount') {
    return 'Document Count';
  }
  if (field === 'photoAdded') {
    return 'Photo Added';
  }
  if (field === 'photoRemoved') {
    return 'Photo Removed';
  }
  if (field === 'photoCount') {
    return 'Photo Count';
  }
  if (field === 'photoEdited') {
    return 'Photo Edited';
  }
  if (field === 'documentEdited') {
    return 'Document Edited';
  }

  return field
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (str) => str.toUpperCase())
    .trim();
};

export const PropertyAuditTab = ({ propertyId }: PropertyAuditTabProps) => {
  const { t } = useTranslation('properties');
  const { formatRelative } = useFormatDate();
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());

  const {
    data: auditLog = [],
    isLoading,
    error,
  } = usePropertyAuditLog(propertyId);

  const toggleItem = (itemId: string) => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedItems(newExpanded);
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <h2 className="text-xl font-semibold text-text-primary mb-4">
        {t('audit.title')}
      </h2>

      {isLoading ? (
        <div className="flex items-center justify-center py-8">
          <LoadingSpinner />
        </div>
      ) : error ? (
        <ErrorMessage message={t('audit.failedToLoad')} />
      ) : auditLog.length > 0 ? (
        <div className="space-y-4">
          {auditLog.map((activity) => {
            const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`;
            const isExpanded = expandedItems.has(activityKey);
            const hasChanges =
              activity.action === 'UPDATE' &&
              activity.changedFields &&
              Object.keys(activity.changedFields).length > 0;

            return (
              <div
                key={activityKey}
                className="border border-border-default rounded-lg overflow-hidden"
              >
                <div
                  className={`flex items-start gap-4 p-4 transition-colors cursor-pointer ${
                    hasChanges ? 'hover:bg-surface-inset' : ''
                  }`}
                  onClick={() => hasChanges && toggleItem(activityKey)}
                >
                  <div
                    className={`
                      flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center
                      ${
                        activity.action === 'CREATE'
                          ? 'bg-success-bg'
                          : activity.action === 'UPDATE'
                            ? 'bg-primary-100 dark:bg-primary-500/10'
                            : 'bg-error-bg'
                      }
                    `}
                  >
                    <span
                      className={`
                        text-xs font-semibold
                        ${
                          activity.action === 'CREATE'
                            ? 'text-success-text'
                            : activity.action === 'UPDATE'
                              ? 'text-info-text'
                              : 'text-error-text'
                        }
                      `}
                    >
                      {activity.action.charAt(0)}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-text-primary">
                      {activity.description}
                    </p>
                    <div className="flex items-center gap-2 mt-1">
                      <p className="text-xs text-text-secondary">
                        {formatRelative(activity.timestamp)}
                      </p>
                      {activity.impersonatedBy && (
                        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-warning-bg text-warning-text">
                          <Eye className="h-3 w-3" />
                          {t('audit.impersonated')}
                        </span>
                      )}
                    </div>
                    {hasChanges && (
                      <p className="text-xs text-primary-500 mt-1">
                        {isExpanded
                          ? t('audit.clickToHide')
                          : t('audit.clickToView')}
                      </p>
                    )}
                  </div>
                </div>

                {/* Expanded Details */}
                {isExpanded && hasChanges && (
                  <div className="bg-surface-page px-4 py-3 border-t border-border-default">
                    <h4 className="text-xs font-semibold text-text-secondary mb-2 uppercase">
                      {t('audit.changedFields')}
                    </h4>
                    <div className="space-y-2">
                      {Object.entries(activity.changedFields ?? {}).map(
                        ([field, value]) => {
                          // Skip internal count fields
                          if (
                            field === 'documentCount' ||
                            field === 'photoCount'
                          ) {
                            return null;
                          }

                          // Skip marker fields for edit operations (fileName is context only)
                          if (
                            field === 'photoEdited' ||
                            field === 'documentEdited'
                          ) {
                            return null;
                          }
                          if (
                            field === 'fileName' &&
                            (activity.changedFields?.photoEdited ||
                              activity.changedFields?.documentEdited)
                          ) {
                            return null;
                          }

                          // Special handling for document/photo upload/delete operations
                          if (
                            field === 'documentAdded' ||
                            field === 'documentRemoved' ||
                            field === 'photoAdded' ||
                            field === 'photoRemoved'
                          ) {
                            const category = activity.changedFields?.category;
                            const title = activity.changedFields?.title;
                            return (
                              <div
                                key={field}
                                className="bg-surface-card rounded p-2 text-xs"
                              >
                                <div className="font-semibold text-text-secondary mb-1">
                                  File Name
                                </div>
                                <div className="text-text-primary">
                                  {String(value)}
                                </div>
                                {title ? (
                                  <>
                                    <div className="font-semibold text-text-secondary mb-1 mt-2">
                                      Title
                                    </div>
                                    <div className="text-text-primary">
                                      {String(title)}
                                    </div>
                                  </>
                                ) : null}
                                <div className="font-semibold text-text-secondary mb-1 mt-2">
                                  Type
                                </div>
                                <div className="text-text-primary">
                                  {category === 'PHOTO' ? 'Photo' : 'Document'}
                                </div>
                              </div>
                            );
                          }

                          // Skip category and title for upload/delete operations (already shown above)
                          if (
                            (field === 'category' || field === 'title') &&
                            (activity.changedFields?.documentAdded ||
                              activity.changedFields?.documentRemoved ||
                              activity.changedFields?.photoAdded ||
                              activity.changedFields?.photoRemoved)
                          ) {
                            return null;
                          }

                          return (
                            <div
                              key={field}
                              className="bg-surface-card rounded p-2 text-xs"
                            >
                              <div className="font-semibold text-text-secondary mb-1">
                                {formatFieldName(field)}
                              </div>
                              <div className="grid grid-cols-2 gap-2">
                                <div>
                                  <span className="text-text-secondary">
                                    Old:{' '}
                                  </span>
                                  {typeof activity.oldValues?.[field] ===
                                    'string' &&
                                  /<[a-z][\s\S]*>/i.test(
                                    activity.oldValues[field]
                                  ) ? (
                                    <RichTextDisplay
                                      content={activity.oldValues[field]}
                                      className="text-xs text-error-text line-through [&_p]:m-0 inline"
                                    />
                                  ) : (
                                    <span className="text-error-text line-through">
                                      {formatAuditValue(
                                        activity.oldValues?.[field]
                                      )}
                                    </span>
                                  )}
                                </div>
                                <div>
                                  <span className="text-text-secondary">
                                    New:{' '}
                                  </span>
                                  {typeof activity.newValues?.[field] ===
                                    'string' &&
                                  /<[a-z][\s\S]*>/i.test(
                                    activity.newValues[field]
                                  ) ? (
                                    <RichTextDisplay
                                      content={activity.newValues[field]}
                                      className="text-xs text-success-text font-medium [&_p]:m-0 inline"
                                    />
                                  ) : (
                                    <span className="text-success-text font-medium">
                                      {formatAuditValue(
                                        activity.newValues?.[field]
                                      )}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          );
                        }
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-8">
          <History className="h-12 w-12 text-text-disabled mx-auto mb-3" />
          <p className="text-text-secondary">{t('audit.empty.title')}</p>
          <p className="text-sm text-text-muted mt-1">
            {t('audit.empty.description')}
          </p>
        </div>
      )}
    </div>
  );
};
