import { useState, useMemo } from 'react';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { useParams, useNavigate } from 'react-router-dom';
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
import { CalendarFeedType } from '@/types/calendarFeed';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { useExpensesByProperty } from '@/hooks/useExpenseHooks';
import { ExpenseCategoryBadge } from '@/components/expenses/ExpenseCategoryBadge';
import { PropertyStatus } from '@/types/property';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { DocumentList } from '@/components/properties/DocumentList';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { PropertyMap } from '@/components/properties/PropertyMap';
import { ContractStatusBadge } from '@/components/contracts/ContractStatusBadge';
import { Button, PageHeader } from '@/components/ui';
import { useTeam } from '@/context/TeamContext';
import client from '@/api/client';
import {
  Edit,
  Trash2,
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
} from 'lucide-react';
import DOMPurify from 'dompurify';
import { formatDistanceToNow } from 'date-fns';
import { useFormatDate } from '@/hooks/useFormatDate';

const statusColors: Record<PropertyStatus, string> = {
  VACANT:
    'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300',
  OCCUPIED:
    'bg-primary-100 dark:bg-primary-500/10 text-blue-800 dark:text-blue-300',
  MAINTENANCE:
    'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300',
  UNAVAILABLE:
    'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db]',
};

const statusLabels: Record<PropertyStatus, string> = {
  VACANT: 'Vacant',
  OCCUPIED: 'Occupied',
  MAINTENANCE: 'Maintenance',
  UNAVAILABLE: 'Unavailable',
};

