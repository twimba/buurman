import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useCreateContact } from '@/hooks/useContactHooks';
import { ContactForm } from '@/components/contacts/ContactForm';
import { CreateContactRequest } from '@/types/contact';
import { ArrowLeft } from 'lucide-react';
import { ImpersonationGuard } from '@/components/ImpersonationGuard';

export const ContactCreatePage = () => {
  const { t } = useTranslation('tenants');
  const navigate = useNavigate();
  const createContactMutation = useCreateContact();

  const handleSubmit = async (data: CreateContactRequest) => {
    await createContactMutation.mutateAsync(data);
  };

  return (
    <div className="min-h-screen bg-surface-page">
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate('/contacts')}
            className="p-2 hover:bg-surface-inset rounded transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">
            {t('create.title')}
          </h1>
        </div>

        {/* Form */}
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <ImpersonationGuard
            blockInReadOnly
            fallback={
              <div className="text-center py-8 text-text-secondary">
                {t('create.readOnlyMessage')}
              </div>
            }
          >
            <ContactForm
              onSubmit={handleSubmit}
              isLoading={createContactMutation.isPending}
            />
          </ImpersonationGuard>
        </div>
      </div>
    </div>
  );
};
