import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useTabState } from '@/hooks/useTabState';
import { useProperty, useDeleteProperty } from '@/hooks/usePropertyHooks';
import {
  useOccupancyPeriods,
  useDeleteOccupancyPeriod,
} from '@/hooks/useOccupancyPeriodHooks';
import { EndSelfOccupancyModal } from '@/components/properties/EndSelfOccupancyModal';
import { useFinancings } from '@/hooks/usePropertyFinancialsHooks';
import { FinancingFormModal } from '@/components/properties/financials/modals/FinancingFormModal';
import {
  useLatestWwsCalculation,
  useWwsCalculations,
  useDeleteWwsCalculation,
} from '@/hooks/useWwsHooks';
import { SelfOccupancyCard } from '@/components/properties/SelfOccupancyCard';
import { PropertyLifecycleTimeline } from '@/components/properties/PropertyLifecycleTimeline';
import { PropertyTypeIcon } from '@/components/common/PropertyTypeIcon';
import { EditSelfOccupancyModal } from '@/components/properties/EditSelfOccupancyModal';
import { CalendarFeedResponseFeedType as CalendarFeedType } from '@/generated/models';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { WwsCalculatorModal } from '@/components/wws/WwsCalculatorModal';
import { InteractiveMap } from '@/components/common/InteractiveMap';
import {
  PropertyStatus,
  PropertyCategory,
  PROPERTY_TYPE_LABELS,
  PROPERTY_CATEGORY_LABELS,
  PROPERTY_STATUS_LABELS,
} from '@/types/property';
import { PropertyDashboardTab } from '@/components/properties/dashboard/PropertyDashboardTab';
import { PropertyFinancialsTab } from '@/components/properties/financials/PropertyFinancialsTab';
import { PropertyContractsTab } from '@/components/properties/PropertyContractsTab';
import { PropertyExpensesTab } from '@/components/properties/PropertyExpensesTab';
import { PropertyDocumentsTab } from '@/components/properties/PropertyDocumentsTab';
import { PropertyPhotosTab } from '@/components/properties/PropertyPhotosTab';
import { PropertyAuditTab } from '@/components/properties/PropertyAuditTab';
import { FeatureGate } from '@/components/FeatureGate';
import { FeatureFlags } from '@/constants/featureFlags';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Button, PageHeader, Skeleton, useToast } from '@buurman/ui';
import { trackEvent } from '@/utils/analytics';
import { AnalyticsEvent } from '@/constants/analyticsEvents';
import { useTeam } from '@/context/TeamContext';
import { downloadPropertyBooklet } from '@/api/properties';
import DOMPurify from 'dompurify';
import { useFormatDate } from '@/hooks/useFormatDate';
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
  Receipt,
  ChevronUp,
  ChevronDown,
  Download,
  BarChart3,
  Wallet,
  Calculator,
  X,
} from 'lucide-react';

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

