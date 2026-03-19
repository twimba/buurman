import { useState, useMemo, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTabState } from '@/hooks/useTabState';
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
import { CalendarFeedResponseFeedType as CalendarFeedType } from '@/generated/models';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import { DocumentList } from '@/components/properties/DocumentList';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { Avatar } from '@/components/common/Avatar';
import { TenantAddressList } from '@/components/tenants/TenantAddressList';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import {
  ContractStatus,
  ContractPartyRole,
  PARTY_ROLE_LABELS,
} from '@/types/contract';
import { Button, PageHeader } from '@buurman/ui';
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
  Eye,
} from 'lucide-react';
import client from '@/api/client';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';
import { getCurrencySymbol } from '@/utils/currencies';

const ROLE_COLORS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]: 'bg-info-bg text-info-text',
  [ContractPartyRole.GUARANTOR]: 'bg-warning-bg text-warning-text',
  [ContractPartyRole.COSIGNER]: 'bg-info-bg text-info-text',
  [ContractPartyRole.EXTRA_TENANT]: 'bg-success-bg text-success-text',
};

const RoleBadge = ({ role }: { role: ContractPartyRole }) => (
  <span
    className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_COLORS[role] ?? 'bg-surface-inset text-text-primary'}`}
  >
    {PARTY_ROLE_LABELS[role] ?? role}
  </span>
);

export const TenantDetailPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [activeTab, setActiveTab] = useTabState('info', [
    'info',
    'photos',
    'documents',
    'addresses',
    'contracts',
    'history',
  ] as const);
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
  const tenantIdentifier = tenant?.identifier;

  useEffect(() => {
    if (tenantIdentifier) {
      trackEvent(AnalyticsEvent.TENANT_VIEWED);
    }
  }, [tenantIdentifier]);

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
  const uploadDocumentMutation = useUploadTenantDocument(id);
  const uploadPhotoMutation = useUploadTenantPhoto(id);
  const setMainPhotoMutation = useSetTenantMainPhoto(id);
  const deleteDocumentMutation = useDeleteTenantDocument(id);
  const deletePhotoMutation = useDeletePhoto();

  const handleDelete = async () => {
    if (!id) {
      return;
    }
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

  // Get properties from active contracts with the tenant's role
  const activeContractProperties = contracts
    ? contracts
        .filter((contract) => contract.status === ContractStatus.ACTIVE)
        .map((contract) => {
          const party = contract.parties?.find(
            (p) => p.tenant.identifier === id
          );
          return {
            property: contract.property,
            role: party?.role,
          };
        })
        .filter(
          (item, index, self) =>
            index ===
            self.findIndex(
              (i) => i.property.identifier === item.property.identifier
            )
        )
    : [];

  // Contracts filtering, sorting, and pagination
  const filteredAndSortedContracts = useMemo(() => {
    if (!contracts) {
      return [];
    }
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
      if (aVal < bVal) {
        return contractsSortOrder === 'asc' ? -1 : 1;
      }
      if (aVal > bVal) {
        return contractsSortOrder === 'asc' ? 1 : -1;
      }
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
              photoUrl={tenant.mainPhotoThumbnailUrl ?? tenant.mainPhotoUrl}
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
                      `/booklets/tenant/${id}`,
                      { responseType: 'blob' }
                    );
                    const blob = new Blob([response.data], {
                      type: 'application/pdf',
                    });
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'tenant-booklet.pdf';
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                  } catch (error) {
                    console.error('Failed to download booklet:', error);
                  }
                }}
              >
                Booklet
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
        <div className="border-b border-border-default mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('info')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'info'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              Information
            </button>
            <button
              onClick={() => setActiveTab('photos')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'photos'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Image className="h-4 w-4" />
              Photos {photos.length > 0 && `(${photos.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'documents'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <FileText className="h-4 w-4" />
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('addresses')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'addresses'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <MapPin className="h-4 w-4" />
              Addresses {addresses.length > 0 && `(${addresses.length})`}
            </button>
            <button
              onClick={() => setActiveTab('contracts')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'contracts'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <FileText className="h-4 w-4" />
              Contracts {contracts.length > 0 && `(${contracts.length})`}
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
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
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Contact Information
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-text-muted " />
                  <div>
                    <p className="text-sm text-text-secondary">Name</p>
                    <p className="font-medium text-text-primary">
                      {tenant.firstName} {tenant.lastName}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-text-muted " />
                  <div>
                    <p className="text-sm text-text-secondary">Email</p>
                    <p className="font-medium text-text-primary">
                      {tenant.email}
                    </p>
                  </div>
                </div>
                {tenant.phone && (
                  <div className="flex items-center gap-3">
                    <Phone className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">Phone</p>
                      <p className="font-medium text-text-primary">
                        {tenant.phone}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.taxNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">Tax Number</p>
                      <p className="font-medium text-text-primary">
                        {tenant.taxNumber}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.idNumber && (
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-text-muted " />
                    <div>
                      <p className="text-sm text-text-secondary">
                        Government ID Number
                      </p>
                      <p className="font-medium text-text-primary">
                        {tenant.idNumber}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Information */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Additional Information
              </h2>
              {tenant.additionalInfo ? (
                <RichTextDisplay content={tenant.additionalInfo} />
              ) : (
                <p className="text-sm text-text-muted italic">
                  No additional information available
                </p>
              )}
            </div>

            {/* Current Properties (from Active Contracts) */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Current Properties
              </h2>
              {contractsLoading ? (
                <LoadingSpinner />
              ) : activeContractProperties.length > 0 ? (
                <div className="space-y-3">
                  {activeContractProperties.map((item) => (
                    <div
                      key={item.property.identifier}
                      className="flex items-center justify-between bg-success-bg p-4 rounded border border-success-border"
                    >
                      <div className="flex items-center gap-3">
                        <Home className="h-8 w-8 text-success-text" />
                        <div>
                          <button
                            onClick={() =>
                              navigate(
                                `/properties/${item.property.identifier}`
                              )
                            }
                            className="font-medium text-text-primary hover:text-primary-500 text-left"
                          >
                            {item.property.street}
                          </button>
                          <p className="text-sm text-text-secondary">
                            {item.property.city}, {item.property.postalCode}
                          </p>
                        </div>
                      </div>
                      {item.role && <RoleBadge role={item.role} />}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-text-muted italic">
                  No active contracts for this tenant
                </p>
              )}
            </div>

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
                      {formatDate(tenant.createdAt)} at{' '}
                      {new Date(tenant.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-text-secondary">Last Updated:</span>{' '}
                    <span className="text-text-primary">
                      {tenant.updatedAt ? (
                        <>
                          {formatDate(tenant.updatedAt)} at{' '}
                          {new Date(tenant.updatedAt).toLocaleTimeString()}
                        </>
                      ) : (
                        '—'
                      )}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'photos' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <PhotoGallery
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
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <TenantAddressList tenantId={id} />
          </div>
        )}

        {activeTab === 'contracts' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-text-primary">
                Contracts ({filteredAndSortedContracts.length})
              </h2>
              <button
                onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                disabled={!canEditData}
                className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
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
                <FileText className="h-12 w-12 text-text-disabled mx-auto mb-3" />
                <p className="text-text-secondary mb-4">
                  No contracts for this tenant
                </p>
                <button
                  onClick={() => navigate(`/contracts/new?tenantId=${id}`)}
                  disabled={!canEditData}
                  className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
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
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-text-muted " />
                    <input
                      type="text"
                      placeholder="Search by contract #, property, type..."
                      value={contractsSearchTerm}
                      onChange={(e) => {
                        setContractsSearchTerm(e.target.value);
                        setContractsCurrentPage(1);
                      }}
                      className="w-full pl-10 pr-4 py-2 border border-border-strong rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-border-default">
                    <thead className="bg-surface-page">
                      <tr>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
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
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
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
                        <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Role
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
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
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
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
                        <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          End Date
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
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
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
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
                    <tbody className="bg-surface-card divide-y divide-border-default">
                      {paginatedContracts.length === 0 ? (
                        <tr>
                          <td
                            colSpan={8}
                            className="px-6 py-12 text-center text-text-secondary"
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
                            className="hover:bg-primary-50 cursor-pointer transition-colors"
                          >
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm font-medium text-primary-500 dark:text-primary-300">
                                #{contract.identifier}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-text-primary">
                                {contract.property.street}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              {(() => {
                                const party = contract.parties?.find(
                                  (p) => p.tenant.identifier === id
                                );
                                return party?.role ? (
                                  <RoleBadge role={party.role} />
                                ) : (
                                  <span className="text-sm text-text-muted">
                                    -
                                  </span>
                                );
                              })()}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-text-primary">
                                {contract.contractType.replace('_', '')}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-text-primary">
                                {formatDate(contract.startDate)}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-text-primary">
                                {contract.effectiveEndDate ?? contract.endDate
                                  ? formatDate(
                                      (contract.effectiveEndDate ?? contract.endDate) as string
                                    )
                                  : '-'}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm font-medium text-text-primary">
                                {getCurrencySymbol(contract.rentAmountCurrency)}{' '}
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
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-border-default">
                    <div className="text-sm text-text-secondary">
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
                        className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-text-secondary">
                        Page {contractsCurrentPage} of {contractsTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setContractsCurrentPage(contractsCurrentPage + 1)
                        }
                        disabled={contractsCurrentPage === contractsTotalPages}
                        className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
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
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
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
                            {Object.entries(activity.changedFields ?? {})
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
                                    className="bg-surface-card rounded p-2 text-xs"
                                  >
                                    <div className="font-semibold text-text-secondary mb-1">
                                      {field
                                        .replace(/([A-Z])/g, ' $1')
                                        .replace(/^./, (str) =>
                                          str.toUpperCase()
                                        )
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
                <p className="text-text-secondary">No history available</p>
                <p className="text-sm text-text-muted mt-1">
                  Changes to this tenant will appear here
                </p>
              </div>
            )}
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
          <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
            <div className="bg-surface-card rounded-lg p-6 max-w-md w-full mx-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-text-primary">
                  Delete Tenant
                </h3>
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="text-text-muted hover:text-text-secondary"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <p className="text-text-secondary mb-6">
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
