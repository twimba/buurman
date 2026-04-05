import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useContact, useUpdateContact } from '@/hooks/useContactHooks';
import { ContactForm } from '@/components/contacts/ContactForm';
import { UpdateContactRequest } from '@/types/contact';
import { LoadingSpinner } from '@buurman/ui';
import { ErrorMessage } from '@/components/ErrorMessage';
import { ArrowLeft } from 'lucide-react';

export const ContactEditPage = () => {
  const { t } = useTranslation('tenants');
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: contact, isLoading, error } = useContact(id);
  const updateContactMutation = useUpdateContact(id);

  const handleSubmit = async (data: UpdateContactRequest) => {
    await updateContactMutation.mutateAsync(data);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-surface-page flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !contact) {
    return (
      <div className="min-h-screen bg-surface-page p-8">
        <ErrorMessage message={t('edit.failedToLoad')} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(`/contacts/${id}`)}
            className="p-2 hover:bg-neutral-100 rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">
            {t('edit.title')}
          </h1>
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <ContactForm
            contact={contact}
            onSubmit={handleSubmit}
            isLoading={updateContactMutation.isPending}
          />
        </div>
      </div>
    </div>
  );
};
