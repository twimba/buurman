import { useState } from 'react';

interface GeneratePaymentsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (count: number) => Promise<void>;
  isLoading: boolean;
}

export default function GeneratePaymentsModal({
  isOpen,
  onClose,
  onSubmit,
  isLoading,
}: GeneratePaymentsModalProps) {
  const [count, setCount] = useState(1);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSubmit(count);
    setCount(1);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />
      <div className="relative bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h3 className="text-lg font-semibold mb-4">Generate Payments</h3>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label
              htmlFor="count"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Number of payments to generate
            </label>
            <input
              type="number"
              id="count"
              min={1}
              max={24}
              value={count}
              onChange={(e) => setCount(Math.max(1, Math.min(24, parseInt(e.target.value) || 1)))}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={isLoading}
            />
            <p className="text-xs text-gray-500 mt-1">
              Payments will be generated for the next {count} period(s) based on the contract's payment frequency.
            </p>
          </div>

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
              disabled={isLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
              disabled={isLoading}
            >
              {isLoading ? 'Generating...' : `Generate ${count} Payment${count > 1 ? 's' : ''}`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
