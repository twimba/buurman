import { useState, useMemo, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTabState } from '@/hooks/useTabState';
import {
  useContact,
  useDeleteContact,
  useEraseContactData,
  useContactDocuments,
  useContactPhotos,
  useContactAddresses,
  useContactNotes,
  useUploadContactDocument,
  useUploadContactPhoto,
  useSetContactMainPhoto,
  useDeleteContactDocument,
} from '@/hooks/useContactHooks';
import { useDeletePhoto } from '@/hooks/usePhotoHooks';
import { useContracts } from '@/hooks/useContractHooks';
import { CalendarFeedResponseFeedType as CalendarFeedType } from '@/generated/models';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { LoadingSpinner, RichTextDisplay } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import { DocumentList } from '@/components/properties/DocumentList';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { Avatar } from '@/components/common/Avatar';
import { ContactAddressList } from '@/components/contacts/ContactAddressList';
import { ContactNotesTab } from '@/components/contacts/ContactNotesTab';
import { ContactActivityTab } from '@/components/contacts/ContactActivityTab';
import { ContactRelationshipsTab } from '@/components/contacts/ContactRelationshipsTab';
import { ContactTagsTab } from '@/components/contacts/ContactTagsTab';
import { ContactFinancialsTab } from '@/components/contacts/ContactFinancialsTab';
import { ContactContractsTable } from '@/components/contacts/ContactContractsTable';
import {
  ContractStatus,
  ContractPartyRole,
  PARTY_ROLE_LABELS,
} from '@/types/contract';
import {
  Button,
  ConfirmDialog,
  ModalWrapper,
  PageHeader,
  StatusBadge,
} from '@buurman/ui';
import { useTeam } from '@/context/TeamContext';
import {
  Edit,
  Trash2,
  Mail,
  Phone,
  User,
  Home,
  FileText,
  MapPin,
  Plus,
  ChevronUp,
  ChevronDown,
  Download,
  Activity,
  Users,
  FolderOpen,
  Wallet,
  AlertCircle,
  Calendar,
  Building2,
  ShieldAlert,
  CheckCircle2,
  Shield,
} from 'lucide-react';
import client from '@/api/client';
import { useFormatDate } from '@/hooks/useFormatDate';

const ROLE_COLORS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]: 'bg-info-bg text-info-text',
  [ContractPartyRole.GUARANTOR]: 'bg-warning-bg text-warning-text',
  [ContractPartyRole.COSIGNER]: 'bg-info-bg text-info-text',
  [ContractPartyRole.EXTRA_TENANT]: 'bg-success-bg text-success-text',
};

const CONTACT_TYPE_LABELS: Record<string, string> = {
  INDIVIDUAL: 'Individual',
  COMPANY: 'Company',
  SERVICE_PROVIDER: 'Service Provider',
};

const formatCurrency = (amount: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);

