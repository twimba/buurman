import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTabState } from '@/hooks/useTabState';
import {
  useExpense,
  useDeleteExpense,
  useUpdateExpense,
  useExpenseAuditLog,
  useExpenseDocuments,
  useUploadExpenseDocument,
  useDeleteExpenseDocument,
} from '@/hooks/useExpenseHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { ExpenseForm } from '@/components/expenses/ExpenseForm';
import { DocumentList } from '@/components/properties/DocumentList';
import { Button, PageHeader } from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import {
  Edit,
  Trash2,
  ChevronUp,
  ChevronDown,
  Receipt,
  Calendar,
  DollarSign,
  Home,
  FileText,
  Package,
  History,
  Eye,
  User,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  UpdateExpenseRequest,
  CreateExpenseRequest,
  formatExpenseCategory,
} from '@/types/expense';

export const ExpenseDetailPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [activeTab, setActiveTab] = useTabState('details', [
    'details',
    'documents',
    'history',
  ] as const);
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);

  const { data: expense, isLoading, error } = useExpense(id);
  const {
    data: auditLog = [],
    isLoading: auditLoading,
    error: auditError,
  } = useExpenseAuditLog(id);
  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = useExpenseDocuments(id);
  const deleteExpenseMutation = useDeleteExpense();
  const updateExpenseMutation = useUpdateExpense(id);
  const uploadDocumentMutation = useUploadExpenseDocument(id);
  const deleteDocumentMutation = useDeleteExpenseDocument(id);

  const handleDelete = async () => {
    if (!id) {
      return;
    }
    try {
      await deleteExpenseMutation.mutateAsync(id);
      navigate('/expenses');
    } catch (err) {
      console.error('Failed to delete expense:', err);
    }
  };

  const handleUpdate = async (data: CreateExpenseRequest) => {
    const updateData: UpdateExpenseRequest = {
      contactIdentifier: data.contactIdentifier,
      category: data.category,
      amount: data.amount,
      currency: data.currency,
      expenseDate: data.expenseDate,
      description: data.description,
      notes: data.notes,
    };
    await updateExpenseMutation.mutateAsync(updateData);
    setIsEditing(false);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !expense) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load expense" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={`Expense #${expense.identifier}`}
          subtitle={expense.description}
          backTo="/expenses"
          badge={<ExpenseCategoryBadge category={expense.category} />}
          actions={
            <>
              {!isEditing && (
                <Button
                  variant="secondary"
                  leftIcon={<Edit />}
                  onClick={() => {
                    setActiveTab('details');
                    setIsEditing(true);
                  }}
                  disabled={!canEditData}
                >
                  Edit
                </Button>
              )}
              <Button
                variant="danger"
                leftIcon={<Trash2 />}
                onClick={() => setShowDeleteModal(true)}
                disabled={!canEditData}
              >
                Delete
              </Button>
            </>
          }
        />

        {/* Tabs */}
        <div className="border-b border-border-default mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('details')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'details'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              Details
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'history'
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <History className="h-4 w-4" />
              History {auditLog.length > 0 && `(${auditLog.length})`}
            </button>
          </div>
        </div>

        {/* Content */}
        {activeTab === 'details' &&
          (isEditing ? (
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Edit Expense
              </h2>
              <ExpenseForm
                expense={expense}
                onSubmit={handleUpdate}
                onCancel={() => setIsEditing(false)}
                isLoading={updateExpenseMutation.isPending}
              />
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Expense Details */}
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Expense Details
                </h2>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">Amount</p>
                      <p className="font-medium text-text-primary text-lg">
                        {expense.currency} {expense.amount.toFixed(2)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">
                        Expense Date
                      </p>
                      <p className="font-medium text-text-primary">
                        {formatDate(expense.expenseDate)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Package className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">Category</p>
                      <p className="font-medium text-text-primary">
                        {formatExpenseCategory(expense.category)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Receipt className="h-5 w-5 text-text-muted mt-1" />
                    <div>
                      <p className="text-sm text-text-secondary">Description</p>
                      <p className="font-medium text-text-primary">
                        {expense.description}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Property & Contact Info */}
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Property & Contact
                </h2>
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <Home className="h-5 w-5 text-text-muted mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-text-secondary">Property</p>
                      <button
                        onClick={() =>
                          navigate(`/properties/${expense.property.identifier}`)
                        }
                        className="font-medium text-primary-500 hover:underline text-left"
                      >
                        {expense.property.street}, {expense.property.city}
                      </button>
                      <p className="text-xs text-text-secondary mt-1">
                        #{expense.property.identifier}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <User className="h-5 w-5 text-text-muted mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-text-secondary">Contact</p>
                      {expense.contact ? (
                        <>
                          <button
                            onClick={() =>
                              navigate(
                                `/contacts/${expense.contact?.identifier}`
                              )
                            }
                            className="font-medium text-primary-500 hover:underline text-left"
                          >
                            {expense.contact.firstName}{' '}
                            {expense.contact.lastName}
                          </button>
                          <p className="text-xs text-text-secondary mt-1">
                            #{expense.contact.identifier}
                          </p>
                        </>
                      ) : (
                        <p className="text-sm text-text-muted">
                          No contact linked
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Notes */}
              {expense.notes && (
                <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                  <h2 className="text-lg font-semibold text-text-primary mb-4">
                    Notes
                  </h2>
                  <RichTextDisplay content={expense.notes} />
                </div>
              )}

              {/* Metadata */}
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
                <button
                  onClick={() => setIsMetadataExpanded(!isMetadataExpanded)}
                  className="w-full flex items-center justify-between text-left group"
                >
                  <h2 className="text-lg font-semibold text-text-primary">
                    Metadata
                  </h2>
                  {isMetadataExpanded ? (
                    <ChevronUp className="h-5 w-5 text-text-secondary group-hover:text-text-secondary " />
                  ) : (
                    <ChevronDown className="h-5 w-5 text-text-secondary group-hover:text-text-secondary " />
                  )}
                </button>
                {isMetadataExpanded && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mt-4">
                    <div>
                      <span className="text-text-secondary">Created:</span>{' '}
                      <span className="text-text-primary">
                        {formatDate(expense.createdAt)} at{' '}
                        {new Date(expense.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <div>
                      <span className="text-text-secondary">Last Updated:</span>{' '}
                      <span className="text-text-primary">
                        {formatDate(expense.updatedAt)} at{' '}
                        {new Date(expense.updatedAt).toLocaleTimeString()}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))}

        {activeTab === 'documents' && (
          <DocumentList
            documents={documents}
            isLoading={docsLoading}
            error={docsError}
            onUpload={async (file, title, notes) => {
              await uploadDocumentMutation.mutateAsync({ file, title, notes });
            }}
            onDelete={async (documentId) => {
              await deleteDocumentMutation.mutateAsync(documentId);
            }}
            isUploading={uploadDocumentMutation.isPending}
            isDeleting={deleteDocumentMutation.isPending}
            readOnly={!canEditData}
          />
        )}

        {activeTab === 'history' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              Expense History
            </h2>
            {auditLoading ? (
              <div className="flex items-center justify-center py-8">
                <LoadingSpinner />
              </div>
            ) : auditError ? (
              <ErrorMessage message="Failed to load history" />
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
                          hasChanges
                            ? 'cursor-pointer hover:bg-surface-inset'
                            : ''
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
                              {formatDistanceToNow(
                                new Date(activity.timestamp),
                                {
                                  addSuffix: true,
                                }
                              )}
                            </p>
                            {activity.impersonatedBy && (
                              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-warning-bg text-warning-text">
                                <Eye className="h-3 w-3" />
                                Impersonated
                              </span>
                            )}
                          </div>
                          {hasChanges && (
                            <p className="text-xs text-primary-500 mt-1">
                              {isExpanded
                                ? 'Click to hide changes'
                                : 'Click to view changes'}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-surface-page px-4 py-3 border-t border-border-default">
                          <h4 className="text-xs font-semibold text-text-secondary mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields ?? {}).map(
                              ([field]) => (
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
                              )
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
                <p className="text-text-secondary">No history available</p>
                <p className="text-sm text-text-muted mt-1">
                  Changes to this expense will appear here
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-text-primary mb-4">
              Delete Expense
            </h2>
            <p className="text-text-secondary mb-6">
              Are you sure you want to delete this expense? This action cannot
              be undone.
            </p>
            <div className="flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setShowDeleteModal(false)}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={handleDelete}
                isLoading={deleteExpenseMutation.isPending}
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
