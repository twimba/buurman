import { useState, useCallback, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useContacts, useCreateContact } from '@/hooks/useContactHooks';
import * as contactsApi from '@/api/contacts';
import { ContactCard } from '@/components/contacts/ContactCard';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  Users,
  Search,
  Filter,
  X,
  ChevronDown,
  ChevronUp,
  Save,
  Upload,
  ArrowUpDown,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { EmptyState, Pagination, RefreshButton, Skeleton } from '@buurman/ui';
import {
  ContactType,
  ContactTag,
  CreateContactRequest,
  CONTACT_TYPE_LABELS,
  CONTACT_TAG_LABELS,
} from '@/types/contact';
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';
import {
  useDuplicateCheck,
  DuplicateContactWarning,
} from '@/components/contacts/DuplicateContactWarning';
import { ImportWizard } from '@/components/contacts/ImportWizard';
import { ExportDropdown } from '@/components/common/ExportDropdown';

const CONTACT_TYPES: ContactType[] = [
  'INDIVIDUAL',
  'COMPANY',
  'SERVICE_PROVIDER',
];

const SORT_OPTIONS = [
  { value: 'createdAt', label: 'Date Created' },
  { value: 'displayName', label: 'Name' },
  { value: 'activeContractCount', label: 'Active Contracts' },
] as const;