const RoleBadge = ({ role }: { role: ContractPartyRole }) => (
  <span
    className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_COLORS[role] ?? 'bg-surface-inset text-text-primary'}`}
  >
    {PARTY_ROLE_LABELS[role] ?? role}
  </span>
);

const TAB_IDS = [
  'overview',
  'notes',
  'activity',
  'financials',
  'relationships',
  'files',
  'addresses',
] as const;

export const ContactDetailPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData, activeTeam } = useTeam();
  const isAdmin = activeTeam?.role === 'TEAM_ADMIN';
  const [activeTab, setActiveTab] = useTabState('overview', TAB_IDS);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showEraseModal, setShowEraseModal] = useState(false);
  const [eraseConfirmText, setEraseConfirmText] = useState('');
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);

  const { formatDate } = useFormatDate();

  const { data: contact, isLoading, error } = useContact(id);
  const contactIdentifier = contact?.identifier;

  useEffect(() => {
    if (contactIdentifier) {
      trackEvent(AnalyticsEvent.CONTACT_VIEWED);
    }
  }, [contactIdentifier]);

  const {
    data: documents = [],
    isLoading: docsLoading,
    error: docsError,
  } = useContactDocuments(id);
  const {
    data: photos = [],
    isLoading: photosLoading,
    error: photosError,
  } = useContactPhotos(id);
  const { data: contractsData, isLoading: contractsLoading } = useContracts(
    id ? { contactIdentifier: id } : undefined
  );
  const contracts = useMemo(
    () => contractsData?.content ?? [],
    [contractsData]
  );
  const { data: addresses = [] } = useContactAddresses(id);
  const { data: notes = [] } = useContactNotes(id);

  const deleteContactMutation = useDeleteContact();
  const eraseContactMutation = useEraseContactData();
  const uploadDocumentMutation = useUploadContactDocument(id);
  const uploadPhotoMutation = useUploadContactPhoto(id);
  const setMainPhotoMutation = useSetContactMainPhoto(id);
  const deleteDocumentMutation = useDeleteContactDocument(id);
  const deletePhotoMutation = useDeletePhoto();

  const handleDelete = async () => {
    if (!id) {
      return;
    }
    try {
      await deleteContactMutation.mutateAsync(id);
      navigate('/contacts');
    } catch {
      // Mutation error handled by React Query onError
    }
  };

  const handleErase = async () => {
    if (!id || eraseConfirmText !== 'ERASE') {
      return;
    }
    try {
      await eraseContactMutation.mutateAsync(id);
      navigate('/contacts');
    } catch {
      // Mutation error handled by React Query onError
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

  // Active contract properties for Overview tab
  const activeContractProperties = contracts
    ? contracts
        .filter((contract) => contract.status === ContractStatus.ACTIVE)
        .map((contract) => {
          const party = contract.parties?.find(
            (p) => p.contact.identifier === id
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

  // Expiring contracts (within 30 days)
  const expiringContracts = useMemo(() => {
    const now = new Date();
    const thirtyDaysFromNow = new Date(
      now.getTime() + 30 * 24 * 60 * 60 * 1000
    );
    return contracts.filter((c) => {
      if (c.status !== ContractStatus.ACTIVE) {
        return false;
      }
      const endDate = c.effectiveEndDate ?? c.endDate;
      if (!endDate) {
        return false;
      }
      const end = new Date(endDate);
      return end >= now && end <= thirtyDaysFromNow;
    });
  }, [contracts]);

  // Upcoming follow-ups from notes
  const overdueFollowUps = useMemo(() => {
    const today = new Date().toISOString().split('T')[0];
    return notes.filter((n) => n.followUpDate && n.followUpDate < today);
  }, [notes]);

  const upcomingFollowUps = useMemo(() => {
    const today = new Date().toISOString().split('T')[0];
    return notes
      .filter((n) => n.followUpDate && n.followUpDate >= today)
      .sort((a, b) =>
        (a.followUpDate ?? '') < (b.followUpDate ?? '') ? -1 : 1
      )
      .slice(0, 5);
  }, [notes]);

  // Missing fields alerts
  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (!contact?.email) {
      missing.push('email');
    }
    if (!contact?.phone) {
      missing.push('phone');
    }
    return missing;
  }, [contact]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !contact) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load contact" />
      </div>
    );
  }

  const displayName =
    contact.displayName ??
    `${contact.firstName} ${contact.lastName ?? ''}`.trim();

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <PageHeader
          title={displayName}
          subtitle={`#${contact.identifier} · ${
            contact.contactType
              ? (CONTACT_TYPE_LABELS[contact.contactType] ??
                contact.contactType)
              : ''
          }`}
          backTo="/contacts"
          avatar={
            <Avatar
              firstName={contact.firstName}
              lastName={contact.lastName}
              photoUrl={contact.mainPhotoThumbnailUrl ?? contact.mainPhotoUrl}
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
                      `/booklets/contact/${id}`,
                      { responseType: 'blob' }
                    );
                    const blob = new Blob([response.data], {
                      type: 'application/pdf',
                    });
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'contact-booklet.pdf';
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                  } catch {
                    // Download error handled by browser
                  }
                }}
              >
                Booklet
              </Button>
              {id && (
                <CalendarFeedButton
                  feedType={CalendarFeedType.CONTACT_PAYMENTS}
                  entityIdentifier={id}
                />
              )}
              <Button
                variant="secondary"
                leftIcon={<Edit />}
                onClick={() => navigate(`/contacts/${id}/edit`)}
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
              {isAdmin && contact.dataRetentionStatus !== 'ANONYMIZED' && (
                <Button
                  variant="danger"
                  leftIcon={<ShieldAlert />}
                  onClick={() => setShowEraseModal(true)}
                >
                  Erase Data
                </Button>
              )}
            </>
          }
        />

        {/* Tabs */}
        <div className="border-b border-border-default mb-6 overflow-x-auto">
          <div
            className="flex gap-6 min-w-max"
            role="tablist"
            aria-label="Contact detail tabs"
          >
            <button
              role="tab"
              aria-selected={activeTab === 'overview'}
              aria-controls="tabpanel-overview"
              tabIndex={activeTab === 'overview' ? 0 : -1}
              onClick={() => setActiveTab('overview')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'overview'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <User className="h-4 w-4" />
              Overview
            </button>
            <button
              role="tab"
              aria-selected={activeTab === 'notes'}
              aria-controls="tabpanel-notes"
              tabIndex={activeTab === 'notes' ? 0 : -1}
              onClick={() => setActiveTab('notes')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'notes'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <FileText className="h-4 w-4" />
              Notes {notes.length > 0 && `(${notes.length})`}
            </button>
            <button
              role="tab"
              aria-selected={activeTab === 'activity'}
              aria-controls="tabpanel-activity"
              tabIndex={activeTab === 'activity' ? 0 : -1}
              onClick={() => setActiveTab('activity')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'activity'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Activity className="h-4 w-4" />
              Activity
            </button>
            <button
              role="tab"
              aria-selected={activeTab === 'financials'}
              aria-controls="tabpanel-financials"
              tabIndex={activeTab === 'financials' ? 0 : -1}
              onClick={() => setActiveTab('financials')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'financials'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Wallet className="h-4 w-4" />
              Financials
            </button>
            <button
              role="tab"
              aria-selected={activeTab === 'relationships'}
              aria-controls="tabpanel-relationships"
              tabIndex={activeTab === 'relationships' ? 0 : -1}
              onClick={() => setActiveTab('relationships')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'relationships'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Users className="h-4 w-4" />
              Relationships
            </button>
            <button
              role="tab"
              aria-selected={activeTab === 'files'}
              aria-controls="tabpanel-files"
              tabIndex={activeTab === 'files' ? 0 : -1}
              onClick={() => setActiveTab('files')}
              className={`pb-3 px-1 font-medium transition-colors flex items-center gap-2 ${
                activeTab === 'files'
                  ? 'border-b-2 border-primary-500 text-primary-500 dark:text-primary-300'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <FolderOpen className="h-4 w-4" />
              Files{' '}
              {documents.length + photos.length > 0 &&
                `(${documents.length + photos.length})`}
            </button>
            <button
              role="tab"
              aria-selected={activeTab === 'addresses'}
              aria-controls="tabpanel-addresses"
              tabIndex={activeTab === 'addresses' ? 0 : -1}
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
          </div>
        </div>

        {/* ===== Tab 1: Overview ===== */}
        {activeTab === 'overview' && (
          <div id="tabpanel-overview" role="tabpanel" className="space-y-6">
            {/* Contextual Alerts */}
            {(contact.balanceSummary?.status === 'OVERDUE' ||
              overdueFollowUps.length > 0 ||
              expiringContracts.length > 0 ||
              missingFields.length > 0) && (
              <div className="space-y-2">
                {contact.balanceSummary?.status === 'OVERDUE' && (
                  <div className="flex items-center gap-2 bg-error-bg text-error-text px-4 py-3 rounded-lg">
                    <Wallet className="h-4 w-4 flex-shrink-0" />
                    <span className="text-sm font-medium">
                      {formatCurrency(
                        contact.balanceSummary.outstandingAmount,
                        contact.balanceSummary.currency
                      )}{' '}
                      overdue ({contact.balanceSummary.outstandingPaymentCount}{' '}
                      payment
                      {contact.balanceSummary.outstandingPaymentCount !== 1
                        ? 's'
                        : ''}
                      )
                    </span>
                  </div>
                )}
                {overdueFollowUps.length > 0 && (
                  <div className="flex items-center gap-2 bg-error-bg text-error-text px-4 py-3 rounded-lg">
                    <AlertCircle className="h-4 w-4 flex-shrink-0" />
                    <span className="text-sm font-medium">
                      {overdueFollowUps.length} overdue follow-up
                      {overdueFollowUps.length > 1 ? 's' : ''}
                    </span>
                  </div>
                )}
                {expiringContracts.length > 0 && (
                  <div className="flex items-center gap-2 bg-warning-bg text-warning-text px-4 py-3 rounded-lg">
                    <AlertCircle className="h-4 w-4 flex-shrink-0" />
                    <span className="text-sm font-medium">
                      {expiringContracts.length} contract
                      {expiringContracts.length > 1 ? 's' : ''} expiring within
                      30 days
                    </span>
                  </div>
                )}
                {missingFields.length > 0 && (
                  <div className="flex items-center gap-2 bg-info-bg text-info-text px-4 py-3 rounded-lg">
                    <AlertCircle className="h-4 w-4 flex-shrink-0" />
                    <span className="text-sm font-medium">
                      Missing: {missingFields.join(', ')}
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Balance Summary */}
            {contact.balanceSummary?.status === 'ALL_PAID' && (
              <div
                className="rounded-lg shadow-sm border border-success-border bg-success-bg/30 p-4 flex items-center justify-between"
                role="status"
                aria-label="All payments are up to date"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-success-bg text-success-text">
                    <CheckCircle2 className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="text-sm text-text-secondary">
                      Payment Status
                    </p>
                    <p className="text-xl font-semibold text-text-primary">
                      All Paid
                    </p>
                  </div>
                </div>
                <StatusBadge label="All paid" color="green" size="sm" />
              </div>
            )}
            {contact.balanceSummary &&
              (contact.balanceSummary.status === 'PENDING' ||
                contact.balanceSummary.status === 'OVERDUE') && (
                <div
                  className={`rounded-lg shadow-sm border p-4 flex items-center justify-between ${
                    contact.balanceSummary.status === 'OVERDUE'
                      ? 'border-error-border bg-error-bg/30'
                      : 'border-warning-border bg-warning-bg/30'
                  }`}
                  role="status"
                  aria-label={`Outstanding balance: ${formatCurrency(contact.balanceSummary.outstandingAmount, contact.balanceSummary.currency)}, ${contact.balanceSummary.outstandingPaymentCount} payment${contact.balanceSummary.outstandingPaymentCount !== 1 ? 's' : ''} ${contact.balanceSummary.status === 'OVERDUE' ? 'overdue' : 'pending'}`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`p-2 rounded-lg ${
                        contact.balanceSummary.status === 'OVERDUE'
                          ? 'bg-error-bg text-error-text'
                          : 'bg-warning-bg text-warning-text'
                      }`}
                    >
                      <Wallet className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-sm text-text-secondary">
                        Outstanding Balance
                      </p>
                      <p className="text-xl font-semibold text-text-primary">
                        {formatCurrency(
                          contact.balanceSummary.outstandingAmount,
                          contact.balanceSummary.currency
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <StatusBadge
                      label={
                        contact.balanceSummary.status === 'OVERDUE'
                          ? 'Overdue'
                          : 'Pending'
                      }
                      color={
                        contact.balanceSummary.status === 'OVERDUE'
                          ? 'red'
                          : 'amber'
                      }
                      size="sm"
                    />
                    <p className="text-xs text-text-muted mt-1">
                      {contact.balanceSummary.outstandingPaymentCount} payment
                      {contact.balanceSummary.outstandingPaymentCount !== 1
                        ? 's'
                        : ''}
                    </p>
                  </div>
                </div>
              )}
            {contact.balanceSummary?.guaranteedAmount != null &&
              contact.balanceSummary.guaranteedAmount > 0 && (
                <div
                  className="rounded-lg shadow-sm border border-info-border bg-info-bg/30 p-4 flex items-center justify-between"
                  role="status"
                  aria-label={`Guarantees ${formatCurrency(contact.balanceSummary.guaranteedAmount, contact.balanceSummary.currency)}`}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-info-bg text-info-text">
                      <Shield className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-sm text-text-secondary">
                        Guaranteed Amount
                      </p>
                      <p className="text-xl font-semibold text-text-primary">
                        {formatCurrency(
                          contact.balanceSummary.guaranteedAmount,
                          contact.balanceSummary.currency
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <StatusBadge label="Guarantor" color="blue" size="sm" />
                    <p className="text-xs text-text-muted mt-1">
                      {contact.balanceSummary.guaranteedPaymentCount ?? 0}{' '}
                      payment
                      {(contact.balanceSummary.guaranteedPaymentCount ?? 0) !==
                      1
                        ? 's'
                        : ''}
                    </p>
                  </div>
                </div>
              )}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Contact Information */}
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Contact Information
                </h2>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <User className="h-5 w-5 text-text-muted" />
                    <div>
                      <p className="text-sm text-text-secondary">Name</p>
                      <p className="font-medium text-text-primary">
                        {contact.firstName} {contact.lastName}
                      </p>
                    </div>
                  </div>
                  {contact.companyName && (
                    <div className="flex items-center gap-3">
                      <Building2 className="h-5 w-5 text-text-muted" />
                      <div>
                        <p className="text-sm text-text-secondary">Company</p>
                        <p className="font-medium text-text-primary">
                          {contact.companyName}
                          {contact.tradeName && (
                            <span className="text-text-secondary ml-1">
                              ({contact.tradeName})
                            </span>
                          )}
                        </p>
                      </div>
                    </div>
                  )}
                  <div className="flex items-center gap-3">
                    <Mail className="h-5 w-5 text-text-muted" />
                    <div>
                      <p className="text-sm text-text-secondary">Email</p>
                      <p className="font-medium text-text-primary">
                        {contact.email ? (
                          <a
                            href={`mailto:${contact.email}`}
                            className="hover:text-primary-500"
                          >
                            {contact.email}
                          </a>
                        ) : (
                          <span className="text-text-muted italic">
                            Not set
                          </span>
                        )}
                      </p>
                    </div>
                  </div>
                  {contact.phone && (
                    <div className="flex items-center gap-3">
                      <Phone className="h-5 w-5 text-text-muted" />
                      <div>
                        <p className="text-sm text-text-secondary">Phone</p>
                        <p className="font-medium text-text-primary">
                          <a
                            href={`tel:${contact.phone}`}
                            className="hover:text-primary-500"
                          >
                            {contact.phone}
                          </a>
                        </p>
                      </div>
                    </div>
                  )}
                  {contact.taxNumber && (
                    <div className="flex items-center gap-3">
                      <FileText className="h-5 w-5 text-text-muted" />
                      <div>
                        <p className="text-sm text-text-secondary">
                          Tax Number
                        </p>
                        <p className="font-medium text-text-primary">
                          {contact.taxNumber}
                        </p>
                      </div>
                    </div>
                  )}
                  {contact.idNumber && (
                    <div className="flex items-center gap-3">
                      <FileText className="h-5 w-5 text-text-muted" />
                      <div>
                        <p className="text-sm text-text-secondary">
                          Government ID Number
                        </p>
                        <p className="font-medium text-text-primary">
                          {contact.idNumber}
                        </p>
                      </div>
                    </div>
                  )}
                  {contact.dateOfBirth && (
                    <div className="flex items-center gap-3">
                      <Calendar className="h-5 w-5 text-text-muted" />
                      <div>
                        <p className="text-sm text-text-secondary">
                          Date of Birth
                        </p>
                        <p className="font-medium text-text-primary">
                          {formatDate(contact.dateOfBirth)}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Additional Info + Tags */}
              <div className="space-y-6">
                <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                  <h2 className="text-lg font-semibold text-text-primary mb-4">
                    Additional Information
                  </h2>
                  {contact.notes ? (
                    <RichTextDisplay content={contact.notes} />
                  ) : (
                    <p className="text-sm text-text-muted italic">
                      No additional information available
                    </p>
                  )}
                </div>

                {/* Tags inline */}
                <ContactTagsTab contactId={id} contact={contact} />
              </div>
            </div>

            {/* Active Contracts & Properties */}
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-text-primary">
                  Active Contracts & Properties
                </h2>
                <button
                  onClick={() => navigate(`/contracts/new?contactId=${id}`)}
                  disabled={!canEditData}
                  className="text-sm text-primary-500 hover:text-primary-600 flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Plus className="h-4 w-4" />
                  Add Contract
                </button>
              </div>
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
                  No active contracts for this contact
                </p>
              )}

              {/* All contracts mini-table */}
              <ContactContractsTable
                contracts={contracts}
                contactIdentifier={id}
              />
            </div>

            {/* Follow-ups */}
            {(overdueFollowUps.length > 0 || upcomingFollowUps.length > 0) && (
              <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
                <h2 className="text-lg font-semibold text-text-primary mb-4">
                  Follow-ups
                </h2>
                <div className="space-y-2">
                  {overdueFollowUps.map((note) => (
                    <div
                      key={note.identifier}
                      className="flex items-center gap-3 px-3 py-2 bg-error-bg rounded text-sm"
                    >
                      <AlertCircle className="h-4 w-4 text-error-text flex-shrink-0" />
                      <span className="text-error-text font-medium">
                        Overdue: {formatDate(note.followUpDate ?? '')}
                      </span>
                      <span className="text-text-secondary truncate">
                        {note.subject ?? note.body.substring(0, 60)}
                      </span>
                    </div>
                  ))}
                  {upcomingFollowUps.map((note) => (
                    <div
                      key={note.identifier}
                      className="flex items-center gap-3 px-3 py-2 bg-surface-inset rounded text-sm"
                    >
                      <Calendar className="h-4 w-4 text-text-muted flex-shrink-0" />
                      <span className="font-medium">
                        {formatDate(note.followUpDate ?? '')}
                      </span>
                      <span className="text-text-secondary truncate">
                        {note.subject ?? note.body.substring(0, 60)}
                      </span>
                    </div>
                  ))}
                </div>
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
                  <ChevronUp className="h-5 w-5 text-text-secondary" />
                ) : (
                  <ChevronDown className="h-5 w-5 text-text-secondary" />
                )}
              </button>
              {isMetadataExpanded && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mt-4">
                  <div>
                    <span className="text-text-secondary">Created:</span>{' '}
                    <span className="text-text-primary">
                      {formatDate(contact.createdAt)} at{' '}
                      {new Date(contact.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-text-secondary">Last Updated:</span>{' '}
                    <span className="text-text-primary">
                      {contact.updatedAt ? (
                        <>
                          {formatDate(contact.updatedAt)} at{' '}
                          {new Date(contact.updatedAt).toLocaleTimeString()}
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

        {/* ===== Tab 2: Notes ===== */}
        {activeTab === 'notes' && (
          <div id="tabpanel-notes" role="tabpanel">
            <ContactNotesTab contactId={id} />
          </div>
        )}

        {/* ===== Tab 3: Activity ===== */}
        {activeTab === 'activity' && (
          <div id="tabpanel-activity" role="tabpanel">
            <ContactActivityTab contactId={id} />
          </div>
        )}

        {/* ===== Tab: Financials ===== */}
        {activeTab === 'financials' && (
          <div id="tabpanel-financials" role="tabpanel">
            <ContactFinancialsTab
              contactIdentifier={id}
              backTo={`/contacts/${id}?tab=financials`}
            />
          </div>
        )}

        {/* ===== Tab: Relationships ===== */}
        {activeTab === 'relationships' && (
          <div id="tabpanel-relationships" role="tabpanel">
            <ContactRelationshipsTab contactId={id} />
          </div>
        )}

        {/* ===== Tab: Files ===== */}
        {activeTab === 'files' && (
          <div id="tabpanel-files" role="tabpanel" className="space-y-6">
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Documents
              </h2>
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
            </div>
            <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
              <h2 className="text-lg font-semibold text-text-primary mb-4">
                Photos
              </h2>
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
          </div>
        )}

        {/* ===== Tab 5: Addresses ===== */}
        {activeTab === 'addresses' && (
          <div
            id="tabpanel-addresses"
            role="tabpanel"
            className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6"
          >
            <ContactAddressList contactId={id} />
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
          <ConfirmDialog
            title="Delete Contact"
            message="Are you sure you want to delete this contact? This action cannot be undone."
            confirmLabel="Delete"
            variant="danger"
            isLoading={deleteContactMutation.isPending}
            onConfirm={handleDelete}
            onCancel={() => setShowDeleteModal(false)}
          />
        )}

        {/* GDPR Erase Confirmation Modal */}
        {showEraseModal && (
          <ModalWrapper
            open
            onClose={() => {
              setShowEraseModal(false);
              setEraseConfirmText('');
            }}
            title="Permanently erase all personal data?"
            size="sm"
            preventClose={eraseContactMutation.isPending}
            footer={
              <>
                <Button
                  variant="secondary"
                  onClick={() => {
                    setShowEraseModal(false);
                    setEraseConfirmText('');
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  leftIcon={<ShieldAlert />}
                  onClick={handleErase}
                  disabled={eraseConfirmText !== 'ERASE'}
                  isLoading={eraseContactMutation.isPending}
                >
                  Erase Data
                </Button>
              </>
            }
          >
            <div className="space-y-4">
              <p className="text-text-secondary">
                This will permanently erase all personal data for this contact.
                Financial records linked via contracts are retained for legal
                compliance. This action cannot be undone.
              </p>
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1">
                  Type <span className="font-mono font-bold">ERASE</span> to
                  confirm
                </label>
                <input
                  type="text"
                  value={eraseConfirmText}
                  onChange={(e) => setEraseConfirmText(e.target.value)}
                  placeholder="ERASE"
                  className="w-full px-3 py-2 border border-border-strong rounded focus:border-error-text focus:ring-1 focus:ring-error-text bg-surface-card text-text-primary"
                />
              </div>
            </div>
          </ModalWrapper>
        )}
      </div>
    </div>
  );
};