export const PropertyDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [activeTab, setActiveTab] = useState<
    'info' | 'photos' | 'documents' | 'contracts' | 'expenses' | 'audit'
  >('info');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [expandedAuditItems, setExpandedAuditItems] = useState<Set<string>>(
    new Set()
  );
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);

  // Contracts table state
  const [contractsSearchTerm, setContractsSearchTerm] = useState('');
  const [contractsSortField, setContractsSortField] = useState<
    'startDate' | 'rentAmount' | 'status' | 'tenant' | 'contractType'
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
  const deletePropertyMutation = useDeleteProperty();
  const uploadDocumentMutation = useUploadPropertyDocument(id!);
  const uploadPhotoMutation = useUploadPropertyPhoto(id!);
  const setMainPhotoMutation = useSetMainPhoto(id!);
  const deleteDocumentMutation = useDeleteDocument(id!);
  const deletePhotoMutation = useDeletePhoto();

  // Contracts filtering, sorting, and pagination
  const filteredAndSortedContracts = useMemo(() => {
    if (!contracts) return [];

    let filtered = [...contracts];

    // Apply search filter
    if (contractsSearchTerm) {
      const search = contractsSearchTerm.toLowerCase();
      filtered = filtered.filter(
        (contract) =>
          contract.identifier.toLowerCase().includes(search) ||
          `${contract.primaryTenant.firstName} ${contract.primaryTenant.lastName}`
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
        case 'tenant':
          aVal = `${a.primaryTenant.firstName} ${a.primaryTenant.lastName}`;
          bVal = `${b.primaryTenant.firstName} ${b.primaryTenant.lastName}`;
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
    if (!expenses) return [];

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

      if (aVal < bVal) return expensesSortOrder === 'asc' ? -1 : 1;
      if (aVal > bVal) return expensesSortOrder === 'asc' ? 1 : -1;
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
    if (!id) return;
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
    if (field === 'documentAdded') return 'Document Added';
    if (field === 'documentRemoved') return 'Document Removed';
    if (field === 'documentCount') return 'Document Count';
    if (field === 'photoAdded') return 'Photo Added';
    if (field === 'photoRemoved') return 'Photo Removed';
    if (field === 'photoCount') return 'Photo Count';
    if (field === 'photoEdited') return 'Photo Edited';
    if (field === 'documentEdited') return 'Document Edited';

    // Convert camelCase to Title Case with spaces
    return field
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  const formatFieldValue = (value: unknown): string => {
    if (value === null || value === undefined) return 'N/A';
    if (typeof value === 'boolean') return value ? 'Yes' : 'No';
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  };

  const formatEnumValue = (value: string | null): string => {
    if (!value) return '';
    return value
      .replace(/_/g, ' ')
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
              className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusColors[property.status as PropertyStatus]}`}
            >
              {statusLabels[property.status as PropertyStatus]}
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
                  ? 'border-[#5c7cfa] text-[#5c7cfa] font-semibold'
                  : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              Info
            </button>
            <button
              onClick={() => setActiveTab('photos')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'photos'
                  ? 'border-[#5c7cfa] text-[#5c7cfa] font-semibold'
                  : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <Image className="h-4 w-4" />
              Photos {photos.length > 0 && `(${photos.length})`}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`px-4 py-2 border-b-2 transition-colors ${
                activeTab === 'documents'
                  ? 'border-[#5c7cfa] text-[#5c7cfa] font-semibold'
                  : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
            <button
              onClick={() => setActiveTab('contracts')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'contracts'
                  ? 'border-[#5c7cfa] text-[#5c7cfa] font-semibold'
                  : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <FileText className="h-4 w-4" />
              Contracts {contracts.length > 0 && `(${contracts.length})`}
            </button>
            <button
              onClick={() => setActiveTab('expenses')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'expenses'
                  ? 'border-[#5c7cfa] text-[#5c7cfa] font-semibold'
                  : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
              }`}
            >
              <Receipt className="h-4 w-4" />
              Expenses {expenses.length > 0 && `(${expenses.length})`}
            </button>
            <button
              onClick={() => setActiveTab('audit')}
              className={`px-4 py-2 border-b-2 transition-colors flex items-center gap-2 ${
                activeTab === 'audit'
                  ? 'border-[#5c7cfa] text-[#5c7cfa] font-semibold'
                  : 'border-transparent text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:text-[#eef0f6] dark:hover:text-[#c4c8db]'
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
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6 space-y-6">
              {/* Status Badge */}
              <div>
                <span
                  className={`px-4 py-2 rounded-full text-sm font-semibold ${statusColors[property.status as PropertyStatus]}`}
                >
                  {statusLabels[property.status as PropertyStatus]}
                </span>
              </div>

              {/* Specifications Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {property.bedrooms !== null && (
                  <div>
                    <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                      <Bed className="h-5 w-5" />
                      <span className="text-sm font-medium">Bedrooms</span>
                    </div>
                    <p className="text-2xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      {property.bedrooms}
                    </p>
                  </div>
                )}

                {property.bathrooms !== null && (
                  <div>
                    <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                      <Bath className="h-5 w-5" />
                      <span className="text-sm font-medium">Bathrooms</span>
                    </div>
                    <p className="text-2xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      {property.bathrooms}
                    </p>
                  </div>
                )}

                {property.areaValue !== null && (
                  <div>
                    <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                      <Ruler className="h-5 w-5" />
                      <span className="text-sm font-medium">Area</span>
                    </div>
                    <p className="text-2xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      {property.areaValue}
                      {property.areaUnit === 'sqft' ? 'ft²' : 'm²'}
                    </p>
                  </div>
                )}

                <div>
                  <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Type</span>
                  </div>
                  <p className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    {property.propertyType.replace('_', ' ')}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Street</span>
                  </div>
                  <p className="text-lg text-[#1a1d2e] dark:text-[#eef0f6]">
                    {property.street}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">City</span>
                  </div>
                  <p className="text-lg text-[#1a1d2e] dark:text-[#eef0f6]">
                    {property.city}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Postal Code</span>
                  </div>
                  <p className="text-lg text-[#1a1d2e] dark:text-[#eef0f6]">
                    {property.postalCode}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-[#6b7194] dark:text-[#8b90a8] mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">Country</span>
                  </div>
                  <p className="text-lg text-[#1a1d2e] dark:text-[#eef0f6]">
                    {property.country}
                  </p>
                </div>
              </div>

              {/* Map */}
              <div className="pt-6 border-t">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-3">
                  Location
                </h3>
                <PropertyMap
                  street={property.street}
                  city={property.city}
                  latitude={property.latitude}
                  longitude={property.longitude}
                />
              </div>
            </div>

            {/* Construction & Structure */}
            {(property.yearBuilt !== null ||
              property.yearLastRenovated !== null ||
              property.constructionType ||
              property.foundationType ||
              property.roofType ||
              property.wallConstruction ||
              property.flooringType ||
              property.windowType ||
              property.numberOfFloors !== null ||
              property.structuralNotes) && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Construction &amp; Structure
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.yearBuilt !== null && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Year Built
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {property.yearBuilt}
                      </div>
                    </div>
                  )}
                  {property.yearLastRenovated !== null && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Last Renovated
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {property.yearLastRenovated}
                      </div>
                    </div>
                  )}
                  {property.constructionType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Construction
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.constructionType)}
                      </div>
                    </div>
                  )}
                  {property.foundationType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Foundation
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.foundationType)}
                      </div>
                    </div>
                  )}
                  {property.roofType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Roof
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.roofType)}
                      </div>
                    </div>
                  )}
                  {property.wallConstruction && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Wall Construction
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.wallConstruction)}
                      </div>
                    </div>
                  )}
                  {property.flooringType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Flooring
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.flooringType)}
                      </div>
                    </div>
                  )}
                  {property.windowType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Windows
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.windowType)}
                      </div>
                    </div>
                  )}
                  {property.numberOfFloors !== null && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Floors
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {property.numberOfFloors}
                      </div>
                    </div>
                  )}
                </div>
                {property.structuralNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                      Structural Notes
                    </div>
                    <div
                      className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] mt-1 prose prose-sm dark:prose-invert max-w-none"
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
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Energy &amp; Climate
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.energyEfficiencyRating && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
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
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Certificate Expiry
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatDate(property.energyCertificateExpiryDate)}
                      </div>
                    </div>
                  )}
                  {property.heatingType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Heating
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.heatingType)}
                      </div>
                    </div>
                  )}
                  {property.coolingType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Cooling
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.coolingType)}
                      </div>
                    </div>
                  )}
                  {property.hotWaterSystem && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Hot Water
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.hotWaterSystem)}
                      </div>
                    </div>
                  )}
                </div>
                {property.insulationNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                      Insulation Notes
                    </div>
                    <div
                      className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] mt-1 prose prose-sm dark:prose-invert max-w-none"
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
              property.electricityCapacityAmps !== null ||
              property.waterConnectionType ||
              property.sewageType ||
              property.internetConnectionType ||
              property.internetMaxSpeedMbps !== null ||
              property.internetStatus) && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Utilities &amp; Connections
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.electricityConnectionType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Electricity
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.electricityConnectionType)}
                      </div>
                    </div>
                  )}
                  {property.electricityCapacityAmps !== null && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Capacity
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {property.electricityCapacityAmps} A
                      </div>
                    </div>
                  )}
                  {property.waterConnectionType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Water
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.waterConnectionType)}
                      </div>
                    </div>
                  )}
                  <div>
                    <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                      Gas Connection
                    </div>
                    <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                      {property.hasGasConnection ? 'Yes' : 'No'}
                    </div>
                  </div>
                  {property.sewageType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Sewage
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.sewageType)}
                      </div>
                    </div>
                  )}
                  {property.internetConnectionType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Internet
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.internetConnectionType)}
                      </div>
                    </div>
                  )}
                  {property.internetMaxSpeedMbps !== null && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Internet Speed
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {property.internetMaxSpeedMbps} Mbps
                      </div>
                    </div>
                  )}
                  {property.internetStatus && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Internet Status
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.internetStatus)}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Parking */}
            {(property.parkingSpaces !== null || property.parkingType) && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Parking
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {property.parkingSpaces !== null && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Parking Spaces
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {property.parkingSpaces}
                      </div>
                    </div>
                  )}
                  {property.parkingType && (
                    <div>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                        Parking Type
                      </div>
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] mt-1">
                        {formatEnumValue(property.parkingType)}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Outdoor Areas */}
            {property.outdoorAreas && property.outdoorAreas.length > 0 && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Outdoor Areas
                </h3>
                <div className="flex flex-wrap gap-2">
                  {property.outdoorAreas.map((area) => (
                    <span
                      key={area.identifier}
                      className="inline-flex items-center px-3 py-1.5 rounded-full text-sm font-medium bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]"
                    >
                      {formatEnumValue(area.type)}
                      {area.areaValue !== null
                        ? ` - ${area.areaValue} m\u00B2`
                        : ''}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Amenities */}
            {property.amenities && property.amenities.length > 0 && (
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Amenities
                </h3>
                <div className="space-y-4">
                  {Object.entries(
                    property.amenities.reduce<
                      Record<string, typeof property.amenities>
                    >((groups, amenity) => {
                      const cat = amenity.amenityCategory;
                      if (!groups[cat]) groups[cat] = [];
                      groups[cat]!.push(amenity);
                      return groups;
                    }, {})
                  ).map(([category, items]) => (
                    <div key={category}>
                      <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide mb-2">
                        {formatEnumValue(category)}
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {items!.map((amenity) => (
                          <span
                            key={amenity.amenityIdentifier}
                            className="inline-flex items-center px-3 py-1.5 rounded-full text-sm font-medium bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]"
                          >
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
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Safety &amp; Security
                </h3>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                  {property.hasSmokeDetectors && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Smoke Detectors
                    </span>
                  )}
                  {property.hasCoDetectors && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      CO Detectors
                    </span>
                  )}
                  {property.hasFireExtinguisher && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Fire Extinguisher
                    </span>
                  )}
                  {property.hasSprinklerSystem && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Sprinkler System
                    </span>
                  )}
                  {property.hasAlarmSystem && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Alarm System
                    </span>
                  )}
                  {property.hasSecurityCameras && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Security Cameras
                    </span>
                  )}
                  {property.hasSecureEntry && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Secure Entry
                    </span>
                  )}
                </div>
                {property.safetyNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                      Safety Notes
                    </div>
                    <div
                      className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] mt-1 prose prose-sm dark:prose-invert max-w-none"
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
              <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] uppercase tracking-wide mb-4">
                  Accessibility
                </h3>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                  {property.isWheelchairAccessible && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Wheelchair Accessible
                    </span>
                  )}
                  {property.hasElevator && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Elevator
                    </span>
                  )}
                  {property.hasStepFreeEntrance && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Step-Free Entrance
                    </span>
                  )}
                  {property.hasAdaptedBathroom && (
                    <span className="inline-flex items-center gap-1.5 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                      <span className="flex-shrink-0 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 flex items-center justify-center text-xs font-bold">
                        {'\u2713'}
                      </span>
                      Adapted Bathroom
                    </span>
                  )}
                </div>
                {property.accessibilityNotes && (
                  <div className="mt-4">
                    <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                      Accessibility Notes
                    </div>
                    <div
                      className="text-sm text-[#1a1d2e] dark:text-[#eef0f6] mt-1 prose prose-sm dark:prose-invert max-w-none"
                      dangerouslySetInnerHTML={{
                        __html: DOMPurify.sanitize(property.accessibilityNotes),
                      }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Metadata */}
            <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
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
                      {formatDate(property.createdAt)} at{' '}
                      {new Date(property.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-[#6b7194] dark:text-[#8b90a8]">
                      Last Updated:
                    </span>{' '}
                    <span className="text-[#1a1d2e] dark:text-[#eef0f6]">
                      {formatDate(property.updatedAt)} at{' '}
                      {new Date(property.updatedAt).toLocaleTimeString()}
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

        {activeTab === 'contracts' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Contracts ({filteredAndSortedContracts.length})
              </h2>
              <button
                onClick={() => navigate(`/contracts/new?propertyId=${id}`)}
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
                <FileText className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8] mb-4">
                  No contracts for this property
                </p>
                <button
                  onClick={() => navigate(`/contracts/new?propertyId=${id}`)}
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
                      placeholder="Search by contract #, tenant, type..."
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
                  <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
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
                          onClick={() => handleContractsSort('tenant')}
                        >
                          <div className="flex items-center gap-1">
                            Tenant
                            {contractsSortField === 'tenant' &&
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
                    <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
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
                                {contract.primaryTenant.firstName}{' '}
                                {contract.primaryTenant.lastName}
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
                                {contract.currency}{' '}
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

        {activeTab === 'expenses' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Expenses ({filteredAndSortedExpenses.length})
              </h2>
              <button
                onClick={() => navigate(`/expenses/new?propertyId=${id}`)}
                disabled={!canEditData}
                className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
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
                <Receipt className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] dark:text-[#6b7194] dark:text-[#8b90a8] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8] mb-4">
                  No expenses for this property
                </p>
                <button
                  onClick={() => navigate(`/expenses/new?propertyId=${id}`)}
                  disabled={!canEditData}
                  className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-[#5c7cfa]"
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
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                    <input
                      type="text"
                      placeholder="Search by expense #, description, category..."
                      value={expensesSearchTerm}
                      onChange={(e) => {
                        setExpensesSearchTerm(e.target.value);
                        setExpensesCurrentPage(1);
                      }}
                      className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
                    <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                          Expense #
                        </th>
                        <th
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                          className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                    <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
                      {paginatedExpenses.length === 0 ? (
                        <tr>
                          <td
                            colSpan={5}
                            className="px-6 py-12 text-center text-[#6b7194] dark:text-[#8b90a8]"
                          >
                            No expenses found matching your search
                          </td>
                        </tr>
                      ) : (
                        paginatedExpenses.map((expense) => (
                          <tr
                            key={expense.identifier}
                            className="hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer"
                            onClick={() =>
                              navigate(`/expenses/${expense.identifier}`)
                            }
                          >
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-500 dark:text-primary-300">
                              #{expense.identifier}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                              {formatDate(expense.expenseDate)}
                            </td>
                            <td className="px-6 py-4 text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                              {expense.description}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <ExpenseCategoryBadge
                                category={expense.category}
                              />
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
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
                  <div className="flex items-center justify-between mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                    <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
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
                        className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] dark:text-[#c4c8db]"
                      >
                        Previous
                      </button>
                      <span className="px-3 py-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Page {expensesCurrentPage} of {expensesTotalPages}
                      </span>
                      <button
                        onClick={() =>
                          setExpensesCurrentPage(expensesCurrentPage + 1)
                        }
                        disabled={expensesCurrentPage === expensesTotalPages}
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

        {activeTab === 'audit' && (
          <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
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
                      className="border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg overflow-hidden"
                    >
                      <div
                        className={`flex items-start gap-4 p-4 transition-colors cursor-pointer ${
                          hasChanges
                            ? 'hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]'
                            : ''
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
                                ? 'bg-green-100 dark:bg-green-900/30'
                                : activity.action === 'UPDATE'
                                  ? 'bg-primary-100 dark:bg-primary-500/10'
                                  : 'bg-red-100 dark:bg-red-900/30'
                            }
                          `}
                        >
                          <span
                            className={`
                              text-xs font-semibold
                              ${
                                activity.action === 'CREATE'
                                  ? 'text-green-700 dark:text-green-300'
                                  : activity.action === 'UPDATE'
                                    ? 'text-blue-700 dark:text-blue-300'
                                    : 'text-red-700 dark:text-red-300'
                              }
                            `}
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

                      {/* Expanded Details */}
                      {isExpanded && hasChanges && (
                        <div className="bg-[#f8f9fc] dark:bg-[#0c0d14] px-4 py-3 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                          <h4 className="text-xs font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-2 uppercase">
                            Changed Fields
                          </h4>
                          <div className="space-y-2">
                            {Object.entries(activity.changedFields!).map(
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
                                      {formatFieldName(field)}
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
                                            {formatFieldValue(
                                              activity.oldValues?.[field]
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
                                            {formatFieldValue(
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
                <History className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mx-auto mb-3" />
                <p className="text-[#6b7194] dark:text-[#8b90a8]">
                  No history available
                </p>
                <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] mt-1">
                  Changes to this property will appear here
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-white dark:bg-[#14161f] rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
              Delete Property
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8] mb-2">
              Are you sure you want to delete this property?
            </p>
            <p className="text-sm text-red-600 mb-6">
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