export const PropertyDetailPage = () => {
  const { t } = useTranslation('properties');
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canEditData, canManageMembers } = useTeam();
  const { formatDate } = useFormatDate();
  const { showToast } = useToast();
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

  // Page-level modal state
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showWwsModal, setShowWwsModal] = useState(false);
  const [showDeleteWwsConfirm, setShowDeleteWwsConfirm] = useState(false);
  const [deleteWwsHistoryId, setDeleteWwsHistoryId] = useState<string | null>(
    null
  );
  const [showWwsHistory, setShowWwsHistory] = useState(false);
  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);
  const [editOccupancyPeriodId, setEditOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [editFinancingId, setEditFinancingId] = useState<string | null>(null);
  // Info-tab self-occupancy card actions
  const [showEndOccupancyModal, setShowEndOccupancyModal] = useState(false);
  const [endOccupancyPeriodId, setEndOccupancyPeriodId] = useState<
    string | null
  >(null);
  const [deleteOccupancyPeriodId, setDeleteOccupancyPeriodId] = useState<
    string | null
  >(null);

  // Core property data
  const { data: property, isLoading, error } = useProperty(id);
  const propertyIdentifier = property?.identifier;

  useEffect(() => {
    if (propertyIdentifier) {
      trackEvent(AnalyticsEvent.PROPERTY_VIEWED);
    }
  }, [propertyIdentifier]);

  // WWS (NL-only)
  const isNlProperty = property?.countryCode === 'NL';
  const { data: latestWws } = useLatestWwsCalculation(
    isNlProperty ? id : undefined
  );
  const { data: wwsHistory = [] } = useWwsCalculations(
    isNlProperty && showWwsHistory ? id : undefined
  );
  const deleteWwsMutation = useDeleteWwsCalculation(id);

  // Info tab data (needed for timeline and self-occupancy card)
  const { data: occupancyPeriods = [] } = useOccupancyPeriods(id);
  const { data: financings = [] } = useFinancings(id);
  const activeOccupancyPeriod = occupancyPeriods.find(
    (p) => !p.endDate || new Date(p.endDate) >= new Date()
  );

  const deleteOccupancyMutation = useDeleteOccupancyPeriod(id);
  const deletePropertyMutation = useDeleteProperty();

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

  const handleDownloadBooklet = async () => {
    try {
      const blob = await downloadPropertyBooklet(id);
      const pdfBlob = new Blob([blob], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(pdfBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'property-booklet.pdf';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download booklet:', err);
      showToast(t('detail.bookletDownloadFailed'), 'error');
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="px-4 py-8 space-y-6">
          {/* Header skeleton */}
          <div className="flex items-center gap-4">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-6 w-20 rounded-full" />
          </div>
          <div className="flex items-center gap-2">
            <Skeleton className="h-4 w-48" />
          </div>
          {/* Action buttons skeleton */}
          <div className="flex gap-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-24 rounded" />
            ))}
          </div>
          {/* Tab bar skeleton */}
          <div className="flex gap-6 border-b border-border-default pb-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-5 w-20" />
            ))}
          </div>
          {/* Tab content skeleton */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="space-y-4">
              <Skeleton className="h-48 w-full rounded-lg" />
              <Skeleton className="h-32 w-full rounded-lg" />
            </div>
            <div className="space-y-4">
              <Skeleton className="h-64 w-full rounded-lg" />
              <Skeleton className="h-32 w-full rounded-lg" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message={t('detail.notFound')} />
      </div>
    );
  }

  // All rich text fields are sanitized with DOMPurify before rendering
  const sanitize = DOMPurify.sanitize;

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
                onClick={handleDownloadBooklet}
              >
                {t('detail.booklet')}
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
              {t('detail.tabs.info')}
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
                {t('detail.tabs.dashboard')}
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
              {t('detail.tabs.financials')}
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
              {t('detail.tabs.photos')}
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`px-4 py-2 border-b-2 transition-colors ${
                activeTab === 'documents'
                  ? 'border-primary-500 text-primary-500 font-semibold'
                  : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}
            >
              {t('detail.tabs.documents')}
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
              {t('detail.tabs.contracts')}
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
              {t('detail.tabs.expenses')}
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
              {t('detail.tabs.history')}
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
                  {t('detail.timeline.title')}
                </span>
                <div className="flex items-center gap-4 text-xs text-text-secondary">
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-3 h-2.5 rounded-full"
                      style={{ background: 'rgba(59,130,246,1)' }}
                    />
                    {t('detail.timeline.rentalContract')}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-3 h-2.5 rounded-full"
                      style={{ background: 'rgba(99,102,241,1)' }}
                    />
                    {t('detail.timeline.selfOccupied')}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span
                      className="inline-block w-3 h-1.5 rounded-full"
                      style={{ background: 'rgba(245,158,11,0.5)' }}
                    />
                    {t('detail.timeline.financing')}
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
              {/* Status & Category */}
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
                <span className="text-text-disabled">{'\u00b7'}</span>
                <span className="inline-flex items-center gap-1 text-text-secondary">
                  <PropertyTypeIcon
                    category={property.propertyCategory}
                    size={13}
                  />
                  {PROPERTY_CATEGORY_LABELS[
                    property.propertyCategory as PropertyCategory
                  ] ?? property.propertyCategory}
                </span>
                <span className="text-text-disabled">{'\u00b7'}</span>
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
                      <span className="text-sm font-medium">
                        {t('detail.specs.bedrooms')}
                      </span>
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
                      <span className="text-sm font-medium">
                        {t('detail.specs.bathrooms')}
                      </span>
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
                      <span className="text-sm font-medium">
                        {t('detail.specs.area')}
                      </span>
                    </div>
                    <p className="text-2xl font-semibold text-text-primary">
                      {property.areaValue}
                      {property.areaUnit === 'sqft' ? 'ft\u00b2' : 'm\u00b2'}
                    </p>
                  </div>
                )}

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <PropertyTypeIcon type={property.propertyType} size={20} />
                    <span className="text-sm font-medium">
                      {t('detail.specs.type')}
                    </span>
                  </div>
                  <p className="text-lg font-semibold text-text-primary">
                    {PROPERTY_TYPE_LABELS[property.propertyType] ??
                      property.propertyType}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">
                      {t('detail.specs.street')}
                    </span>
                  </div>
                  <p className="text-lg text-text-primary">{property.street}</p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">
                      {t('detail.specs.city')}
                    </span>
                  </div>
                  <p className="text-lg text-text-primary">{property.city}</p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">
                      {t('detail.specs.postalCode')}
                    </span>
                  </div>
                  <p className="text-lg text-text-primary">
                    {property.postalCode}
                  </p>
                </div>

                <div>
                  <div className="flex items-center gap-2 text-text-secondary mb-1">
                    <MapPin className="h-5 w-5" />
                    <span className="text-sm font-medium">
                      {t('detail.specs.country')}
                    </span>
                  </div>
                  <p className="text-lg text-text-primary">
                    {property.countryCode}
                  </p>
                </div>
              </div>

              {/* Map */}
              <div className="pt-6 border-t">
                <h3 className="text-sm font-semibold text-text-secondary mb-3">
                  {t('detail.specs.location')}
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
                  {t('detail.construction.title')}
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
                        __html: sanitize(property.structuralNotes),
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
                  {t('detail.energy.title')}
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
                        __html: sanitize(property.insulationNotes),
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
                  {t('detail.utilities.title')}
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
                  {t('detail.safety.title')}
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
                        __html: sanitize(property.safetyNotes),
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
                        __html: sanitize(property.accessibilityNotes),
                      }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* WWS Points Calculator -- NL properties only */}
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

        {activeTab === 'photos' && <PropertyPhotosTab propertyId={id} />}

        {activeTab === 'documents' && <PropertyDocumentsTab propertyId={id} />}

        {activeTab === 'contracts' && <PropertyContractsTab propertyId={id} />}

        {activeTab === 'expenses' && <PropertyExpensesTab propertyId={id} />}

        {activeTab === 'audit' && <PropertyAuditTab propertyId={id} />}
      </div>

      {/* Page-level modals */}
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
