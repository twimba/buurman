import { useState } from 'react';
import { Calendar, CalendarCheck } from 'lucide-react';

interface GeneratePaymentsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (count: number, markAsPaid: boolean) => Promise<void>;
  isLoading: boolean;
}

export default function GeneratePaymentsModal({
  isOpen,
  onClose,
  onSubmit,
  isLoading,
}: GeneratePaymentsModalProps) {
  const [count, setCount] = useState(1);

  if (!isOpen) {
    return null;
  }

  const submitForm = async (markAsPaid: boolean) => {
    await onSubmit(count, markAsPaid);
    setCount(1);
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitForm(false);
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" />
      <div className="relative bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 w-full max-w-md p-6">
        <h3 className="text-lg font-semibold mb-4 text-text-primary">
          Generate Future Payments
        </h3>

        <form onSubmit={handleSubmit} onKeyDown={handleCmdEnter}>
          <div className="mb-4">
            <label
              htmlFor="count"
              className="block text-sm font-medium text-text-secondary mb-1"
            >
              Number of payments to generate
            </label>
            <input
              type="number"
              id="count"
              min={1}
              max={24}
              value={count}
              onChange={(e) =>
                setCount(
                  Math.max(1, Math.min(24, parseInt(e.target.value) || 1))
                )
              }
              className="w-full px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
              disabled={isLoading}
            />
            <p className="text-xs text-text-secondary mt-1">
              Will generate {count} future payment(s) based on the
              contract&apos;s payment frequency.
            </p>
          </div>

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm text-text-secondary hover:bg-surface-inset rounded-md"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm text-text-secondary border border-border-strong hover:bg-surface-inset rounded-md disabled:opacity-50 flex items-center gap-1.5"
              disabled={isLoading}
            >
              <Calendar className="h-4 w-4" />
              Generate
            </button>
            <button
              type="button"
              onClick={() => submitForm(true)}
              className="px-4 py-2 text-sm text-white bg-primary-500 hover:bg-primary-600 rounded-md disabled:opacity-50 flex items-center gap-1.5"
              disabled={isLoading}
            >
              <CalendarCheck className="h-4 w-4" />
              {isLoading ? 'Generating...' : 'Generate & Mark Paid'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
