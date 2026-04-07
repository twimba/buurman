import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useContractAuditLog } from '@/hooks/useContractHooks';
import { ErrorMessage } from '@/components/ErrorMessage';
import { LoadingSpinner, RichTextDisplay } from '@buurman/ui';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { History, Eye } from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';

interface ContractHistoryTabProps {
  contractId: string;
}

export const ContractHistoryTab = ({ contractId }: ContractHistoryTabProps) => {
  const { t } = useTranslation('contracts');
  const { formatRelative } = useFormatDate();
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );

  const {
    data: auditLog = [],
    isLoading: auditLoading,
    error: auditError,
  } = useContractAuditLog(contractId);

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <h2 className="text-xl font-semibold text-text-primary mb-4">
        {t('history.title')}
      </h2>
      {auditLoading ? (
        <div className="flex items-center justify-center py-8">
          <LoadingSpinner />
        </div>
      ) : auditError ? (
        <ErrorMessage message={t('history.failedToLoad')} />
      ) : auditLog.length > 0 ? (
        <div className="space-y-4">
          {auditLog.map((activity) => {
            const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`;
            const isExpanded = expandedAuditItems.has(activityKey);
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
                  className={`flex items-start gap-4 p-4 transition-colors ${
                    hasChanges ? 'cursor-pointer hover:bg-surface-inset' : ''
                  }`}
                  onClick={() =>
                    hasChanges &&
                    setExpandedAuditItems((prev) => {
                      const newSet = new Set(prev);
                      if (newSet.has(activityKey)) {
                        newSet.delete(activityKey);
                      } else {
                        newSet.add(activityKey);
                      }
                      return newSet;
                    })
                  }
                >
                  <div
                    className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${
                      activity.action === 'CREATE'
                        ? 'bg-success-bg'
                        : activity.action === 'UPDATE'
                          ? 'bg-info-bg'
                          : 'bg-error-bg'
                    }`}
                  >
                    <span
                      className={`text-xs font-semibold ${
                        activity.action === 'CREATE'
                          ? 'text-success-text'
                          : activity.action === 'UPDATE'
                            ? 'text-info-text'
                            : 'text-error-text'
                      }`}
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
                          {t('history.impersonated')}
                        </span>
                      )}
                    </div>
                    {hasChanges && (
                      <p className="text-xs text-primary-500 mt-1">
                        {isExpanded
                          ? t('history.clickToHide')
                          : t('history.clickToView')}
                      </p>
                    )}
                  </div>
                </div>

                {isExpanded && hasChanges && (
                  <div className="bg-surface-page px-4 py-3 border-t border-border-default">
                    <h4 className="text-xs font-semibold text-text-secondary mb-2 uppercase">
                      {t('history.changedFields')}
                    </h4>
                    <div className="space-y-2">
                      {Object.entries(activity.changedFields ?? {})
                        .filter(([field]) => field !== 'updatedAt')
                        .map(([field, value]) => {
                          // Skip internal fields for document operations
                          if (field === 'documentCount') {
                            return null;
                          }

                          // Special handling for document operations
                          if (
                            field === 'documentAdded' ||
                            field === 'documentRemoved'
                          ) {
                            const category = activity.changedFields?.category;
                            const title = activity.changedFields?.title;
                            return (
                              <div
                                key={field}
                                className="bg-surface-card rounded p-2 text-xs"
                              >
                                <div className="font-semibold text-text-secondary mb-1">
                                  {t('history.fileName')}
                                </div>
                                <div className="text-text-primary">
                                  {String(value)}
                                </div>
                                {title ? (
                                  <>
                                    <div className="font-semibold text-text-secondary mb-1 mt-2">
                                      {t('history.titleField')}
                                    </div>
                                    <div className="text-text-primary">
                                      {String(title)}
                                    </div>
                                  </>
                                ) : null}
                                <div className="font-semibold text-text-secondary mb-1 mt-2">
                                  {t('history.typeField')}
                                </div>
                                <div className="text-text-primary">
                                  {category === 'PHOTO'
                                    ? t('history.photo')
                                    : t('history.document')}
                                </div>
                              </div>
                            );
                          }

                          // Skip category and title for document operations (already shown above)
                          if (
                            (field === 'category' || field === 'title') &&
                            (activity.changedFields?.documentAdded ||
                              activity.changedFields?.documentRemoved)
                          ) {
                            return null;
                          }

                          return (
                            <div
                              key={field}
                              className="bg-surface-card rounded p-2 text-xs"
                            >
                              <div className="font-semibold text-text-secondary mb-1">
                                {field
                                  .replace(/([A-Z])/g, ' $1')
                                  .replace(/^./, (str) => str.toUpperCase())
                                  .trim()}
                              </div>
                              <div className="grid grid-cols-2 gap-2">
                                <div>
                                  <span className="text-text-secondary">
                                    {t('history.old')}{' '}
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
                                    {t('history.new')}{' '}
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
                        })}
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
          <p className="text-text-secondary">{t('history.empty')}</p>
          <p className="text-sm text-text-muted mt-1">
            {t('history.emptyDescription')}
          </p>
        </div>
      )}
    </div>
  );
};
