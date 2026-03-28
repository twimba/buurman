import { ImportHistory } from '@/components/contacts/ImportHistory';
import { ArrowLeft, History } from 'lucide-react';
import { Link } from 'react-router-dom';

export const ImportHistoryPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        <div className="flex items-center gap-3 mb-6">
          <Link
            to="/contacts"
            className="text-text-secondary hover:text-text-primary transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <History className="h-8 w-8 text-primary-500 dark:text-primary-300" />
          <div>
            <h1 className="text-3xl font-bold text-text-primary">
              Import History
            </h1>
            <p className="text-text-secondary">
              View past imports and manage imported data
            </p>
          </div>
        </div>
        <ImportHistory />
      </div>
    </div>
  );
};
