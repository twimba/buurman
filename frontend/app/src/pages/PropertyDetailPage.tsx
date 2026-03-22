import { useState, useMemo, useEffect } from 'react';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { formatAuditValue } from '@/utils/formatAuditValue';
import { useParams, useNavigate } from 'react-router-dom';
import { useTabState } from '@/hooks/useTabState';
import {
  useProperty,
  useDeleteProperty,
  usePropertyDocuments,
  useUploadPropertyDocument,
  useDeleteDocument,
  usePropertyAuditLog,
  usePropertyPhotos,
  useUploadPropertyPhoto,
  useSetMainPhoto,
} from '@/hooks/usePropertyHooks';
import { useDeletePhoto } from '@/hooks/usePhotoHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { CalendarFeedResponseFeedType as CalendarFeedType } from '@/generated/models';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { useExpensesByProperty } from '@/hooks/useExpenseHooks';
import {
  useOccupancyPeriods,
  useDeleteOccupancyPeriod,
} from '@/hooks/useOccupancyPeriodHooks';
import { useFinancings } from '@/hooks/usePropertyFinancialsHooks';
import { FinancingFormModal } from '@/components/properties/financials/modals/FinancingFormModal';
import { OCCUPANCY_TYPE_LABELS } from '@/types/occupancyPeriod';
import { SelfOccupancyModal } from '@/components/properties/SelfOccupancyModal';
import { WwsCalculatorModal } from '@/components/wws/WwsCalculatorModal';
import {
  useLatestWwsCalculation,
  useWwsCalculations,
  useDeleteWwsCalculation,
} from '@/hooks/useWwsHooks';
import { EndSelfOccupancyModal } from '@/components/properties/EndSelfOccupancyModal';
import { EditSelfOccupancyModal } from '@/components/properties/EditSelfOccupancyModal';
import { SelfOccupancyCard } from '@/components/properties/SelfOccupancyCard';
import { PropertyLifecycleTimeline } from '@/components/properties/PropertyLifecycleTimeline';
import { PropertyTypeIcon } from '@/components/common/PropertyTypeIcon';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import {
  PropertyStatus,
  PropertyCategory,
  PROPERTY_TYPE_LABELS,
  PROPERTY_CATEGORY_LABELS,
  PROPERTY_STATUS_LABELS,
} from '@/types/property';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { PropertyDashboardTab } from '@/components/properties/dashboard/PropertyDashboardTab';
import { PropertyFinancialsTab } from '@/components/properties/financials/PropertyFinancialsTab';
import { FeatureGate } from '@/components/FeatureGate';
import { FeatureFlags } from '@/constants/featureFlags';
import { ErrorMessage } from '@/components/ErrorMessage';
import { DocumentList } from '@/components/properties/DocumentList';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { InteractiveMap } from '@/components/common/InteractiveMap';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { Button, PageHeader } from '@buurman/ui';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import { useTeam } from '@/context/TeamContext';
import client from '@/api/client';
import {
  Edit,
  Trash2,
  Square,
  Bed,
  Bath,
  Ruler,
  MapPin,
  History,
  Image,
  FileText,
  Plus,
  Receipt,
  Search,
  ChevronUp,
  ChevronDown,
  Download,
  BarChart3,
  Wallet,
  Home,
  Calculator,
  X,
  Eye,
} from 'lucide-react';
import DOMPurify from 'dompurify';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';

const statusColors: Record<string, string> = {
  VACANT: 'bg-success-bg text-success-text',
  OCCUPIED: 'bg-info-bg text-info-text',
  MAINTENANCE: 'bg-warning-bg text-warning-text',
  UNAVAILABLE: 'bg-surface-inset text-text-primary',
  UNDER_RENOVATION: 'bg-warning-bg text-warning-text',
  FALLOW: 'bg-surface-inset text-text-primary',
  LISTED: 'bg-info-bg text-info-text',
  SELF_OCCUPIED: 'bg-info-bg text-info-text',
};

