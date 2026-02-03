import { useState } from 'react';
import { ContractStatus } from '@/types/contract';
import { X } from 'lucide-react';
import { RichTextEditor } from '@/components/common/RichTextEditor';

interface ChangeContractStatusModalProps {
  currentStatus: ContractStatus;
  onClose: () => void;
  onConfirm: (newStatus: ContractStatus, reason?: string) => void;
  isLoading?: boolean;
}

const getValidTransitions = (
  currentStatus: ContractStatus
): ContractStatus[] => {
  switch (currentStatus) {
    case ContractStatus.DRAFT:
      return [ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE];
    case ContractStatus.PENDING_SIGNATURE:
      return [ContractStatus.DRAFT, ContractStatus.ACTIVE];
    case ContractStatus.ACTIVE:
      return [ContractStatus.TERMINATED, ContractStatus.EXPIRED];
    case ContractStatus.EXPIRED:
    case ContractStatus.TERMINATED:
      return []; // Terminal states
    default:
      return [];
  }
};

const statusLabels: Record<ContractStatus, string> = {
  DRAFT: 'Draft',
  PENDING_SIGNATURE: 'Pending Signature',
  ACTIVE: 'Active',
  EXPIRED: 'Expired',
  TERMINATED: 'Terminated',
};

export const ChangeContractStatusModal = ({
  currentStatus,
  onClose,
  onConfirm,
  isLoading = false,
}: ChangeContractStatusModalProps) => {
  const validTransitions = getValidTransitions(currentStatus);
  const [selectedStatus, setSelectedStatus] = useState<ContractStatus>(
    validTransitions[0] || currentStatus
  );
  const [reason, setReason] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onConfirm(selectedStatus, reason || undefined);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl dark:shadow-gray-900 max-w-md w-full mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            Change Contract Status
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            disabled={isLoading}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit}>
          <div className="p-4 space-y-4">
            {/* Current Status */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Current Status
              </label>
              <p className="text-sm text-gray-900 dark:text-gray-100">
                {statusLabels[currentStatus]}
              </p>
            </div>

            {/* New Status Selection */}
            {validTransitions.length > 0 ? (
              <>
                <div>
                  <label
                    htmlFor="newStatus"
                    className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
                  >
                    New Status
                  </label>
                  <select
                    id="newStatus"
                    value={selectedStatus}
                    onChange={(e) =>
                      setSelectedStatus(e.target.value as ContractStatus)
                    }
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                    disabled={isLoading}
                  >
                    {validTransitions.map((status) => (
                      <option key={status} value={status}>
                        {statusLabels[status]}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Reason */}
                <div>
                  <label
                    htmlFor="reason"
                    className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
                  >
                    Reason (Optional)
                  </label>
                  <RichTextEditor
                    value={reason}
                    onChange={setReason}
                    placeholder="Enter reason for status change..."
                  />
                </div>
              </>
            ) : (
              <div className="text-sm text-gray-600 dark:text-gray-400">
                No valid status transitions available from{' '}
                {statusLabels[currentStatus]}.
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 p-4 border-t border-gray-200 dark:border-gray-700">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600"
              disabled={isLoading}
            >
              Cancel
            </button>
            {validTransitions.length > 0 && (
              <button
                type="submit"
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
                disabled={isLoading}
              >
                {isLoading ? 'Changing...' : 'Change Status'}
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
};
