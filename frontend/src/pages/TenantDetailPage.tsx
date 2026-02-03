import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useTenant,
  useDeleteTenant,
  useTenantAuditLog,
  useTenantDocuments,
  useTenantPhotos,
  useUploadTenantDocument,
  useUploadTenantPhoto,
  useSetTenantMainPhoto,
  useDeleteTenantDocument,
} from '@/hooks/useTenantHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { DocumentList } from '@/components/properties/DocumentList';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { Avatar } from '@/components/common/Avatar';
import { TenantAddressList } from '@/components/tenants/TenantAddressList';
import { ContractCard } from '@/components/contracts/ContractCard';
import { ContractStatus } from '@/types/contract';
import { Button, PageHeader } from '@/components/ui';
import { useTeam } from '@/context/TeamContext';
import {
  Edit,
  Trash2,
  Mail,
  Phone,
  User,
  Home,
  X,
  FileText,
  Image,
  MapPin,
  Plus,
  History,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

export const TenantDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [activeTab, setActiveTab] = useState<
    'info' | 'photos' | 'documents' | 'addresses' | 'contracts' | 'history'
  >('info');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );

  const { data: tenant, isLoading, error } = useTenant(id);
  const {
    data: auditLog = [],
    isLoading: auditLoading,
    error: auditError,
  } = useTenantAuditLog(id);
  const {
    data: allDocuments = [],
    isLoading: docsLoading,
    error: docsError,
  } = useTenantDocuments(id);
  const {
    data: photos = [],
    isLoading: photosLoading,
    error: photosError,
  } = useTenantPhotos(id);
  const {
    data: contracts = [],
    isLoading: contractsLoading,
    error: contractsError,
  } = useContracts(id ? { tenantId: id } : undefined);

  // Filter out photos from documents list
  const documents = allDocuments.filter((doc) => doc.category !== 'PHOTO');

  const deleteTenantMutation = useDeleteTenant();
  const uploadDocumentMutation = useUploadTenantDocument(id!);
  const uploadPhotoMutation = useUploadTenantPhoto(id!);
  const setMainPhotoMutation = useSetTenantMainPhoto(id!);
  const deleteDocumentMutation = useDeleteTenantDocument(id!);

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deleteTenantMutation.mutateAsync(id);
      navigate('/tenants');
    } catch (err) {
      console.error('Failed to delete tenant:', err);
    }
  };

  const handleUploadDocument = async (
    file: File,
    title?: string,
    notes?: string
  ) => {
    await uploadDocumentMutation.mutateAsync({ file, title, notes });
  };

  const handleDeleteDocument = async (documentId: string) => {
    await deleteDocumentMutation.mutateAsync(documentId);
  };

  const handleUploadPhoto = async (
    file: File,
    title?: string,
    notes?: string
  ) => {
    await uploadPhotoMutation.mutateAsync({ file, title, notes });
  };

  const handleSetMainPhoto = async (photoId: string) => {
    await setMainPhotoMutation.mutateAsync(photoId);
  };

  // Get unique properties from active contracts
  const activeContractProperties = contracts
    ? contracts
        .filter((contract) => contract.status === ContractStatus.ACTIVE)
        .map((contract) => contract.property)
        .filter(
          (property, index, self) =>
            index === self.findIndex((p) => p.id === property.id)
        )
    : [];

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load tenant" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={`${tenant.firstName} ${tenant.lastName}`}
          subtitle={`#${tenant.identifier}`}
          backTo="/tenants"
          avatar={
            <Avatar
              firstName={tenant.firstName}
              lastName={tenant.lastName}
              photoUrl={tenant.mainPhotoUrl}
              size="xl"
            />
          }
          actions={
            <>
              <Button
                variant="secondary"
                leftIcon={<Edit />}
                onClick={() => navigate(`/tenants/${id}/edit`)}
                disabled={!canEditData}
              >
                Edit
              </Button>
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
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('info')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'info'
                  ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
              }`}
            >
              Information
            </button>
            <button
              onClick={() => setActiveTab('photos')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'photos'
                  ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
              }`}
            >
              <Image className="h-4 w-4" />
              Photos {photos.length > 0 && `(${photos.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('addresses')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'addresses'
                  ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
              }`}
            >
              <MapPin className="h-4 w-4" />
              Addresses
            </button>
            <button
              onClick={() => setActiveTab('contracts')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'contracts'
                  ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
              }`}
            >
              <FileText className="h-4 w-4" />
              Contracts {contracts.length > 0 && `(${contracts.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-300'
              }`}
            >
              History {auditLog.length > 0 && `(${auditLog.length})`}
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'info' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Contact Information */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
                Contact Information
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      Name
                    </p>
                    <p className="font-medium text-gray-900 dark:text-gray-100">
                      {tenant.firstName} {tenant.lastName}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      Email
                    </p>
                    <p className="font-medium text-gray-900 dark:text-gray-100">
                      {tenant.email}
                    </p>
                  </div>
                </div>
                {tenant.phone && (
                  <div className="flex items-center gap-3">
                    <Phone className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        Phone
                      </p>
                      <p className="font-medium text-gray-900 dark:text-gray-100">
                        {tenant.phone}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.taxNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        Tax Number
                      </p>
                      <p className="font-medium text-gray-900 dark:text-gray-100">
                        {tenant.taxNumber}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.idNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        Government ID Number
                      </p>
                      <p className="font-medium text-gray-900 dark:text-gray-100">
                        {tenant.idNumber}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Information */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
                Additional Information
              </h2>
              {tenant.additionalInfo ? (
                <RichTextDisplay content={tenant.additionalInfo} />
              ) : (
                <p className="text-sm text-gray-400 dark:text-gray-500 italic">
                  No additional information available
                </p>
              )}
            </div>

            {/* Current Properties (from Active Contracts) */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
                Current Properties
              </h2>
              {contractsLoading ? (
                <LoadingSpinner />
              ) : activeContractProperties.length > 0 ? (
                <div className="space-y-3">
                  {activeContractProperties.map((property) => (
                    <div
                      key={property.id}
                      className="flex items-center justify-between bg-green-50 dark:bg-green-900/30 p-4 rounded border border-green-200 dark:border-green-900/50"
                    >
                      <div className="flex items-center gap-3">
                        <Home className="h-8 w-8 text-green-600" />
                        <div>
                          <button
                            onClick={() =>
                              navigate(`/properties/${property.id}`)
                            }
                            className="font-medium text-gray-900 dark:text-gray-100 hover:text-blue-600 text-left"
                          >
                            {property.street}
                          </button>
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {property.city}, {property.postalCode}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-400 dark:text-gray-500 italic">
                  No active contracts for this tenant
                </p>
              )}
            </div>
          </div>
        )}

        {activeTab === 'photos' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <PhotoGallery
              propertyId={id!}
              photos={photos}
              isLoading={photosLoading}
              error={photosError}
              onUpload={handleUploadPhoto}
              onSetMain={handleSetMainPhoto}
              onDelete={handleDeleteDocument}
              isUploading={uploadPhotoMutation.isPending}
              isDeleting={deleteDocumentMutation.isPending}
              readOnly={!canEditData}
            />
          </div>
        )}

        {activeTab === 'documents' && (
          <DocumentList
            propertyId={id!}
            documents={documents}
            isLoading={docsLoading}
            error={docsError}
            onUpload={handleUploadDocument}
            onDelete={handleDeleteDocument}
            isUploading={uploadDocumentMutation.isPending}
            isDeleting={deleteDocumentMutation.isPending}
            readOnly={!canEditData}
          />
        )}

        {activeTab === 'addresses' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <TenantAddressList tenantId={id!} />
          </div>
        )}

        {activeTab === 'contracts' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Contracts</h2>
              <button
                onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                disabled={!canEditData}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-blue-600"
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
                <FileText className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-600 mb-4">
                  No contracts for this tenant
                </p>
                <button
                  onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                  disabled={!canEditData}
                  className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-blue-600"
                >
                  <Plus className="h-4 w-4" />
                  Create First Contract
                </button>
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-2">
                {contracts.map((contract) => (
                  <ContractCard key={contract.id} contract={contract} />
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Tenant History
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
                  const isExpanded = expandedAuditItems.has(activity.id);
                  const hasChanges =
                    activity.action === 'UPDATE' &&
                    activity.changedFields &&
                    Object.keys(activity.changedFields).length > 0;

                  return (
                    <div
                      key={activity.id}
                      className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors ${
                          hasChanges
                            ? 'cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700'
                            : ''
                        }`}
                        onClick={() =>
                          hasChanges &&
                          setExpandedAuditItems((prev) => {
                            const newSet = new Set(prev);
                            if (newSet.has(activity.id)) {
                              newSet.delete(activity.id);
                            } else {
                              newSet.add(activity.id);
                            }
                            return newSet;
                          })
                        }
                      >
                        <div
                          className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center ${
                            activity.action === 'CREATE'
                              ? 'bg-green-100'
                              : activity.action === 'UPDATE'
                                ? 'bg-blue-100'
                                : 'bg-red-100'
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
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {activity.description}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                            {formatDistanceToNow(new Date(activity.timestamp), {
                              addSuffix: true,
                            })}
                          </p>
                          {hasChanges && (
                            <p className="text-xs text-blue-600 mt-1">
                              {isExpanded
                                ? 'Click to hide changes'
                                : 'Click to view changes'}
                            </p>
                          )}
                        </div>
                      </div>

                      {isExpanded && hasChanges && (
                        <div className="bg-gray-50 px-4 py-3 border-t border-gray-200">
                          <h4 className="text-xs font-semibold text-gray-700 mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields!)
                              .filter(([field]) => field !== 'updatedAt')
                              .map(([field, value]) => {
                                // Skip internal fields for document operations
                                if (field === 'documentCount') return null;

                                // Special handling for document operations
                                if (
                                  field === 'documentAdded' ||
                                  field === 'documentRemoved'
                                ) {
                                  const category =
                                    activity.changedFields?.category;
                                  const title = activity.changedFields?.title;
                                  return (
                                    <div
                                      key={field}
                                      className="bg-white rounded p-2 text-xs"
                                    >
                                      <div className="font-semibold text-gray-700 mb-1">
                                        File Name
                                      </div>
                                      <div className="text-gray-900">
                                        {String(value)}
                                      </div>
                                      {title ? (
                                        <>
                                          <div className="font-semibold text-gray-700 mb-1 mt-2">
                                            Title
                                          </div>
                                          <div className="text-gray-900">
                                            {String(title)}
                                          </div>
                                        </>
                                      ) : null}
                                      <div className="font-semibold text-gray-700 mb-1 mt-2">
                                        Type
                                      </div>
                                      <div className="text-gray-900">
                                        {category === 'PHOTO'
                                          ? 'Photo'
                                          : 'Document'}
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
                                    className="bg-white rounded p-2 text-xs"
                                  >
                                    <div className="font-semibold text-gray-700 mb-1">
                                      {field
                                        .replace(/([A-Z])/g, ' $1')
                                        .replace(/^./, (str) =>
                                          str.toUpperCase()
                                        )
                                        .trim()}
                                    </div>
                                    <div className="grid grid-cols-2 gap-2">
                                      <div>
                                        <span className="text-gray-500">
                                          Old:{' '}
                                        </span>
                                        <span className="text-red-600 line-through">
                                          {String(
                                            activity.oldValues?.[field] ?? 'N/A'
                                          )}
                                        </span>
                                      </div>
                                      <div>
                                        <span className="text-gray-500">
                                          New:{' '}
                                        </span>
                                        <span className="text-green-600 font-medium">
                                          {String(
                                            activity.newValues?.[field] ?? 'N/A'
                                          )}
                                        </span>
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
                <History className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500">No history available</p>
                <p className="text-sm text-gray-400 mt-1">
                  Changes to this tenant will appear here
                </p>
              </div>
            )}
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900">
                  Delete Tenant
                </h3>
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <p className="text-gray-600 mb-6">
                Are you sure you want to delete this tenant? This action cannot
                be undone.
              </p>
              <div className="flex gap-3 justify-end">
                <Button
                  variant="secondary"
                  onClick={() => setShowDeleteModal(false)}
                >
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  leftIcon={<Trash2 />}
                  onClick={handleDelete}
                  isLoading={deleteTenantMutation.isPending}
                >
                  Delete
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