export const PropertyDetailPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData, canManageMembers } = useTeam();
  const { formatDate } = useFormatDate();
  const [activeTab, setActiveTab] = useTabState('info', [
    'info',
    'financials',
    'photos',
    'documents',
    'contracts',
    'expenses',
    'audit',
    'dashboard',
  ] as const);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showSelfOccupancyModal, setShowSelfOccupancyModal] = useState(false);
  const [showEndOccupancyModal, setShowEndOccupancyModal] = useState(false);
  const [endOccupancyPeriodId, setEndOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [deleteOccupancyPeriodId, setDeleteOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [editOccupancyPeriodId, setEditOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [editFinancingId, setEditFinancingId] = useState<string | null>(null);
  const [showWwsModal, setShowWwsModal] = useState(false);
  const [showDeleteWwsConfirm, setShowDeleteWwsConfirm] = useState(false);
  const [deleteWwsHistoryId, setDeleteWwsHistoryId] = useState<string | null>(
    null
  );
  const [showWwsHistory, setShowWwsHistory] = useState(false);
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);

  // Contracts table state
  const [contractsSearchTerm, setContractsSearchTerm] = useState('');
  const [contractsSortField, setContractsSortField] = useState<
    'startDate' | 'rentAmount' | 'status' | 'contact' | 'contractType'
  >('startDate');
  const [contractsSortOrder, setContractsSortOrder] = useState<'asc' | 'desc'>(
    'desc'
  );
  const [contractsCurrentPage, setContractsCurrentPage] = useState(1);
  const contractsPerPage = 10;

  // Expenses table state
  const [expensesSearchTerm, setExpensesSearchTerm] = useState('');
  const [expensesSortField, setExpensesSortField] = useState<
    'expenseDate' | 'amount' | 'category' | 'description'
  >('expenseDate');
  const [expensesSortOrder, setExpensesSortOrder] = useState<'asc' | 'desc'>(
    'desc'
  );
  const [expensesCurrentPage, setExpensesCurrentPage] = useState(1);
  const expensesPerPage = 10;

  const { data: property, isLoading, error } = useProperty(id);
  const propertyIdentifier = property?.identifier;

  useEffect(() => {
    if (propertyIdentifier) {
      trackEvent(AnalyticsEvent.PROPERTY_VIEWED);
    }
  }, [propertyIdentifier]);

  const isNlProperty = property?.countryCode === 'NL';
  const { data: latestWws } = useLatestWwsCalculation(
    isNlProperty ? id : undefined
  );
  const { data: wwsHistory = [] } = useWwsCalculations(
    isNlProperty && showWwsHistory ? id : undefined
  );
  const deleteWwsMutation = useDeleteWwsCalculation(id);
  const {
    data: allDocuments = [],
    isLoading: docsLoading,
    error: docsError,
  } = usePropertyDocuments(id);
  const {
    data: photos = [],
    isLoading: photosLoading,
    error: photosError,
  } = usePropertyPhotos(id);

  const documents = allDocuments;
  const {
    data: auditLog = [],
    isLoading: auditLoading,
    error: auditError,
  } = usePropertyAuditLog(id);
  const {
    data: contractsData,
    isLoading: contractsLoading,
    error: contractsError,
  } = useContracts(id ? { propertyIdentifier: id } : undefined);
  const contracts = useMemo(
    () => contractsData?.content ?? [],
    [contractsData]
  );
  const {
    data: expenses = [],
    isLoading: expensesLoading,
    error: expensesError,
  } = useExpensesByProperty(id);
  const { data: occupancyPeriods = [] } = useOccupancyPeriods(id);
  const { data: financings = [] } = useFinancings(id);
  const activeOccupancyPeriod = occupancyPeriods.find(
    (p) => !p.endDate || new Date(p.endDate) >= new Date()
  );
  const deleteOccupancyMutation = useDeleteOccupancyPeriod(id);
  const deletePropertyMutation = useDeleteProperty();
  const uploadDocumentMutation = useUploadPropertyDocument(id);
  const uploadPhotoMutation = useUploadPropertyPhoto(id);
  const setMainPhotoMutation = useSetMainPhoto(id);
  const deleteDocumentMutation = useDeleteDocument(id);
  const deletePhotoMutation = useDeletePhoto();

  // Contracts filtering, sorting, and pagination
  const filteredAndSortedContracts = useMemo(() => {
    if (!contracts) {
      return [];
    }

    let filtered = [...contracts];

    // Apply search filter
    if (contractsSearchTerm) {
      const search = contractsSearchTerm.toLowerCase();
      filtered = filtered.filter(
        (contract) =>
          contract.identifier.toLowerCase().includes(search) ||
          `${contract.primaryContact.firstName} ${contract.primaryContact.lastName}`
            .toLowerCase()
            .includes(search) ||
          contract.contractType.toLowerCase().includes(search)
      );
    }

    // Apply sorting
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
        case 'contact':
          aVal = `${a.primaryContact.firstName} ${a.primaryContact.lastName}`;
          bVal = `${b.primaryContact.firstName} ${b.primaryContact.lastName}`;
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

  // Paginated contracts
  const paginatedContracts = useMemo(() => {
    const startIndex = (contractsCurrentPage - 1) * contractsPerPage;
    const endIndex = startIndex + contractsPerPage;
    return filteredAndSortedContracts.slice(startIndex, endIndex);
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

  // Expenses filtering, sorting, and pagination
  const filteredAndSortedExpenses = useMemo(() => {
    if (!expenses) {
      return [];
    }

    let filtered = [...expenses];

    // Apply search filter
    if (expensesSearchTerm) {
      const search = expensesSearchTerm.toLowerCase();
      filtered = filtered.filter(
        (expense) =>
          expense.identifier.toLowerCase().includes(search) ||
          expense.description.toLowerCase().includes(search) ||
          expense.category.toLowerCase().includes(search)
      );
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

      switch (expensesSortField) {
        case 'expenseDate':
          aVal = new Date(a.expenseDate).getTime();
          bVal = new Date(b.expenseDate).getTime();
          break;
        case 'amount':
          aVal = a.amount;
          bVal = b.amount;
          break;
        case 'category':
          aVal = a.category;
          bVal = b.category;
          break;
        case 'description':
          aVal = a.description;
          bVal = b.description;
          break;
        default:
          return 0;
      }

      if (aVal < bVal) {
        return expensesSortOrder === 'asc' ? -1 : 1;
      }
      if (aVal > bVal) {
        return expensesSortOrder === 'asc' ? 1 : -1;
      }
      return 0;
    });

    return filtered;
  }, [expenses, expensesSearchTerm, expensesSortField, expensesSortOrder]);

  // Paginated expenses
  const paginatedExpenses = useMemo(() => {
    const startIndex = (expensesCurrentPage - 1) * expensesPerPage;
    const endIndex = startIndex + expensesPerPage;
    return filteredAndSortedExpenses.slice(startIndex, endIndex);
  }, [filteredAndSortedExpenses, expensesCurrentPage]);

  const expensesTotalPages = Math.ceil(
    filteredAndSortedExpenses.length / expensesPerPage
  );

  const handleExpensesSort = (field: typeof expensesSortField) => {
    if (expensesSortField === field) {
      setExpensesSortOrder(expensesSortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setExpensesSortField(field);
      setExpensesSortOrder('asc');
    }
  };

  const handleDelete = async () => {
    if (!id) {
      return;
    }
    try {
      await deletePropertyMutation.mutateAsync(id);
      navigate('/properties');
    } catch (err) {
      console.error('Failed to delete property:', err);
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

  const toggleAuditItem = (itemId: string) => {
    const newExpanded = new Set(expandedAuditItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedAuditItems(newExpanded);
  };

  const formatFieldName = (field: string): string => {
    // Handle special field names
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

    // Convert camelCase to Title Case with spaces
    return field
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  const formatEnumValue = (value: string | null): string => {
    if (!value) {
      return '';
    }
    return value
      .replace(/_/g, '')
      .replace(/\b\w/g, (c) => c.toUpperCase())
      .replace(/\bAc\b/g, 'AC')
      .replace(/\bCo\b/g, 'CO')
      .replace(/\bDsl\b/g, 'DSL');
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Property not found" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={property.street}
          subtitle={`#${property.identifier}`}
          description={`${property.city}, ${property.postalCode}`}
          backTo="/properties"
          badge={
            <span
              className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusColors[property.status] ?? 'bg-gray-100 text-gray-800'}`}
            >
              {PROPERTY_STATUS_LABELS[property.status as PropertyStatus] ??
                property.status}
            </span>
          }
          actions={
            <>
              <Button
                variant="primary"
                leftIcon={<Download />}
                onClick={async () => {
                  try {
                    const response = await client.get(
                      `/booklets/property/${id}`,
                      {
                        responseType: 'blob',
                      }
                    );
                    const blob = new Blob([response.data], {
                      type: 'application/pdf',
                    });
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'property-booklet.pdf';
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                  } catch (error) {
                    console.error('Failed to download booklet:', error);
                    alert('Failed to download booklet. Please try again.');
                  }
                }}
              >
                Booklet
              </Button>
              {id && (
                <CalendarFeedButton
                  feedType={CalendarFeedType.PROPERTY_PAYMENTS}
                  entityIdentifier={id}
                />
              )}
              <Button
                variant="secondary"
                leftIcon={<Edit />}
                onClick={() => navigate(`/properties/${id}/edit`)}
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
        <div className="border-b mb-6">
          <div className="flex gap-8">
            <button
              onClick={() => setActiveTab('info')}
              className={`px-4 py-2 border-b-2 transition-colors ${
                activeTab === 'info'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              Info
            </button>
            <FeatureGate flag={FeatureFlags.REPORTS}>
              <button
                onClick={() => setActiveTab('dashboard')}
                className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                  activeTab === 'dashboard'
                    ? 'border-primary-500 text-primary-500 font-semibold'
                    : 'border-transparent text-text-secondary hover:text-text-primary'
                }`}
              >
                <BarChart3 className="h-4 w-4" />
                Dashboard
              </button>
            </FeatureGate>
            <button
              onClick={() => setActiveTab('financials')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'financials'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              <Wallet className="h-4 w-4" />
              Financials
            </button>
            <button
              onClick={() => setActiveTab('photos')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'photos'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              <Image className="h-4 w-4" />
              Photos {photos.length > 0 && `(${photos.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`px-4 py-2 border-b-2 transition-colors ${
                activeTab === 'documents'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('contracts')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'contracts'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              <FileText className="h-4 w-4" />
              Contracts {contracts.length > 0 && `(${contracts.length})`}
            </button>
            <button
              onClick={() => setActiveTab('expenses')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'expenses'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              <Receipt className="h-4 w-4" />
              Expenses {expenses.length > 0 && `(${expenses.length})`}
            </button>
            <button
              onClick={() => setActiveTab('audit')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'audit'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              <History className="h-4 w-4" />
              History {auditLog.length > 0 && `(${auditLog.length})`}
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'info' && (
          <div className="space-y-6">
            {/* Property Lifecycle Timeline */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm font-semibold text-text-primary">
                  Property Timeline
                </span>
                <div className="flex items-center gap-4 text-xs text-text-secondary">
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-3 h-2.5 rounded-full"
                      style={{ background: 'rgba(59,130,246,1)' }}
                    />
                    Rental contract
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-3 h-2.5 rounded-full"
                      style={{ background: 'rgba(99,102,241,1)' }}
                    />
                    Self-occupied
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-3 h-1.5 rounded-full"
                      style={{ background: 'rgba(245,158,11,0.5)' }}
                    />
                    Financing
                  </span>
                </div>
              </div>
              <PropertyLifecycleTimeline
                propertyIdentifier={id}
                onSelfOccupancyClick={(identifier) =>
                  setEditOccupancyPeriodId(identifier)
                }
                onFinancingClick={(identifier) =>
                  setEditFinancingId(identifier)
                }
              />
            </div>

            {activeOccupancyPeriod && (
              <SelfOccupancyCard
                period={activeOccupancyPeriod}
                canEdit={canEditData}
                canAdmin={canManageMembers}
                onEnd={() => {
                  setEndOccupancyPeriodId(activeOccupancyPeriod.identifier);
                  setShowEndOccupancyModal(true);
                }}
                onDelete={() =>
                  setDeleteOccupancyPeriodId(activeOccupancyPeriod.identifier)
                }
              />
            )}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 space-y-6">
              {/* Status & Category — elegant inline display */}
              <div className="flex items-center gap-4 text-sm pb-2 border-b border-border-subtle">
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-block w-2.5 h-2.5 rounded-full flex-shrink-0 ${
                      {
                        VACANT: 'bg-success-text',
                        OCCUPIED: 'bg-info-text',
                        MAINTENANCE: 'bg-warning-text',
                        UNAVAILABLE: 'bg-text-disabled',
                        UNDER_RENOVATION: 'bg-warning-text',
                        FALLOW: 'bg-text-disabled',
                        LISTED: 'bg-info-text',
                        SELF_OCCUPIED: 'bg-info-text',
                      }[property.status] ?? 'bg-gray-400'
                    }`}
                  />
                  <span className="font-semibold text-text-primary">
                    {PROPERTY_STATUS_LABELS[
                      property.status as PropertyStatus
                    ] ?? property.status}
                  </span>
                </div>
                <span className="text-text-disabled">·</span>
                <span className="inline-flex items-center gap-1 text-text-secondary">
                  <PropertyTypeIcon
                    category={property.propertyCategory}
                    size={13}
                  />
                  {PROPERTY_CATEGORY_LABELS[
                    property.propertyCategory as PropertyCategory
                  ] ?? property.propertyCategory}
                </span>
                <span className="text-text-disabled">·</span>
                <span className="inline-flex items-center gap-1 text-text-secondary">
                  <PropertyTypeIcon type={property.propertyType} size={13} />
                  {PROPERTY_TYPE_LABELS[property.propertyType] ??
                    property.propertyType}
                </span>
              </div>

              {/* Specifications Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {property.residentialDetails?.bedrooms != null && (
                  <div>
                    <div className="flex items-center gap-2 text-text-secondary mb-1">
                      <Bed className="h-5 w-5" />
                      <span className="text-sm font-medium">Bedrooms</span>
                    </div>
                    <p className="text-2xl font-semibold text-text-primary">
                      {property.residentialDetails.bedrooms}
                    </p>
                  </div>
                )}

                {property.residentialDetails?.bathrooms != null && (
                  <div>
                    <div className="flex items-center gap-2 text-text-secondary mb-1">
                      <Bath className="h-5 w-5" />
                      <span className="text-sm font-medium">Bathrooms</span>
                    </div>
                    <p className="text-2xl font-semibold text-text-primary">
                      {property.residentialDetails.bathrooms}
                    </p>
                  </div>
                )}

                {property.areaValue != null && (
                  <div>
                    <div className="flex items-center gap-2 text-text-secondary mb-1">
                      <Ruler className="h-5 w-5" />
                      <span className="text-sm font-medium">Area</span>
                    </div>
                    <p className="text-2xl font-semibold text-text-primary">
                      {property.areaValue}
                      {property.areaUnit === 'sqft' ? 'ft²' : 'm²'}
                    </p>
                  </div>
                )}

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <PropertyTypeIcon type={property.propertyType} size={20} />
                    <span className="text-sm font-medium">Type</span>
                  </div>
                  <p className="text-lg font-semibold text-text-primary">
                    {PROPERTY_TYPE_LABELS[property.propertyType] ??
                      property.propertyType}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Street</span>
                  </div>
                  <p className="text-lg text-text-primary">{property.street}</p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">City</span>
                  </div>
                  <p className="text-lg text-text-primary">{property.city}</p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Postal Code</span>
                  </div>
                  <p className="text-lg text-text-primary">
                    {property.postalCode}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Country</span>
                  </div>
                  <p className="text-lg text-text-primary">
                    {property.countryCode}
                  </p>
                </div>
              </div>

              {/* Map */}
              <div className="pt-6 border-t">
                <h3 className="text-sm font-semibold text-text-secondary mb-3">
                  Location
                </h3>
                <InteractiveMap
                  street={property.street}
                  city={property.city}
                  latitude={property.latitude}
                  longitude={property.longitude}
                  geocodeAccuracy={property.geocodeAccuracy}
                />
              </div>
            </div>

            {/* Construction & Structure */}
            {(property.yearBuilt != null ||
              property.yearLastRenovated != null ||
              property.constructionType ||
              property.foundationType ||
              property.roofType ||
              property.wallConstruction ||
              property.flooringType ||
              property.windowType ||
              property.numberOfFloors != null ||
              property.structuralNotes) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Construction &amp; Structure
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.yearBuilt != null && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Year Built
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {property.yearBuilt}
                      </div>
                    </div>
                  )}
                  {property.yearLastRenovated != null && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Last Renovated
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {property.yearLastRenovated}
                      </div>
                    </div>
                  )}
                  {property.constructionType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Construction
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.constructionType)}
                      </div>
                    </div>
                  )}
                  {property.foundationType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Foundation
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.foundationType)}
                      </div>
                    </div>
                  )}
                  {property.roofType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Roof
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.roofType)}
                      </div>
                    </div>
                  )}
                  {property.wallConstruction && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Wall Construction
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.wallConstruction)}
                      </div>
                    </div>
                  )}
                  {property.flooringType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Flooring
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.flooringType)}
                      </div>
                    </div>
                  )}
                  {property.windowType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Windows
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.windowType)}
                      </div>
                    </div>
                  )}
                  {property.numberOfFloors != null && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Floors
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {property.numberOfFloors}
                      </div>
                    </div>
                  )}
                </div>
                {property.structuralNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                      Structural Notes
                    </div>
                    <div
                      className="text-sm text-text-primary mt-1 prose prose-sm dark:prose-invert max-w-none"
                      dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(property.structuralNotes),
                      }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Energy & Climate */}
            {(property.energyEfficiencyRating ||
              property.energyCertificateExpiryDate ||
              property.heatingType ||
              property.coolingType ||
              property.hotWaterSystem ||
              property.insulationNotes) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Energy &amp; Climate
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.energyEfficiencyRating && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Energy Rating
                      </div>
                      <div className="mt-1">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold text-white ${
                            (
                              {
                                'A++': 'bg-green-900',
                                'A+': 'bg-green-700',
                                A: 'bg-green-500',
                                B: 'bg-lime-500',
                                C: 'bg-yellow-500',
                                D: 'bg-orange-500',
                                E: 'bg-orange-600',
                                F: 'bg-red-500',
                                G: 'bg-red-800',
                              } as Record<string, string>
                            )[property.energyEfficiencyRating] || 'bg-gray-500'
                          }`}
                        >
                          {property.energyEfficiencyRating}
                        </span>
                      </div>
                    </div>
                  )}
                  {property.energyCertificateExpiryDate && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Certificate Expiry
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatDate(property.energyCertificateExpiryDate)}
                      </div>
                    </div>
                  )}
                  {property.heatingType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Heating
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.heatingType)}
                      </div>
                    </div>
                  )}
                  {property.coolingType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Cooling
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.coolingType)}
                      </div>
                    </div>
                  )}
                  {property.hotWaterSystem && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Hot Water
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.hotWaterSystem)}
                      </div>
                    </div>
                  )}
                </div>
                {property.insulationNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                      Insulation Notes
                    </div>
                    <div
                      className="text-sm text-text-primary mt-1 prose prose-sm dark:prose-invert max-w-none"
                      dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(property.insulationNotes),
                      }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Utilities & Connections */}
            {(property.electricityConnectionType ||
              property.electricityCapacityValue != null ||
              property.waterConnectionType ||
              property.sewageType ||
              property.internetConnectionType ||
              property.internetMaxSpeedValue != null ||
              property.internetStatus) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Utilities &amp; Connections
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.electricityConnectionType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Electricity
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.electricityConnectionType)}
                      </div>
                    </div>
                  )}
                  {property.electricityCapacityValue != null && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Capacity
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {property.electricityCapacityValue}{' '}
                        {(
                          property.electricityCapacityUnit ?? 'A'
                        ).toUpperCase()}
                      </div>
                    </div>
                  )}
                  {property.waterConnectionType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Water
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.waterConnectionType)}
                      </div>
                    </div>
                  )}
                  <div>
                    <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                      Gas Connection
                    </div>
                    <div className="text-sm font-medium text-text-primary mt-1">
                      {property.hasGasConnection ? 'Yes' : 'No'}
                    </div>
                  </div>
                  {property.sewageType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Sewage
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.sewageType)}
                      </div>
                    </div>
                  )}
                  {property.internetConnectionType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Internet
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.internetConnectionType)}
                      </div>
                    </div>
                  )}
                  {property.internetMaxSpeedValue != null && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Internet Speed
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {property.internetMaxSpeedValue}{' '}
                        {(
                          property.internetMaxSpeedUnit ?? 'Mbps'
                        ).toUpperCase()}
                      </div>
                    </div>
                  )}
                  {property.internetStatus && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Internet Status
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.internetStatus)}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Parking */}
            {(property.parkingSpaces != null || property.parkingType) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Parking
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.parkingSpaces != null && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Parking Spaces
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {property.parkingSpaces}
                      </div>
                    </div>
                  )}
                  {property.parkingType && (
                    <div>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                        Parking Type
                      </div>
                      <div className="text-sm font-medium text-text-primary mt-1">
                        {formatEnumValue(property.parkingType)}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Outdoor Areas */}
            {property.outdoorAreas && property.outdoorAreas.length > 0 && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Outdoor Areas
                </h3>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                  {property.outdoorAreas.map((area) => (
                    <span
                      key={area.identifier}
                      className="inline-flex items-center gap-1.5 text-sm text-text-primary"
                    >
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      {formatEnumValue(area.type)}
                      {area.areaValue != null
                        ? ` - ${area.areaValue} m\u00B2`
                        : ''}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Amenities */}
            {property.amenities && property.amenities.length > 0 && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Amenities
                </h3>
                <div className="space-y-4">
                  {Object.entries(
                    property.amenities.reduce<
                      Record<string, typeof property.amenities>
                    >((groups, amenity) => {
                      const cat = amenity.amenityCategory;
                      if (!groups[cat]) {
                        groups[cat] = [];
                      }
                      groups[cat].push(amenity);
                      return groups;
                    }, {})
                  ).map(([category, items]) => (
                    <div key={category}>
                      <div className="text-xs font-medium text-text-secondary uppercase tracking-wide mb-2">
                        {formatEnumValue(category)}
                      </div>
                      <div className="flex flex-wrap gap-x-6 gap-y-2">
                        {(items ?? []).map((amenity) => (
                          <span
                            key={amenity.amenityIdentifier}
                            className="inline-flex items-center gap-1.5 text-sm text-text-primary"
                          >
                            <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                              {'\u2713'}
                            </span>
                            {amenity.amenityName}
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Safety & Security */}
            {(property.hasSmokeDetectors ||
              property.hasCoDetectors ||
              property.hasFireExtinguisher ||
              property.hasSprinklerSystem ||
              property.hasAlarmSystem ||
              property.hasSecurityCameras ||
              property.hasSecureEntry ||
              property.safetyNotes) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Safety &amp; Security
                </h3>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                  {property.hasSmokeDetectors && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Smoke Detectors
                    </span>
                  )}
                  {property.hasCoDetectors && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      CO Detectors
                    </span>
                  )}
                  {property.hasFireExtinguisher && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Fire Extinguisher
                    </span>
                  )}
                  {property.hasSprinklerSystem && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Sprinkler System
                    </span>
                  )}
                  {property.hasAlarmSystem && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Alarm System
                    </span>
                  )}
                  {property.hasSecurityCameras && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Security Cameras
                    </span>
                  )}
                  {property.hasSecureEntry && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Secure Entry
                    </span>
                  )}
                </div>
                {property.safetyNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                      Safety Notes
                    </div>
                    <div
                      className="text-sm text-text-primary mt-1 prose prose-sm dark:prose-invert max-w-none"
                      dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(property.safetyNotes),
                      }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Accessibility */}
            {(property.isWheelchairAccessible ||
              property.hasElevator ||
              property.hasStepFreeEntrance ||
              property.hasAdaptedBathroom ||
              property.accessibilityNotes) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide mb-4">
                  Accessibility
                </h3>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                  {property.isWheelchairAccessible && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Wheelchair Accessible
                    </span>
                  )}
                  {property.hasElevator && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Elevator
                    </span>
                  )}
                  {property.hasStepFreeEntrance && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Step-Free Entrance
                    </span>
                  )}
                  {property.hasAdaptedBathroom && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-text-primary">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-success-bg text-success-text flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Adapted Bathroom
                    </span>
                  )}
                </div>
                {property.accessibilityNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                      Accessibility Notes
                    </div>
                    <div
                      className="text-sm text-text-primary mt-1 prose prose-sm dark:prose-invert max-w-none"
                      dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(property.accessibilityNotes),
                      }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Investment & Financial panel removed — data now in Financials tab */}

            {/* WWS Points Calculator — NL properties only */}
            {isNlProperty && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-sm font-semibold text-text-secondary uppercase tracking-wide">
                    WWS Points
                  </h3>
                  <div className="flex items-center gap-3">
                    {latestWws?.identifier && (
                      <button
                        onClick={() => setShowDeleteWwsConfirm(true)}
                        className="inline-flex items-center gap-1.5 text-xs font-medium text-error-text hover:text-error-text/80 transition-colors"
                      >
                        Delete
                      </button>
                    )}
                    <button
                      onClick={() => setShowWwsModal(true)}
                      className="inline-flex items-center gap-1.5 text-xs font-medium text-primary-500 hover:text-primary-600 transition-colors"
                    >
                      <Calculator className="h-3.5 w-3.5" />
                      {latestWws ? 'Recalculate' : 'Calculate'}
                    </button>
                  </div>
                </div>
                {latestWws ? (
                  <div className="space-y-4">
                    <div className="flex items-baseline justify-between">
                      <div className="text-2xl font-bold text-text-primary">
                        {latestWws.totalPoints}
                        <span className="text-sm font-normal text-text-secondary ml-1.5">
                          points
                        </span>
                      </div>
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                          latestWws.sectorClassification === 'REGULATED'
                            ? 'bg-success-bg text-success-text'
                            : latestWws.sectorClassification === 'MID_SEGMENT'
                              ? 'bg-warning-bg text-warning-text'
                              : 'bg-error-bg text-error-text'
                        }`}
                      >
                        {latestWws.sectorClassification === 'REGULATED' &&
                          'Regulated'}
                        {latestWws.sectorClassification === 'MID_SEGMENT' &&
                          'Mid-Segment'}
                        {latestWws.sectorClassification === 'FREE_SECTOR' &&
                          'Free Sector'}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      {latestWws.maxRentIndication != null && (
                        <div>
                          <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                            Max Rent
                          </div>
                          <div className="text-sm font-semibold text-text-primary mt-0.5">
                            EUR {latestWws.maxRentIndication.toFixed(2)}
                          </div>
                        </div>
                      )}
                      <div>
                        <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                          Calculated
                        </div>
                        <div className="text-sm text-text-primary mt-0.5">
                          {new Date(
                            latestWws.calculationDate
                          ).toLocaleDateString()}
                        </div>
                      </div>
                    </div>

                    {/* History toggle */}
                    <button
                      onClick={() => setShowWwsHistory(!showWwsHistory)}
                      className="flex items-center gap-1.5 text-xs text-text-secondary hover:text-text-secondary transition-colors mt-1"
                    >
                      <History className="h-3 w-3" />
                      History
                      <ChevronDown
                        className={`h-3 w-3 transition-transform ${showWwsHistory ? 'rotate-180' : ''}`}
                      />
                    </button>

                    {showWwsHistory && wwsHistory.length > 1 && (
                      <div className="mt-3 space-y-2">
                        {wwsHistory
                          .filter((c) => c.identifier !== latestWws?.identifier)
                          .map((calc) => (
                            <div
                              key={calc.identifier}
                              className="flex items-center justify-between px-3 py-2 rounded-lg bg-surface-inset dark:bg-surface-card border border-border-default"
                            >
                              <div className="flex items-baseline gap-2">
                                <span className="text-sm font-semibold text-text-primary">
                                  {calc.totalPoints} pts
                                </span>
                                <span
                                  className={`text-xs font-medium ${
                                    calc.sectorClassification === 'REGULATED'
                                      ? 'text-success-text'
                                      : calc.sectorClassification ===
                                          'MID_SEGMENT'
                                        ? 'text-warning-text'
                                        : 'text-error-text'
                                  }`}
                                >
                                  {calc.sectorClassification === 'REGULATED' &&
                                    'Regulated'}
                                  {calc.sectorClassification ===
                                    'MID_SEGMENT' && 'Mid-Segment'}
                                  {calc.sectorClassification ===
                                    'FREE_SECTOR' && 'Free Sector'}
                                </span>
                              </div>
                              <div className="flex items-center gap-2">
                                <span className="text-xs text-text-secondary">
                                  v{calc.systemVersion} &middot;{' '}
                                  {new Date(
                                    calc.calculationDate
                                  ).toLocaleDateString()}
                                </span>
                                {calc.identifier && (
                                  <button
                                    onClick={() =>
                                      setDeleteWwsHistoryId(
                                        calc.identifier ?? null
                                      )
                                    }
                                    className="text-text-secondary hover:text-error-text transition-colors"
                                  >
                                    <X className="h-3.5 w-3.5" />
                                  </button>
                                )}
                              </div>
                            </div>
                          ))}
                        {wwsHistory.filter(
                          (c) => c.identifier !== latestWws?.identifier
                        ).length === 0 && (
                          <p className="text-xs text-text-secondary">
                            No previous calculations.
                          </p>
                        )}
                      </div>
                    )}

                    {showWwsHistory && wwsHistory.length <= 1 && (
                      <p className="mt-3 text-xs text-text-secondary">
                        No previous calculations.
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="text-center py-4">
                    <div className="w-10 h-10 rounded-lg bg-surface-inset flex items-center justify-center mx-auto mb-3">
                      <Calculator className="h-5 w-5 text-text-secondary " />
                    </div>
                    <p className="text-sm text-text-secondary">
                      No calculation yet
                    </p>
                    <p className="text-xs text-text-secondary mt-1">
                      Calculate the WWS points to determine the maximum
                      regulated rent for this property.
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* Metadata */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
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
                      {formatDate(property.createdAt)} at{' '}
                      {new Date(property.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-text-secondary">Last Updated:</span>{' '}
                    <span className="text-text-primary">
                      {formatDate(property.updatedAt)} at{' '}
                      {new Date(property.updatedAt).toLocaleTimeString()}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'dashboard' && <PropertyDashboardTab propertyId={id} />}

        {activeTab === 'financials' && (
          <PropertyFinancialsTab propertyId={id} />
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

        {activeTab === 'contracts' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-text-primary">
                Contracts ({filteredAndSortedContracts.length})
              </h2>
              <button
                onClick={() => navigate(`/contracts/new?propertyId=${id}`)}
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
                  No contracts for this property
                </p>
                <button
                  onClick={() => navigate(`/contracts/new?propertyId=${id}`)}
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
                      placeholder="Search by contract #, contact, type..."
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
                          onClick={() => handleContractsSort('contact')}
                        >
                          <div className="flex items-center gap-1">
                            Contact
                            {contractsSortField === 'contact' &&
                              (contractsSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
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
                            colSpan={7}
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
                                {contract.primaryContact.firstName}{' '}
                                {contract.primaryContact.lastName}
                              </div>
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
                                {(contract.effectiveEndDate ?? contract.endDate)
                                  ? formatDate(
                                      (contract.effectiveEndDate ??
                                        contract.endDate) as string
                                    )
                                  : '-'}
                              </div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm font-medium text-text-primary">
                                {contract.rentAmountCurrency}{' '}
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

            {/* Self-Occupancy Periods Section */}
            <div className="mt-8 pt-6 border-t border-border-default">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Home className="h-5 w-5 text-info-text" />
                  <h2 className="text-lg font-semibold text-text-primary">
                    Self-Occupancy Periods
                  </h2>
                  {occupancyPeriods.length > 0 && (
                    <span className="text-sm text-text-secondary">
                      ({occupancyPeriods.length})
                    </span>
                  )}
                </div>
                {canEditData && (
                  <button
                    onClick={() => setShowSelfOccupancyModal(true)}
                    className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 text-sm"
                  >
                    <Plus className="h-4 w-4" />
                    Add Self-Occupancy
                  </button>
                )}
              </div>

              {occupancyPeriods.length === 0 ? (
                <div className="text-center py-8 border border-dashed border-border-default rounded-lg">
                  <Home className="h-10 w-10 text-text-disabled mx-auto mb-3" />
                  <p className="text-text-secondary text-sm mb-3">
                    No self-occupancy periods recorded
                  </p>
                  {canEditData && (
                    <button
                      onClick={() => setShowSelfOccupancyModal(true)}
                      className="text-primary-500 hover:underline text-sm font-medium inline-flex items-center gap-1"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Record a self-occupancy period
                    </button>
                  )}
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-border-default">
                    <thead className="bg-surface-page">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Period
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Type
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Occupant
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Status
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-surface-card divide-y divide-border-default">
                      {occupancyPeriods.map((period) => {
                        const isPeriodActive =
                          !period.endDate ||
                          new Date(period.endDate) >= new Date();
                        return (
                          <tr
                            key={period.identifier}
                            className="hover:bg-surface-page transition-colors"
                          >
                            <td className="px-4 py-3 text-sm text-text-primary">
                              {formatDate(period.startDate)} &mdash;{' '}
                              {period.endDate
                                ? formatDate(period.endDate)
                                : 'Ongoing'}
                            </td>
                            <td className="px-4 py-3 text-sm text-text-secondary">
                              {OCCUPANCY_TYPE_LABELS[period.type]}
                            </td>
                            <td className="px-4 py-3 text-sm text-text-secondary">
                              {period.occupantName ?? (
                                <span className="text-text-muted">—</span>
                              )}
                            </td>
                            <td className="px-4 py-3">
                              {isPeriodActive ? (
                                <span className="inline-flex items-center gap-1.5 text-xs font-medium text-info-text">
                                  <span className="w-1.5 h-1.5 rounded-full bg-primary-500 animate-pulse" />
                                  Active
                                </span>
                              ) : (
                                <span className="text-xs font-medium text-text-secondary">
                                  Ended
                                </span>
                              )}
                            </td>
                            <td className="px-4 py-3 text-right">
                              <div className="flex items-center justify-end gap-1">
                                <button
                                  onClick={() =>
                                    setEditOccupancyPeriodId(period.identifier)
                                  }
                                  title="Edit"
                                  className="p-1.5 rounded-lg hover:bg-surface-inset text-text-secondary hover:text-primary-500 transition-colors"
                                >
                                  <Edit className="h-4 w-4" />
                                </button>
                                {isPeriodActive && canEditData && (
                                  <button
                                    onClick={() => {
                                      setEndOccupancyPeriodId(
                                        period.identifier
                                      );
                                      setShowEndOccupancyModal(true);
                                    }}
                                    title="End occupancy"
                                    className="p-1.5 rounded-lg hover:bg-warning-bg text-text-secondary hover:text-warning-text transition-colors"
                                  >
                                    <Square className="h-4 w-4" />
                                  </button>
                                )}
                                {canManageMembers && (
                                  <button
                                    onClick={() =>
                                      setDeleteOccupancyPeriodId(
                                        period.identifier
                                      )
                                    }
                                    title="Delete"
                                    className="p-1.5 rounded-lg hover:bg-error-bg text-text-secondary hover:text-error-text transition-colors"
                                  >
                                    <Trash2 className="h-4 w-4" />
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'expenses' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-text-primary">
                Expenses ({filteredAndSortedExpenses.length})
              </h2>
              <button
                onClick={() => navigate(`/expenses/new?propertyId=${id}`)}
                disabled={!canEditData}
                className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
              >
                <Plus className="h-4 w-4" />
                Add Expense
              </button>
            </div>

            {expensesLoading ? (
              <LoadingSpinner />
            ) : expensesError ? (
              <ErrorMessage message="Failed to load expenses" />
            ) : expenses.length === 0 ? (
              <div className="text-center py-12">
                <Receipt className="h-12 w-12 text-text-disabled mx-auto mb-3" />
                <p className="text-text-secondary mb-4">
                  No expenses for this property
                </p>
                <button
                  onClick={() => navigate(`/expenses/new?propertyId=${id}`)}
                  disabled={!canEditData}
                  className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
                >
                  <Plus className="h-4 w-4" />
                  Create First Expense
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
                      placeholder="Search by expense #, description, category..."
                      value={expensesSearchTerm}
                      onChange={(e) => {
                        setExpensesSearchTerm(e.target.value);
                        setExpensesCurrentPage(1);
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
                        <th className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider">
                          Expense #
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handleExpensesSort('expenseDate')}
                        >
                          <div className="flex items-center gap-1">
                            Date
                            {expensesSortField === 'expenseDate' &&
                              (expensesSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handleExpensesSort('description')}
                        >
                          <div className="flex items-center gap-1">
                            Description
                            {expensesSortField === 'description' &&
                              (expensesSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handleExpensesSort('category')}
                        >
                          <div className="flex items-center gap-1">
                            Category
                            {expensesSortField === 'category' &&
                              (expensesSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wider cursor-pointer hover:bg-surface-inset"
                          onClick={() => handleExpensesSort('amount')}
                        >
                          <div className="flex items-center gap-1">
                            Amount
                            {expensesSortField === 'amount' &&
                              (expensesSortOrder === 'asc' ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              ))}
                          </div>
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-surface-card divide-y divide-border-default">
                      {paginatedExpenses.length === 0 ? (
                        <tr>
                          <td
                            colSpan={5}
                            className="px-6 py-12 text-center text-text-secondary"
                          >
                            No expenses found matching your search
                          </td>
                        </tr>
                      ) : (
                        paginatedExpenses.map((expense) => (
                          <tr
                            key={expense.identifier}
                            className="hover:bg-primary-50 cursor-pointer"
                            onClick={() =>
                              navigate(`/expenses/${expense.identifier}`)
                            }
                          >
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-500 dark:text-primary-300">
                              #{expense.identifier}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-text-primary">
                              {formatDate(expense.expenseDate)}
                            </td>
                            <td className="px-6 py-4 text-sm text-text-primary">
                              {expense.description}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <ExpenseCategoryBadge
                                category={expense.category}
                              />
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-text-primary">
                              {expense.currency} {expense.amount.toFixed(2)}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                {expensesTotalPages > 1 && (
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-border-default">
                    <div className="text-sm text-text-secondary">
                      Showing {(expensesCurrentPage - 1) * expensesPerPage + 1}{' '}
                      to{' '}
                      {Math.min(
                        expensesCurrentPage * expensesPerPage,
                        filteredAndSortedExpenses.length
                      )}{' '}
                      of {filteredAndSortedExpenses.length} expenses
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() =>
                          setExpensesCurrentPage(expensesCurrentPage - 1)
                        }
                        disabled={expensesCurrentPage === 1}
                        className="px-3 py-1 border border-border-strong rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-inset"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-text-secondary">
                        Page {expensesCurrentPage} of {expensesTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setExpensesCurrentPage(expensesCurrentPage + 1)
                        }
                        disabled={expensesCurrentPage === expensesTotalPages}
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

        {activeTab === 'audit' && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
            <h2 className="text-xl font-semibold text-text-primary mb-4">
              Property History
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
                        className={`flex items-start gap-4 p-4 transition-colors cursor-pointer ${
                          hasChanges ? 'hover:bg-surface-inset' : ''
                        }`}
                        onClick={() =>
                          hasChanges && toggleAuditItem(activityKey)
                        }
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

                      {/* Expanded Details */}
                      {isExpanded && hasChanges && (
                        <div className="bg-surface-page px-4 py-3 border-t border-border-default">
                          <h4 className="text-xs font-semibold text-text-secondary mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields ?? {}).map(
                              ([field, value]) => {
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
                <p className="text-text-secondary">No history available</p>
                <p className="text-sm text-text-muted mt-1">
                  Changes to this property will appear here
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showSelfOccupancyModal && id && (
        <SelfOccupancyModal
          propertyIdentifier={id}
          onClose={() => setShowSelfOccupancyModal(false)}
        />
      )}

      {showEndOccupancyModal && id && endOccupancyPeriodId && (
        <EndSelfOccupancyModal
          propertyIdentifier={id}
          periodIdentifier={endOccupancyPeriodId}
          onClose={() => {
            setShowEndOccupancyModal(false);
            setEndOccupancyPeriodId(null);
          }}
        />
      )}

      {editOccupancyPeriodId &&
        id &&
        (() => {
          const editPeriod = occupancyPeriods.find(
            (p) => p.identifier === editOccupancyPeriodId
          );
          return editPeriod ? (
            <EditSelfOccupancyModal
              propertyIdentifier={id}
              period={editPeriod}
              onClose={() => setEditOccupancyPeriodId(null)}
            />
          ) : null;
        })()}

      {editFinancingId &&
        id &&
        (() => {
          const editFinancing = financings.find(
            (f) => f.identifier === editFinancingId
          );
          return editFinancing ? (
            <FinancingFormModal
              propertyId={id}
              existing={editFinancing}
              onClose={() => setEditFinancingId(null)}
            />
          ) : null;
        })()}

      {deleteOccupancyPeriodId && id && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-text-primary mb-4">
              Delete Self-Occupancy Period
            </h3>
            <p className="text-text-secondary mb-2">
              Are you sure you want to delete this self-occupancy period?
            </p>
            <p className="text-sm text-error-text mb-6">
              This action cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <Button
                variant="secondary"
                onClick={() => setDeleteOccupancyPeriodId(null)}
                disabled={deleteOccupancyMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                leftIcon={<Trash2 />}
                onClick={() => {
                  deleteOccupancyMutation.mutate(deleteOccupancyPeriodId, {
                    onSuccess: () => setDeleteOccupancyPeriodId(null),
                  });
                }}
                isLoading={deleteOccupancyMutation.isPending}
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {isNlProperty && (
        <WwsCalculatorModal
          propertyIdentifier={id}
          isOpen={showWwsModal}
          onClose={() => setShowWwsModal(false)}
        />
      )}

      {showDeleteWwsConfirm && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg p-6 max-w-sm w-full mx-4">
            <h3 className="text-base font-semibold text-text-primary mb-2">
              Delete WWS calculation?
            </h3>
            <p className="text-sm text-text-secondary mb-5">
              This will permanently remove the saved calculation from this
              property.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="ghost"
                onClick={() => setShowDeleteWwsConfirm(false)}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => {
                  const calcId = latestWws?.identifier;
                  if (calcId) {
                    deleteWwsMutation.mutate(calcId, {
                      onSuccess: () => setShowDeleteWwsConfirm(false),
                    });
                  }
                }}
                isLoading={deleteWwsMutation.isPending}
                className="bg-error-text hover:bg-error-text/90"
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {deleteWwsHistoryId && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg p-6 max-w-sm w-full mx-4">
            <h3 className="text-base font-semibold text-text-primary mb-2">
              Delete WWS calculation?
            </h3>
            <p className="text-sm text-text-secondary mb-5">
              This will permanently remove this historic calculation.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="ghost"
                onClick={() => setDeleteWwsHistoryId(null)}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => {
                  deleteWwsMutation.mutate(deleteWwsHistoryId, {
                    onSuccess: () => setDeleteWwsHistoryId(null),
                  });
                }}
                isLoading={deleteWwsMutation.isPending}
                className="bg-error-text hover:bg-error-text/90"
              >
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-surface-card rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-text-primary mb-4">
              Delete Property
            </h3>
            <p className="text-text-secondary mb-2">
              Are you sure you want to delete this property?
            </p>
            <p className="text-sm text-error-text mb-6">
              This action cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <Button
                variant="secondary"
                onClick={() => setShowDeleteModal(false)}
                disabled={deletePropertyMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                leftIcon={<Trash2 />}
                onClick={handleDelete}
                isLoading={deletePropertyMutation.isPending}
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
