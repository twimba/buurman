import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useContacts } from '@/hooks/useContactHooks';
import { ContactCard } from '@/components/contacts/ContactCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  Plus,
  Users,
  Search,
  Filter,
  X,
} from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { Pagination, RefreshButton, EmptyState } from '@buurman/ui';
import {
  ContactType,
  ContactTag,
  CONTACT_TYPE_LABELS,
  CONTACT_TAG_LABELS,
} from '@/types/contact';

const CONTACT_TYPES: ContactType[] = ['INDIVIDUAL', 'COMPANY', 'SERVICE_PROVIDER'];

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
  const {
    pageParams,
    page,
    size,
    handlePageChange,
    handleSizeChange,
    resetPage,
  } = usePagination({ defaultSize: 12 });

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

  const handleSearch = useCallback(
    (value: string) => {
      setSearchTerm(value);
      setTimeout(() => {
        setDebouncedSearch(value);
        resetPage();
      }, 500);
    },
    [resetPage]
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

  const hasActiveFilters = contactTypeFilter || tagFilters.length > 0;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
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
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
            />
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
              title={hasActiveFilters ? 'No contacts match your filters' : 'No contacts yet'}
              description={
                hasActiveFilters
                  ? 'Try adjusting your filters or search term.'
                  : 'Add your tenants, companies, and service providers to keep everything organized.'
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
    </div>
  );
};
