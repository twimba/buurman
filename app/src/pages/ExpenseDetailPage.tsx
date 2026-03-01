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
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { ExpenseForm } from '@/components/expenses/ExpenseForm';
import { DocumentList } from '@/components/properties/DocumentList';
import { Button, PageHeader } from '@/components/ui';
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
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  UpdateExpenseRequest,
  CreateExpenseRequest,
  formatExpenseCategory,
} from '@/types/expense';

export const ExpenseDetailPage = () => {
  const { id = "" } = useParams<{ id: string }>();
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
        <div className="border-b border-[#e2e6f0] mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('details')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'details'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              Details
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'history'
                  ? 'border-b-2 border-[#5c7cfa] text-blue-600'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
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
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
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
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                  Expense Details
                </h2>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <DollarSign className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Amount
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] text-lg">
                        {expense.currency} {expense.amount.toFixed(2)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Calendar className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Expense Date
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(expense.expenseDate)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Package className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Category
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatExpenseCategory(expense.category)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Receipt className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Description
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {expense.description}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Property Info */}
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                  Property Information
                </h2>
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <Home className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180] mt-1" />
                    <div className="flex-1">
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Property
                      </p>
                      <button
                        onClick={() =>
                          navigate(`/properties/${expense.property.identifier}`)
                        }
                        className="font-medium text-[#5c7cfa] hover:underline text-left"
                      >
                        {expense.property.street}, {expense.property.city}
                      </button>
                      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                        #{expense.property.identifier}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Notes */}
              {expense.notes && (
                <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
                  <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                    Notes
                  </h2>
                  <RichTextDisplay content={expense.notes} />
                </div>
              )}

              {/* Metadata */}
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
                <button
                  onClick={() => setIsMetadataExpanded(!isMetadataExpanded)}
                  className="w-full flex items-center justify-between text-left group"
                >
                  <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    Metadata
                  </h2>
                  {isMetadataExpanded ? (
                    <ChevronUp className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8] group-hover:text-[#3d4463] dark:group-hover:text-[#c4c8db]" />
                  ) : (
                    <ChevronDown className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8] group-hover:text-[#3d4463] dark:group-hover:text-[#c4c8db]" />
                  )}
                </button>
                {isMetadataExpanded && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mt-4">
                    <div>
                      <span className="text-[#6b7194] dark:text-[#8b90a8]">
                        Created:
                      </span>{' '}
                      <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                        {formatDate(expense.createdAt)} at{' '}
                        {new Date(expense.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <div>
                      <span className="text-[#6b7194] dark:text-[#8b90a8]">
                        Last Updated:
                      </span>{' '}
                      <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
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
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
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
                      className="border border-[#e2e6f0] rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors ${
                          hasChanges
                            ? 'cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]'
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
                              ? 'bg-green-100 dark:bg-green-900/30'
                              : activity.action === 'UPDATE'
                                ? 'bg-blue-100 dark:bg-blue-900/30'
                                : 'bg-red-100 dark:bg-red-900/30'
                          }`}
                        >
                          <span
                            className={`text-xs font-semibold ${
                              activity.action === 'CREATE'
                                ? 'text-green-700'
                                : activity.action === 'UPDATE'
                                  ? 'text-blue-700'
                                  : 'text-red-700'
                            }`}
                          >
                            {activity.action.charAt(0)}
                          </span>
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                            {activity.description}
                          </p>
                          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                            {formatDistanceToNow(new Date(activity.timestamp), {
                              addSuffix: true,
                            })}
                          </p>
                          {hasChanges && (
                            <p className="text-xs text-[#5c7cfa] mt-1">
                              {isExpanded
                                ? 'Click to hide changes'
                                : 'Click to view changes'}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-[#f8f9fc] dark:bg-[#0c0d14] px-4 py-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                          <h4 className="text-xs font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields ?? {}).map(
                              ([field]) => (
                                <div
                                  key={field}
                                  className="bg-white dark:bg-[#14161f] dark:text-[#eef0f6] rounded p-2 text-xs"
                                >
                                  <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1">
                                    {field
                                      .replace(/([A-Z])/g, ' $1')
                                      .replace(/^./, (str) => str.toUpperCase())
                                      .trim()}
                                  </div>
                                  <div className="grid grid-cols-2 gap-2">
                                    <div>
                                      <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                        Old:{' '}
                                      </span>
                                      {typeof activity.oldValues?.[field] ===
                                        'string' &&
                                      /<[a-z][\s\S]*>/i.test(
                                        activity.oldValues[field]
                                      ) ? (
                                        <RichTextDisplay
                                          content={activity.oldValues[field]}
                                          className="text-xs text-red-600 line-through [&_p]:m-0 inline"
                                        />
                                      ) : (
                                        <span className="text-red-600 line-through">
                                          {String(
                                            activity.oldValues?.[field] ?? 'N/A'
                                          )}
                                        </span>
                                      )}
                                    </div>
                                    <div>
                                      <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                        New:{' '}
                                      </span>
                                      {typeof activity.newValues?.[field] ===
                                        'string' &&
                                      /<[a-z][\s\S]*>/i.test(
                                        activity.newValues[field]
                                      ) ? (
                                        <RichTextDisplay
                                          content={activity.newValues[field]}
                                          className="text-xs text-green-600 font-medium [&_p]:m-0 inline"
                                        />
                                      ) : (
                                        <span className="text-green-600 font-medium">
                                          {String(
                                            activity.newValues?.[field] ?? 'N/A'
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
                <History className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8]">
                  No history available
                </p>
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] mt-1">
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
          <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
              Delete Expense
            </h2>
            <p className="text-[#3d4463] dark:text-[#c4c8db] mb-6">
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
