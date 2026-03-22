import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useContacts } from '@/hooks/useContactHooks';
import { ContactCard } from '@/components/contacts/ContactCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, Users, User, Search } from 'lucide-react';
import { useTeam } from '@/context/TeamContext';
import { usePagination } from '@/hooks/usePagination';
import { Pagination, RefreshButton } from '@buurman/ui';

export const ContactListPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
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
  } = useContacts({ search: debouncedSearch || undefined, ...pageParams });

  // Debounce search
  const handleSearch = (value: string) => {
    setSearchTerm(value);
    setTimeout(() => {
      setDebouncedSearch(value);
      resetPage();
    }, 500);
  };

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

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-text-muted " />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search by name, email, or phone..."
              className="w-full pl-10 pr-4 py-2 border border-border-strong rounded focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary"
            />
          </div>
        </div>

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
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-surface-card rounded-lg">
            <User className="h-16 w-16 text-text-disabled mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              No contacts yet
            </h3>
            <p className="text-text-secondary mb-6">
              Get started by adding your first contact
            </p>
            <button
              onClick={() => navigate('/contacts/new')}
              disabled={!canEditData}
              className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-primary-500"
            >
              <Plus className="h-5 w-5" />
              Add Contact
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
