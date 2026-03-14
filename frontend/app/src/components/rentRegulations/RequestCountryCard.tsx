import { useState } from 'react';
import { Globe, Send, X, MessageCircleHeart } from 'lucide-react';
import { useRequestCountryRegulation } from '@/hooks/useRentRegulationHooks';

export const RequestCountryCard = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [countryName, setCountryName] = useState('');
  const [notes, setNotes] = useState('');
  const mutation = useRequestCountryRegulation();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!countryName.trim()) {
      return;
    }
    mutation.mutate(
      { countryName: countryName.trim(), notes: notes.trim() || undefined },
      {
        onSuccess: () => {
          setCountryName('');
          setNotes('');
          setIsOpen(false);
        },
      }
    );
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className="group text-left p-5 rounded-lg border-2 border-dashed border-border-default bg-surface-page/50 hover:border-primary-500/40 dark:hover:border-primary-400/40 hover:bg-primary-50 dark:hover:bg-primary-500/[0.05] transition-all duration-200"
      >
        <div className="flex items-center gap-3 mb-2">
          <span className="flex items-center justify-center w-[46px] h-[46px] rounded-xl bg-surface-inset text-text-muted group-hover:bg-primary-500/10 group-hover:text-primary-500 transition-colors">
            <Globe className="h-5 w-5" />
          </span>
          <span className="font-semibold text-text-secondary group-hover:text-primary-500 transition-colors">
            Missing a country?
          </span>
        </div>
        <p className="text-sm text-text-muted leading-relaxed">
          Don&apos;t see your country listed? Let us know and we&apos;ll work on
          adding it!
        </p>
      </button>

      {/* Modal */}
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            onClick={() => setIsOpen(false)}
          />
          <div className="relative w-full max-w-md bg-surface-card rounded-2xl border border-border-default shadow-xl overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
              <div className="flex items-center gap-2">
                <MessageCircleHeart className="h-5 w-5 text-primary-500 dark:text-primary-300" />
                <h3 className="font-semibold text-text-primary">
                  Request a Country
                </h3>
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 rounded-lg text-text-muted hover:text-text-secondary hover:bg-surface-inset transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Body */}
            <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
              <p className="text-sm text-text-secondary leading-relaxed">
                We&apos;re always expanding our coverage. Tell us which country
                you&apos;d like regulation data for, and we&apos;ll bump it up
                the priority list!
              </p>

              <div>
                <label className="block text-xs font-semibold text-text-secondary mb-1.5">
                  Country name
                </label>
                <input
                  type="text"
                  value={countryName}
                  onChange={(e) => setCountryName(e.target.value)}
                  placeholder="e.g. Germany, Brazil, Japan..."
                  autoFocus
                  required
                  maxLength={255}
                  className="w-full h-10 px-3 rounded-lg border border-border-default bg-surface-card text-text-primary text-sm placeholder:text-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text-secondary mb-1.5">
                  Anything else we should know?{' '}
                  <span className="font-normal text-text-muted">
                    (optional)
                  </span>
                </label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Specific regions, property types, urgency..."
                  rows={3}
                  maxLength={1000}
                  className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-text-primary text-sm placeholder:text-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors resize-none"
                />
              </div>

              <div className="flex justify-end gap-3 pt-1">
                <button
                  type="button"
                  onClick={() => setIsOpen(false)}
                  className="px-4 py-2 rounded-lg text-sm font-medium text-text-secondary hover:bg-surface-inset transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!countryName.trim() || mutation.isPending}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-b from-primary-500 to-primary-600 text-white border border-primary-600 shadow-sm shadow-primary-500/20 hover:from-primary-400 hover:to-primary-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Send className="h-3.5 w-3.5" />
                  {mutation.isPending ? 'Sending...' : 'Send Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};
