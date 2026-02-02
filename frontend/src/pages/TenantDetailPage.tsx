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
import {
  ArrowLeft,
  Edit,
  Trash2,
  Mail,
  Phone,
  User,
  Home,
  X,
  FileText,
  Image,
  ChevronDown,
  MapPin,
  Plus,
} from 'lucide-react';
import { formatDistanceToNow, format } from 'date-fns';

export const TenantDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
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

  const toggleAuditItem = (itemId: string) => {
    const newExpanded = new Set(expandedAuditItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedAuditItems(newExpanded);
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
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/tenants')}
              className="p-2 hover:bg-gray-200 rounded transition-colors"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <Avatar
              firstName={tenant.firstName}
              lastName={tenant.lastName}
              photoUrl={tenant.mainPhotoUrl}
              size="xl"
            />
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                {tenant.firstName} {tenant.lastName}
              </h1>
              <p className="text-sm text-gray-500">#{tenant.identifier}</p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => navigate(`/tenants/${id}/edit`)}
              className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
            >
              <Edit className="h-4 w-4" />
              Edit
            </button>
            <button
              onClick={() => setShowDeleteModal(true)}
              className="flex items-center gap-2 px-4 py-2 border border-red-300 text-red-700 rounded hover:bg-red-50 transition-colors"
            >
              <Trash2 className="h-4 w-4" />
              Delete
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('info')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'info'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Information
            </button>
            <button
              onClick={() => setActiveTab('photos')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'photos'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <Image className="h-4 w-4" />
              Photos {photos.length > 0 && `(${photos.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('addresses')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'addresses'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <MapPin className="h-4 w-4" />
              Addresses
            </button>
            <button
              onClick={() => setActiveTab('contracts')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'contracts'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <FileText className="h-4 w-4" />
              Contracts {contracts.length > 0 && `(${contracts.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
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
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Contact Information
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Name</p>
                    <p className="font-medium text-gray-900">
                      {tenant.firstName} {tenant.lastName}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Email</p>
                    <p className="font-medium text-gray-900">{tenant.email}</p>
                  </div>
                </div>
                {tenant.phone && (
                  <div className="flex items-center gap-3">
                    <Phone className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Phone</p>
                      <p className="font-medium text-gray-900">
                        {tenant.phone}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.taxNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Tax Number</p>
                      <p className="font-medium text-gray-900">
                        {tenant.taxNumber}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.idNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">
                        Government ID Number
                      </p>
                      <p className="font-medium text-gray-900">
                        {tenant.idNumber}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Information */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Additional Information
              </h2>
              {tenant.additionalInfo ? (
                <RichTextDisplay content={tenant.additionalInfo} />
              ) : (
                <p className="text-sm text-gray-400 italic">
                  No additional information available
                </p>
              )}
            </div>

            {/* Current Properties (from Active Contracts) */}
            <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Current Properties
              </h2>
              {contractsLoading ? (
                <LoadingSpinner />
              ) : activeContractProperties.length > 0 ? (
                <div className="space-y-3">
                  {activeContractProperties.map((property) => (
                    <div
                      key={property.id}
                      className="flex items-center justify-between bg-green-50 p-4 rounded border border-green-200"
                    >
                      <div className="flex items-center gap-3">
                        <Home className="h-8 w-8 text-green-600" />
                        <div>
                          <button
                            onClick={() =>
                              navigate(`/properties/${property.id}`)
                            }
                            className="font-medium text-gray-900 hover:text-blue-600 text-left"
                          >
                            {property.street}
                          </button>
                          <p className="text-sm text-gray-600">
                            {property.city}, {property.postalCode}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-400 italic">
                  No active contracts for this tenant
                </p>
              )}
            </div>
          </div>
        )}

        {activeTab === 'photos' && (
          <div className="bg-white rounded-lg shadow p-6">
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
          />
        )}

        {activeTab === 'addresses' && (
          <div className="bg-white rounded-lg shadow p-6">
            <TenantAddressList tenantId={id!} />
          </div>
        )}

        {activeTab === 'contracts' && (
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Contracts</h2>
              <button
                onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2 text-sm"
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
                  className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors inline-flex items-center gap-2"
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
          <div className="bg-white rounded-lg shadow">
            <div className="p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                History
              </h2>
              {auditLoading ? (
                <LoadingSpinner />
              ) : auditError ? (
                <ErrorMessage message="Failed to load audit log" />
              ) : auditLog.length > 0 ? (
                <div className="space-y-3">
                  {auditLog.map((activity) => {
                    const isExpanded = expandedAuditItems.has(activity.id);
                    const hasChanges =
                      activity.changedFields &&
                      Object.keys(activity.changedFields).length > 0;

                    return (
                      <div
                        key={activity.id}
                        className="border border-gray-200 rounded-lg"
                      >
                        <div
                          className={`p-4 ${
                            hasChanges ? 'cursor-pointer hover:bg-gray-50' : ''
                          }`}
                          onClick={() =>
                            hasChanges && toggleAuditItem(activity.id)
                          }
                        >
                          <div className="flex items-start justify-between">
                            <div className="flex-1">
                              <div className="flex items-center gap-2">
                                <span className="font-medium text-gray-900">
                                  {activity.action}
                                </span>
                                <span className="text-gray-500">·</span>
                                <span className="text-sm text-gray-600">
                                  {activity.entityName}
                                </span>
                              </div>
                              <p className="text-sm text-gray-600 mt-1">
                                {activity.description}
                              </p>
                              <div className="flex items-center gap-2 mt-2 text-xs text-gray-500">
                                <span>{activity.userName}</span>
                                <span>·</span>
                                <span
                                  title={format(
                                    new Date(activity.timestamp),
                                    "PPpp 'UTC'"
                                  )}
                                  className="cursor-help"
                                >
                                  {formatDistanceToNow(
                                    new Date(activity.timestamp),
                                    {
                                      addSuffix: true,
                                    }
                                  )}
                                </span>
                              </div>
                            </div>
                            {hasChanges && (
                              <ChevronDown
                                className={`h-5 w-5 text-gray-400 transition-transform ${
                                  isExpanded ? 'rotate-180' : ''
                                }`}
                              />
                            )}
                          </div>
                        </div>
                        {isExpanded && hasChanges && (
                          <div className="border-t border-gray-200 p-4 bg-gray-50">
                            <h4 className="text-sm font-medium text-gray-900 mb-2">
                              Changes
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
                                      <div key={field} className="text-sm">
                                        <div className="font-medium text-gray-700 mb-1">
                                          File Name
                                        </div>
                                        <div className="ml-4 text-gray-900">
                                          {String(value)}
                                        </div>
                                        {title ? (
                                          <>
                                            <div className="font-medium text-gray-700 mb-1 mt-2">
                                              Title
                                            </div>
                                            <div className="ml-4 text-gray-900">
                                              {String(title)}
                                            </div>
                                          </>
                                        ) : null}
                                        <div className="font-medium text-gray-700 mb-1 mt-2">
                                          Type
                                        </div>
                                        <div className="ml-4 text-gray-900">
                                          {category === 'PHOTO'
                                            ? 'Photo'
                                            : 'Document'}
                                        </div>
                                      </div>
                                    );
                                  }

                                  // Skip category and title for document operations (already shown above)
                                  if (
                                    (field === 'category' ||
                                      field === 'title') &&
                                    (activity.changedFields?.documentAdded ||
                                      activity.changedFields?.documentRemoved)
                                  ) {
                                    return null;
                                  }

                                  const isRichText = field === 'additionalInfo';
                                  const oldValue = activity.oldValues?.[field];
                                  const newValue = activity.newValues?.[field];

                                  return (
                                    <div key={field} className="text-sm">
                                      <span className="font-medium text-gray-700">
                                        {field}:
                                      </span>
                                      <div className="ml-4 mt-1">
                                        {oldValue !== undefined && (
                                          <div className="text-red-600">
                                            <div className="flex gap-1">
                                              <span>-</span>
                                              {isRichText && oldValue ? (
                                                <div className="flex-1">
                                                  <RichTextDisplay
                                                    content={String(oldValue)}
                                                  />
                                                </div>
                                              ) : (
                                                <span>
                                                  {String(oldValue ?? '')}
                                                </span>
                                              )}
                                            </div>
                                          </div>
                                        )}
                                        {newValue !== undefined && (
                                          <div className="text-green-600">
                                            <div className="flex gap-1">
                                              <span>+</span>
                                              {isRichText && newValue ? (
                                                <div className="flex-1">
                                                  <RichTextDisplay
                                                    content={String(newValue)}
                                                  />
                                                </div>
                                              ) : (
                                                <span>
                                                  {String(newValue ?? '')}
                                                </span>
                                              )}
                                            </div>
                                          </div>
                                        )}
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
                <p className="text-sm text-gray-400 italic">
                  No audit log entries yet
                </p>
              )}
            </div>
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
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleDelete}
                  disabled={deleteTenantMutation.isPending}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition-colors disabled:opacity-50"
                >
                  {deleteTenantMutation.isPending ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