export const ContactListPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [contactTypeFilter, setContactTypeFilter] = useState<
    ContactType | undefined
  >();
  const [tagFilters, setTagFilters] = useState<ContactTag[]>([]);
  const [showFilters, setShowFilters] = useState(false);
  const [showQuickAdd, setShowQuickAdd] = useState(false);
  const [quickAdd, setQuickAdd] = useState<CreateContactRequest>({
    contactType: 'INDIVIDUAL',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
  });
  const [quickAddErrors, setQuickAddErrors] = useState<Record<string, string>>(
    {}
  );
  const createMutation = useCreateContact();
  const {
    matches: quickAddDuplicates,
    dismissed: quickAddDupDismissed,
    setDismissed: setQuickAddDupDismissed,
    check: checkQuickAddDuplicates,
    reset: resetQuickAddDuplicates,
    blocking: quickAddDupBlocking,
  } = useDuplicateCheck();
  const {
    pageParams,
    page,
    size,
    sort,
    direction,
    handlePageChange,
    handleSizeChange,
    handleSortChange,
    resetPage,
  } = usePagination({
    defaultSize: 12,
    defaultSort: 'createdAt',
    defaultDirection: 'desc',
  });

  const {
    data: contactsData,
    isLoading,
    isFetching,
    refetch,
    error,
  } = useContacts({
    search: debouncedSearch || undefined,
    contactType: contactTypeFilter,
    tags: tagFilters.length > 0 ? tagFilters : undefined,
    ...pageParams,
  });

  // Trigger duplicate check as user types in quick-add
  useEffect(() => {
    if (showQuickAdd) {
      checkQuickAddDuplicates(quickAdd);
    }
  }, [quickAdd, showQuickAdd, checkQuickAddDuplicates]);

  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined
  );
  const handleSearch = useCallback(
    (value: string) => {
      setSearchTerm(value);
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
      debounceRef.current = setTimeout(() => {
        setDebouncedSearch(value);
        resetPage();
      }, 500);
    },
    [resetPage]
  );
  useEffect(
    () => () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    },
    []
  );

  const handleContactTypeChange = (type: ContactType | undefined) => {
    setContactTypeFilter(type);
    resetPage();
  };

  const toggleTag = (tag: ContactTag) => {
    setTagFilters((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
    resetPage();
  };

  const clearFilters = () => {
    setContactTypeFilter(undefined);
    setTagFilters([]);
    resetPage();
  };

  const handleQuickAddSubmit = async () => {
    const errs: Record<string, string> = {};
    if (quickAdd.contactType === 'INDIVIDUAL') {
      if (!quickAdd.firstName?.trim()) {
        errs.firstName = 'Required';
      }
    } else {
      if (!quickAdd.companyName?.trim()) {
        errs.companyName = 'Required';
      }
    }
    if (
      quickAdd.email?.trim() &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(quickAdd.email)
    ) {
      errs.email = 'Invalid email';
    }
    if (quickAdd.phone) {
      const phoneErr = validatePhoneE164(quickAdd.phone);
      if (phoneErr) {
        errs.phone = phoneErr;
      }
    }
    setQuickAddErrors(errs);
    if (Object.keys(errs).length > 0) {
      return;
    }
    const payload: CreateContactRequest = {
      ...quickAdd,
      firstName: quickAdd.firstName?.trim() || undefined,
      lastName: quickAdd.lastName?.trim() || undefined,
      email: quickAdd.email?.trim() || undefined,
      phone: quickAdd.phone?.trim() || undefined,
      companyName: quickAdd.companyName?.trim() || undefined,
    };
    await createMutation.mutateAsync(payload);
    setQuickAdd({
      contactType: 'INDIVIDUAL',
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
    });
    setQuickAddErrors({});
    setShowQuickAdd(false);
  };

  const [isExporting, setIsExporting] = useState(false);
  const [showImportWizard, setShowImportWizard] = useState(false);

  const downloadBlob = (blob: Blob, filename: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const handleExportCsv = async () => {
    setIsExporting(true);
    try {
      const blob = await contactsApi.exportContactsCsv();
      downloadBlob(blob, 'contacts.csv');
    } catch {
      // Download error — browser handles feedback
    } finally {
      setIsExporting(false);
    }
  };

  const handleExportXlsx = async () => {
    setIsExporting(true);
    try {
      const blob = await contactsApi.exportContactsXlsx();
      downloadBlob(blob, 'contacts.xlsx');
    } catch {
      // Download error — browser handles feedback
    } finally {
      setIsExporting(false);
    }
  };

  const hasActiveFilters = contactTypeFilter || tagFilters.length > 0;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="px-4 py-8 space-y-6">
          {/* Header skeleton */}
          <div className="flex justify-between items-center">
            <div className="space-y-2">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-4 w-64" />
            </div>
            <div className="flex items-center gap-2">
              <Skeleton className="h-10 w-24 rounded" />
              <Skeleton className="h-10 w-32 rounded" />
            </div>
          </div>
          {/* Search bar skeleton */}
          <Skeleton className="h-10 w-full rounded" />
          {/* Contact cards grid skeleton */}
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="p-4 rounded-lg border border-border-default space-y-3">
                <div className="flex items-center gap-3">
                  <Skeleton className="h-10 w-10 rounded-full" />
                  <div className="space-y-1.5 flex-1">
                    <Skeleton className="h-5 w-3/4" />
                    <Skeleton className="h-3 w-1/2" />
                  </div>
                </div>
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-2/3" />
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load contacts" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <Users className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">Contacts</h1>
            </div>
            <p className="text-text-secondary ml-11">
              Manage your contacts and their information
            </p>
          </div>
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <ExportDropdown
              size="md"
              disabled={isExporting}
              exporting={isExporting}
              options={[
                { label: 'CSV', onExport: handleExportCsv },
                { label: 'Excel', onExport: handleExportXlsx },
              ]}
            />
            {canEditData && (
              <button
                onClick={() => setShowImportWizard(true)}
                className="border border-border-strong bg-surface-card text-text-secondary px-3 py-2 rounded hover:border-primary-500 transition-colors flex items-center gap-1.5 text-sm"
                title="Import contacts from file"
              >
                <Upload className="h-4 w-4" />
                Import
              </button>
            )}
            {canEditData && (
              <button
                onClick={() => setShowQuickAdd(!showQuickAdd)}
                className={`border px-3 py-2 rounded transition-colors flex items-center gap-1.5 text-sm ${
                  showQuickAdd
                    ? 'border-primary-500 bg-primary-50 text-primary-600 dark:bg-primary-950 dark:text-primary-300'
                    : 'border-border-strong bg-surface-card text-text-secondary hover:border-primary-500'
                }`}
                title="Quick add contact"
              >
                {showQuickAdd ? (
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
                Quick Add
              </button>
            )}
            <button
              onClick={() => navigate('/contacts/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              Add Contact
            </button>
          </div>
        </div>

        {/* Search + Filter Toggle */}
        <div className="flex gap-3 mb-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-3 h-5 w-5 text-text-muted" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search by name, email, or phone..."
              aria-label="Search contacts"
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
            />
          </div>
          <div className="flex items-center gap-1.5">
            <ArrowUpDown className="h-4 w-4 text-text-muted" />
            <select
              value={sort ?? 'createdAt'}
              onChange={(e) => handleSortChange(e.target.value)}
              aria-label="Sort contacts by"
              className="border border-border-strong rounded px-2 py-2 text-sm bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <button
              onClick={() => handleSortChange(sort ?? 'createdAt')}
              className="px-2 py-2 border border-border-strong rounded bg-surface-card text-text-secondary hover:border-primary-500 text-sm"
              title={`Sort ${direction === 'asc' ? 'ascending' : 'descending'}`}
            >
              {direction === 'asc' ? '↑' : '↓'}
            </button>
          </div>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`flex items-center gap-2 px-4 py-2 border rounded transition-colors ${
              hasActiveFilters
                ? 'border-primary-500 bg-primary-50 text-primary-600 dark:bg-primary-950 dark:text-primary-300'
                : 'border-border-strong bg-surface-card text-text-secondary hover:border-primary-500'
            }`}
          >
            <Filter className="h-4 w-4" />
            Filters
            {hasActiveFilters && (
              <span className="bg-primary-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                {(contactTypeFilter ? 1 : 0) + tagFilters.length}
              </span>
            )}
          </button>
        </div>

        {/* Filter Panel */}
        {showFilters && (
          <div className="bg-surface-card border border-border-default rounded-lg p-4 mb-4 space-y-4">
            {/* Contact Type */}
            <div>
              <label className="text-sm font-medium text-text-secondary mb-2 block">
                Contact Type
              </label>
              <div className="flex flex-wrap gap-2">
                {CONTACT_TYPES.map((type) => (
                  <button
                    key={type}
                    onClick={() =>
                      handleContactTypeChange(
                        contactTypeFilter === type ? undefined : type
                      )
                    }
                    className={`px-3 py-1.5 text-sm rounded-full border transition-colors ${
                      contactTypeFilter === type
                        ? 'border-primary-500 bg-primary-50 text-primary-600 dark:bg-primary-950 dark:text-primary-300'
                        : 'border-border-strong text-text-secondary hover:border-primary-400'
                    }`}
                  >
                    {CONTACT_TYPE_LABELS[type]}
                  </button>
                ))}
              </div>
            </div>

            {/* Tags */}
            <div>
              <label className="text-sm font-medium text-text-secondary mb-2 block">
                Tags
              </label>
              <div className="flex flex-wrap gap-2">
                {Object.values(ContactTag).map((tag) => (
                  <button
                    key={tag}
                    onClick={() => toggleTag(tag)}
                    className={`px-3 py-1.5 text-sm rounded-full border transition-colors ${
                      tagFilters.includes(tag)
                        ? 'border-primary-500 bg-primary-50 text-primary-600 dark:bg-primary-950 dark:text-primary-300'
                        : 'border-border-strong text-text-secondary hover:border-primary-400'
                    }`}
                  >
                    {CONTACT_TAG_LABELS[tag]}
                  </button>
                ))}
              </div>
            </div>

            {/* Clear */}
            {hasActiveFilters && (
              <button
                onClick={clearFilters}
                className="text-sm text-primary-500 hover:text-primary-600 flex items-center gap-1"
              >
                <X className="h-3.5 w-3.5" />
                Clear all filters
              </button>
            )}
          </div>
        )}

        {/* Quick Add Panel */}
        {showQuickAdd && canEditData && (
          <div className="bg-surface-card border border-border-default rounded-lg p-4 mb-4">
            <div className="space-y-3">
              {/* Type selector row */}
              <div className="flex gap-2">
                {(
                  ['INDIVIDUAL', 'COMPANY', 'SERVICE_PROVIDER'] as ContactType[]
                ).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() =>
                      setQuickAdd((prev) => ({ ...prev, contactType: type }))
                    }
                    className={`px-3 py-1.5 text-sm rounded-full border transition-colors ${
                      quickAdd.contactType === type
                        ? 'border-primary-500 bg-primary-50 text-primary-600 dark:bg-primary-950 dark:text-primary-300'
                        : 'border-border-strong text-text-secondary hover:border-primary-400'
                    }`}
                  >
                    {CONTACT_TYPE_LABELS[type]}
                  </button>
                ))}
              </div>

              {/* Fields row */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                {quickAdd.contactType !== 'INDIVIDUAL' && (
                  <div>
                    <input
                      type="text"
                      value={quickAdd.companyName ?? ''}
                      onChange={(e) => {
                        setQuickAdd((prev) => ({
                          ...prev,
                          companyName: e.target.value,
                        }));
                        if (quickAddErrors.companyName) {
                          setQuickAddErrors((prev) => ({
                            ...prev,
                            companyName: '',
                          }));
                        }
                      }}
                      placeholder={
                        quickAdd.contactType === 'SERVICE_PROVIDER'
                          ? 'Business name *'
                          : 'Company name *'
                      }
                      className={`w-full border rounded px-3 py-2 text-sm bg-surface-card text-text-primary ${
                        quickAddErrors.companyName
                          ? 'border-error-text'
                          : 'border-border-strong'
                      } focus:border-primary-500 focus:ring-1 focus:ring-primary-500`}
                    />
                  </div>
                )}
                <div>
                  <input
                    type="text"
                    value={quickAdd.firstName ?? ''}
                    onChange={(e) => {
                      setQuickAdd((prev) => ({
                        ...prev,
                        firstName: e.target.value,
                      }));
                      if (quickAddErrors.firstName) {
                        setQuickAddErrors((prev) => ({
                          ...prev,
                          firstName: '',
                        }));
                      }
                    }}
                    placeholder={`First name${quickAdd.contactType === 'INDIVIDUAL' ? ' *' : ''}`}
                    className={`w-full border rounded px-3 py-2 text-sm bg-surface-card text-text-primary ${
                      quickAddErrors.firstName
                        ? 'border-error-text'
                        : 'border-border-strong'
                    } focus:border-primary-500 focus:ring-1 focus:ring-primary-500`}
                  />
                </div>
                <div>
                  <input
                    type="text"
                    value={quickAdd.lastName ?? ''}
                    onChange={(e) =>
                      setQuickAdd((prev) => ({
                        ...prev,
                        lastName: e.target.value,
                      }))
                    }
                    placeholder="Last name"
                    className="w-full border border-border-strong rounded px-3 py-2 text-sm bg-surface-card text-text-primary focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <input
                    type="email"
                    value={quickAdd.email ?? ''}
                    onChange={(e) => {
                      setQuickAdd((prev) => ({
                        ...prev,
                        email: e.target.value,
                      }));
                      if (quickAddErrors.email) {
                        setQuickAddErrors((prev) => ({ ...prev, email: '' }));
                      }
                    }}
                    placeholder="Email"
                    className={`w-full border rounded px-3 py-2 text-sm bg-surface-card text-text-primary ${
                      quickAddErrors.email
                        ? 'border-error-text'
                        : 'border-border-strong'
                    } focus:border-primary-500 focus:ring-1 focus:ring-primary-500`}
                  />
                </div>
                <div>
                  <PhoneInput
                    value={quickAdd.phone ?? null}
                    onChange={(e164) => {
                      setQuickAdd((prev) => ({ ...prev, phone: e164 ?? '' }));
                      if (quickAddErrors.phone) {
                        setQuickAddErrors((prev) => ({ ...prev, phone: '' }));
                      }
                    }}
                    error={quickAddErrors.phone}
                  />
                </div>
              </div>

              {/* Duplicate warning */}
              <DuplicateContactWarning
                matches={quickAddDuplicates}
                dismissed={quickAddDupDismissed}
                onDismiss={() => setQuickAddDupDismissed(true)}
                compact
              />

              {/* Action buttons */}
              <div className="flex items-center gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => {
                    setShowQuickAdd(false);
                    setQuickAddErrors({});
                    resetQuickAddDuplicates();
                  }}
                  className="px-3 py-1.5 text-sm text-text-secondary hover:text-text-primary transition-colors"
                >
                  Cancel
                </button>
                <div className="relative group/submit">
                  <button
                    type="button"
                    onClick={handleQuickAddSubmit}
                    disabled={createMutation.isPending || quickAddDupBlocking}
                    className="bg-primary-500 text-white px-4 py-1.5 text-sm rounded hover:bg-primary-600 transition-colors flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Save className="h-3.5 w-3.5" />
                    {createMutation.isPending ? 'Creating...' : 'Create'}
                  </button>
                  {quickAddDupBlocking && (
                    <div className="absolute bottom-full right-0 mb-2 px-3 py-2 text-xs font-medium text-white bg-neutral-800 dark:bg-neutral-700 rounded-lg whitespace-nowrap opacity-0 group-hover/submit:opacity-100 transition-opacity duration-150 shadow-lg pointer-events-none">
                      Dismiss the duplicate warning first
                      <div className="absolute top-full right-4 -mt-px border-4 border-transparent border-t-neutral-800 dark:border-t-neutral-700" />
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Active Filter Chips */}
        {hasActiveFilters && !showFilters && (
          <div className="flex flex-wrap gap-2 mb-4">
            {contactTypeFilter && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300">
                {CONTACT_TYPE_LABELS[contactTypeFilter]}
                <button
                  onClick={() => handleContactTypeChange(undefined)}
                  className="hover:text-primary-900"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}
            {tagFilters.map((tag) => (
              <span
                key={tag}
                className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full bg-surface-inset text-text-secondary"
              >
                {CONTACT_TAG_LABELS[tag]}
                <button
                  onClick={() => toggleTag(tag)}
                  className="hover:text-text-primary"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}
            <button
              onClick={clearFilters}
              className="text-xs text-primary-500 hover:text-primary-600"
            >
              Clear all
            </button>
          </div>
        )}

        {/* Contact Count */}
        <p className="text-sm text-text-secondary mb-4">
          {contactsData?.totalElements ?? 0}{' '}
          {contactsData?.totalElements === 1 ? 'contact' : 'contacts'}
        </p>

        {/* Contacts Grid */}
        {contactsData?.content && contactsData.content.length > 0 ? (
          <>
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {contactsData.content.map((contact) => (
                <ContactCard key={contact.identifier} contact={contact} />
              ))}
            </div>
            {contactsData && (
              <div className="mt-6">
                <Pagination
                  page={page}
                  totalPages={contactsData.totalPages}
                  totalElements={contactsData.totalElements}
                  size={size}
                  onPageChange={handlePageChange}
                  onSizeChange={handleSizeChange}
                />
              </div>
            )}
          </>
        ) : (
          <div className="bg-surface-card rounded-lg">
            <EmptyState
              icon={<Users className="h-12 w-12" />}
              title={
                hasActiveFilters
                  ? 'No contacts match your filters'
                  : 'No contacts yet'
              }
              description={
                hasActiveFilters
                  ? 'Try adjusting your filters or search term.'
                  : 'Add your contacts — individuals, companies, and service providers — to keep everything organized.'
              }
              variant="page"
              actions={
                hasActiveFilters ? (
                  <button
                    onClick={clearFilters}
                    className="text-primary-500 hover:text-primary-600 px-4 py-2 border border-primary-500 rounded transition-colors"
                  >
                    Clear filters
                  </button>
                ) : (
                  <button
                    onClick={() => navigate('/contacts/new')}
                    disabled={!canEditData}
                    className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
                  >
                    <Plus className="h-5 w-5" />
                    Add first contact
                  </button>
                )
              }
            />
          </div>
        )}
      </div>
      <ImportWizard
        open={showImportWizard}
        onClose={() => setShowImportWizard(false)}
      />
    </div>
  );
};
