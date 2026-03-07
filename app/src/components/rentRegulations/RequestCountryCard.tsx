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
        className="group text-left p-5 rounded-xl border-2 border-dashed border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc]/50 dark:bg-[#0c0d14]/50 hover:border-[#5c7cfa]/40 dark:hover:border-[#748ffc]/40 hover:bg-[#f0f4ff] dark:hover:bg-[#5c7cfa]/[0.05] transition-all duration-200"
      >
        <div className="flex items-center gap-3 mb-2">
          <span className="flex items-center justify-center w-[46px] h-[46px] rounded-xl bg-[#f1f3f9] dark:bg-[#1e2130] text-[#9ca0b8] dark:text-[#5c6180] group-hover:bg-[#5c7cfa]/10 dark:group-hover:bg-[#5c7cfa]/20 group-hover:text-[#5c7cfa] dark:group-hover:text-[#91a7ff] transition-colors">
            <Globe className="h-5 w-5" />
          </span>
          <span className="font-semibold text-[#6b7194] dark:text-[#8b90a8] group-hover:text-[#5c7cfa] dark:group-hover:text-[#91a7ff] transition-colors">
            Missing a country?
          </span>
        </div>
        <p className="text-sm text-[#9ca0b8] dark:text-[#5c6180] leading-relaxed">
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
          <div className="relative w-full max-w-md bg-white dark:bg-[#14161f] rounded-2xl border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-xl overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <div className="flex items-center gap-2">
                <MessageCircleHeart className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
                <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Request a Country
                </h3>
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 rounded-lg text-[#9ca0b8] hover:text-[#3d4463] hover:bg-[#f1f3f9] dark:hover:text-[#c4c8db] dark:hover:bg-[#1e2130] transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Body */}
            <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] leading-relaxed">
                We&apos;re always expanding our coverage. Tell us which country
                you&apos;d like regulation data for, and we&apos;ll bump it up
                the priority list!
              </p>

              <div>
                <label className="block text-xs font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
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
                  className="w-full h-10 px-3 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] text-sm placeholder:text-[#9ca0b8] dark:placeholder:text-[#5c6180] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-1.5">
                  Anything else we should know?{' '}
                  <span className="font-normal text-[#9ca0b8]">(optional)</span>
                </label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Specific regions, property types, urgency..."
                  rows={3}
                  maxLength={1000}
                  className="w-full px-3 py-2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] text-sm placeholder:text-[#9ca0b8] dark:placeholder:text-[#5c6180] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors resize-none"
                />
              </div>

              <div className="flex justify-end gap-3 pt-1">
                <button
                  type="button"
                  onClick={() => setIsOpen(false)}
                  className="px-4 py-2 rounded-lg text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!countryName.trim() || mutation.isPending}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-b from-[#5c7cfa] to-[#4c6ef5] text-white border border-[#4263eb] shadow-sm shadow-[#5c7cfa]/20 hover:from-[#4c6ef5] hover:to-[#4263eb] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
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
