import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useTenant,
  useDeleteTenant,
  useTenantAuditLog,
  useTenantDocuments,
  useTenantPhotos,
  useTenantAddresses,
  useUploadTenantDocument,
  useUploadTenantPhoto,
  useSetTenantMainPhoto,
  useDeleteTenantDocument,
} from '@/hooks/useTenantHooks';
import { useDeletePhoto } from '@/hooks/usePhotoHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { CalendarFeedType } from '@/types/calendarFeed';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { DocumentList } from '@/components/properties/DocumentList';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { Avatar } from '@/components/common/Avatar';
import { TenantAddressList } from '@/components/tenants/TenantAddressList';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
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
  Search,
  ChevronUp,
  ChevronDown,
  Download,
} from 'lucide-react';
import client from '@/api/client';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import { getCurrencySymbol } from '@/utils/currencies';

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
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);

  const { formatDate } = useFormatDate();

  // Contracts table state
  const [contractsSearchTerm, setContractsSearchTerm] = useState('');
  const [contractsSortField, setContractsSortField] = useState<
    'startDate' | 'rentAmount' | 'status' | 'property' | 'contractType'
  >('startDate');
  const [contractsSortOrder, setContractsSortOrder] = useState<'asc' | 'desc'>(
    'desc'
  );
  const [contractsCurrentPage, setContractsCurrentPage] = useState(1);
  const contractsPerPage = 10;

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
    data: contractsData,
    isLoading: contractsLoading,
    error: contractsError,
  } = useContracts(id ? { tenantIdentifier: id } : undefined);
  const contracts = useMemo(
    () => contractsData?.content ?? [],
    [contractsData]
  );
  const { data: addresses = [] } = useTenantAddresses(id);

  const documents = allDocuments;

  const deleteTenantMutation = useDeleteTenant();
  const uploadDocumentMutation = useUploadTenantDocument(id!);
  const uploadPhotoMutation = useUploadTenantPhoto(id!);
  const setMainPhotoMutation = useSetTenantMainPhoto(id!);
  const deleteDocumentMutation = useDeleteTenantDocument(id!);
  const deletePhotoMutation = useDeletePhoto();

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

  const handleDeletePhoto = async (photoId: string) => {
    await deletePhotoMutation.mutateAsync(photoId);
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
            index ===
            self.findIndex((p) => p.identifier === property.identifier)
        )
    : [];

  // Contracts filtering, sorting, and pagination
  const filteredAndSortedContracts = useMemo(() => {
    if (!contracts) return [];
    let filtered = [...contracts];
    if (contractsSearchTerm) {
      const search = contractsSearchTerm.toLowerCase();
      filtered = filtered.filter(
        (contract) =>
          contract.identifier.toLowerCase().includes(search) ||
          contract.property.street.toLowerCase().includes(search) ||
          contract.contractType.toLowerCase().includes(search)
      );
    }
    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;
      switch (contractsSortField) {
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
      if (aVal < bVal) return contractsSortOrder === 'asc' ? -1 : 1;
      if (aVal > bVal) return contractsSortOrder === 'asc' ? 1 : -1;
      return 0;
    });
    return filtered;
  }, [contracts, contractsSearchTerm, contractsSortField, contractsSortOrder]);

  const paginatedContracts = useMemo(() => {
    const startIndex = (contractsCurrentPage - 1) * contractsPerPage;
    return filteredAndSortedContracts.slice(
      startIndex,
      startIndex + contractsPerPage
    );
  }, [filteredAndSortedContracts, contractsCurrentPage]);

  const contractsTotalPages = Math.ceil(
    filteredAndSortedContracts.length / contractsPerPage
  );

  const handleContractsSort = (field: typeof contractsSortField) => {
    if (contractsSortField === field) {
      setContractsSortOrder(contractsSortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setContractsSortField(field);
      setContractsSortOrder('asc');
    }
  };

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
                variant="primary"
                leftIcon={<Download />}
                onClick={async () => {
                  try {
                    const response = await client.get(
                      `/reports/export/tenant/${id}/report`,
                      { responseType: 'blob' }
                    );
                    const blob = new Blob([response.data], {
                      type: 'application/pdf',
                    });
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'tenant-report.pdf';
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                  } catch (error) {
                    console.error('Failed to download report:', error);
                  }
                }}
              >
                Report
              </Button>
              {id && (
                <CalendarFeedButton
                  feedType={CalendarFeedType.TENANT_PAYMENTS}
                  entityIdentifier={id}
                />
              )}
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
        <div className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('info')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'info'
                  ? 'border-b-2 border-[#5c7cfa] text-primary-500 dark:text-primary-300'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              Information
            </button>
            <button
              onClick={() => setActiveTab('photos')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'photos'
                  ? 'border-b-2 border-[#5c7cfa] text-primary-500 dark:text-primary-300'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <Image className="h-4 w-4" />
              Photos {photos.length > 0 && `(${photos.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-[#5c7cfa] text-primary-500 dark:text-primary-300'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('addresses')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'addresses'
                  ? 'border-b-2 border-[#5c7cfa] text-primary-500 dark:text-primary-300'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <MapPin className="h-4 w-4" />
              Addresses {addresses.length > 0 && `(${addresses.length})`}
            </button>
            <button
              onClick={() => setActiveTab('contracts')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'contracts'
                  ? 'border-b-2 border-[#5c7cfa] text-primary-500 dark:text-primary-300'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <FileText className="h-4 w-4" />
              Contracts {contracts.length > 0 && `(${contracts.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-[#5c7cfa] text-primary-500 dark:text-primary-300'
                  : 'text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
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
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Contact Information
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      Name
                    </p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {tenant.firstName} {tenant.lastName}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                  <div>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      Email
                    </p>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {tenant.email}
                    </p>
                  </div>
                </div>
                {tenant.phone && (
                  <div className="flex items-center gap-3">
                    <Phone className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Phone
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {tenant.phone}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.taxNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Tax Number
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {tenant.taxNumber}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.idNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <div>
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Government ID Number
                      </p>
                      <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                        {tenant.idNumber}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Information */}
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Additional Information
              </h2>
              {tenant.additionalInfo ? (
                <RichTextDisplay content={tenant.additionalInfo} />
              ) : (
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] italic">
                  No additional information available
                </p>
              )}
            </div>

            {/* Current Properties (from Active Contracts) */}
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
                Current Properties
              </h2>
              {contractsLoading ? (
                <LoadingSpinner />
              ) : activeContractProperties.length > 0 ? (
                <div className="space-y-3">
                  {activeContractProperties.map((property) => (
                    <div
                      key={property.identifier}
                      className="flex items-center justify-between bg-green-50 dark:bg-green-900/30 p-4 rounded border border-green-200 dark:border-green-900/50"
                    >
                      <div className="flex items-center gap-3">
                        <Home className="h-8 w-8 text-green-600" />
                        <div>
                          <button
                            onClick={() =>
                              navigate(`/properties/${property.identifier}`)
                            }
                            className="font-medium text-[#1a1d2e] dark:text-[#eef0f6] hover:text-[#5c7cfa] text-left"
                          >
                            {property.street}
                          </button>
                          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                            {property.city}, {property.postalCode}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] italic">
                  No active contracts for this tenant
                </p>
              )}
            </div>

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
                      {formatDate(tenant.createdAt)} at{' '}
                      {new Date(tenant.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-[#6b7194] dark:text-[#8b90a8]">
                      Last Updated:
                    </span>{' '}
                    <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                      {formatDate(tenant.updatedAt)} at{' '}
                      {new Date(tenant.updatedAt).toLocaleTimeString()}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'photos' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <PhotoGallery
              propertyId={id!}
              photos={photos}
              isLoading={photosLoading}
              error={photosError}
              onUpload={handleUploadPhoto}
              onSetMain={handleSetMainPhoto}
              onDelete={handleDeletePhoto}
              isUploading={uploadPhotoMutation.isPending}
              isDeleting={deletePhotoMutation.isPending}
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
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <TenantAddressList tenantId={id!} />
          </div>
        )}

        {activeTab === 'contracts' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Contracts ({filteredAndSortedContracts.length})
              </h2>
              <button
                onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                disabled={!canEditData}
                className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
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
                <FileText className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8] mb-4">
                  No contracts for this tenant
                </p>
                <button
                  onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                  disabled={!canEditData}
                  className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
                >
                  <Plus className="h-4 w-4" />
                  Create First Contract
                </button>
              </div>
            ) : (
              <>
                {/* Search Bar */}
                <div className="mb-4">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <input
                      type="text"
                      placeholder="Search by contract #, property, type..."
                      value={contractsSearchTerm}
                      onChange={(e) => {
                        setContractsSearchTerm(e.target.value);
                        setContractsCurrentPage(1);
                      }}
                      className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                    <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                      <tr>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                          onClick={() => handleContractsSort('startDate')}
                        >
                          <div className="flex items-center gap-1">
                            Contract #
                            {contractsSortField === 'startDate' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                          onClick={() => handleContractsSort('property')}
                        >
                          <div className="flex items-center gap-1">
                            Property
                            {contractsSortField === 'property' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                          onClick={() => handleContractsSort('contractType')}
                        >
                          <div className="flex items-center gap-1">
                            Type
                            {contractsSortField === 'contractType' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                          onClick={() => handleContractsSort('startDate')}
                        >
                          <div className="flex items-center gap-1">
                            Start Date
                            {contractsSortField === 'startDate' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                          End Date
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                          onClick={() => handleContractsSort('rentAmount')}
                        >
                          <div className="flex items-center gap-1">
                            Rent Amount
                            {contractsSortField === 'rentAmount' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
                          onClick={() => handleContractsSort('status')}
                        >
                          <div className="flex items-center gap-1">
                            Status
                            {contractsSortField === 'status' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
                      {paginatedContracts.length === 0 ? (
                        <tr>
                          <td
                            colSpan={7}
                            className="px-6 py-12 text-center text-[#6b7194] dark:text-[#8b90a8]"
                          >
                            No contracts found matching your search
                          </td>
                        </tr>
                      ) : (
                        paginatedContracts.map((contract) => (
                          <tr
                            key={contract.identifier}
                            onClick={() =>
                              navigate(`/contracts/${contract.identifier}`)
                            }
                            className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer transition-colors"
                          >
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm font-medium text-primary-500 dark:text-primary-300">
                                #{contract.identifier}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                                {contract.property.street}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                                {contract.contractType.replace('_', ' ')}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                                {formatDate(contract.startDate)}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                                {contract.endDate
                                  ? formatDate(contract.endDate)
                                  : '-'}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                                {getCurrencySymbol(contract.currency)}{' '}
                                {contract.rentAmount.toFixed(2)}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <ContractStatusBadge status={contract.status} />
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {contractsTotalPages > 1 && (
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                    <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      Showing{' '}
                      {(contractsCurrentPage - 1) * contractsPerPage + 1} to{' '}
                      {Math.min(
                        contractsCurrentPage * contractsPerPage,
                        filteredAndSortedContracts.length
                      )}{' '}
                      of {filteredAndSortedContracts.length} contracts
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() =>
                          setContractsCurrentPage(contractsCurrentPage - 1)
                        }
                        disabled={contractsCurrentPage === 1}
                        className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:text-[#c4c8db]"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Page {contractsCurrentPage} of {contractsTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setContractsCurrentPage(contractsCurrentPage + 1)
                        }
                        disabled={contractsCurrentPage === contractsTotalPages}
                        className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:text-[#c4c8db]"
                      >
                        Next
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
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
                  const activityKey = `${activity.entityType}-${activity.entityIdentifier}-${activity.timestamp}`;
                  const isExpanded = expandedAuditItems.has(activityKey);
                  const hasChanges =
                    activity.action === 'UPDATE' &&
                    activity.changedFields &&
                    Object.keys(activity.changedFields).length > 0;

                  return (
                    <div
                      key={activityKey}
                      className="border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg overflow-hidden"
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
                            {Object.entries(activity.changedFields!)
                              .filter(([field]) => field !== 'updatedAt')
                              .map(([field, value]) => {
                                // Skip internal count fields
                                if (
                                  field === 'documentCount' ||
                                  field === 'photoCount'
                                )
                                  return null;

                                // Skip marker fields for edit operations (fileName is context only)
                                if (
                                  field === 'photoEdited' ||
                                  field === 'documentEdited'
                                )
                                  return null;
                                if (
                                  field === 'fileName' &&
                                  (activity.changedFields?.photoEdited ||
                                    activity.changedFields?.documentEdited)
                                )
                                  return null;

                                // Special handling for document/photo upload/delete operations
                                if (
                                  field === 'documentAdded' ||
                                  field === 'documentRemoved' ||
                                  field === 'photoAdded' ||
                                  field === 'photoRemoved'
                                ) {
                                  const category =
                                    activity.changedFields?.category;
                                  const title = activity.changedFields?.title;
                                  return (
                                    <div
                                      key={field}
                                      className="bg-white dark:bg-[#14161f] dark:text-[#eef0f6] rounded p-2 text-xs"
                                    >
                                      <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1">
                                        File Name
                                      </div>
                                      <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                        {String(value)}
                                      </div>
                                      {title ? (
                                        <>
                                          <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1 mt-2">
                                            Title
                                          </div>
                                          <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                            {String(title)}
                                          </div>
                                        </>
                                      ) : null}
                                      <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1 mt-2">
                                        Type
                                      </div>
                                      <div className="text-[#1a1d2e] dark:text-[#eef0f6]">
                                        {category === 'PHOTO'
                                          ? 'Photo'
                                          : 'Document'}
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
                                    className="bg-white dark:bg-[#14161f] dark:text-[#eef0f6] rounded p-2 text-xs"
                                  >
                                    <div className="font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1">
                                      {field
                                        .replace(/([A-Z])/g, ' $1')
                                        .replace(/^./, (str) =>
                                          str.toUpperCase()
                                        )
                                        .trim()}
                                    </div>
                                    <div className="grid grid-cols-2 gap-2">
                                      <div>
                                        <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                          Old:{' '}
                                        </span>
                                        {typeof activity.oldValues?.[field] === 'string' && /<[a-z][\s\S]*>/i.test(activity.oldValues[field]) ? (
                                          <RichTextDisplay content={activity.oldValues[field]} className="text-xs text-red-600 line-through [&_p]:m-0 inline" />
                                        ) : (
                                          <span className="text-red-600 line-through">
                                            {String(activity.oldValues?.[field] ?? 'N/A')}
                                          </span>
                                        )}
                                      </div>
                                      <div>
                                        <span className="text-[#6b7194] dark:text-[#8b90a8]">
                                          New:{' '}
                                        </span>
                                        {typeof activity.newValues?.[field] === 'string' && /<[a-z][\s\S]*>/i.test(activity.newValues[field]) ? (
                                          <RichTextDisplay content={activity.newValues[field]} className="text-xs text-green-600 font-medium [&_p]:m-0 inline" />
                                        ) : (
                                          <span className="text-green-600 font-medium">
                                            {String(activity.newValues?.[field] ?? 'N/A')}
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
                <History className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8]">
                  No history available
                </p>
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] mt-1">
                  Changes to this tenant will appear here
                </p>
              </div>
            )}
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
          <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
            <div className="bg-white dark:bg-[#14161f] rounded-xl p-6 max-w-md w-full mx-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Delete Tenant
                </h3>
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <p className="text-[#6b7194] dark:text-[#8b90a8] mb-6">
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
