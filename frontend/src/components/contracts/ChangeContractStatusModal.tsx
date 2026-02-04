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
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-xl dark:shadow-black/20 max-w-md w-full mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Change Contract Status
          </h2>
          <button
            onClick={onClose}
            className="text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:text-[#8b90a8] dark:hover:text-[#c4c8db]"
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
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Current Status
              </label>
              <p className="text-sm text-[#1a1d2e] dark:text-[#eef0f6]">
                {statusLabels[currentStatus]}
              </p>
            </div>

            {/* New Status Selection */}
            {validTransitions.length > 0 ? (
              <>
                <div>
                  <label
                    htmlFor="newStatus"
                    className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1"
                  >
                    New Status
                  </label>
                  <select
                    id="newStatus"
                    value={selectedStatus}
                    onChange={(e) =>
                      setSelectedStatus(e.target.value as ContractStatus)
                    }
                    className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]"
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
                    className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1"
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
              <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                No valid status transitions available from{' '}
                {statusLabels[currentStatus]}.
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 p-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] bg-white dark:bg-[#1e2130] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md hover:bg-[#f1f3f9] dark:hover:bg-[#3a3f54]"
              disabled={isLoading}
            >
              Cancel
            </button>
            {validTransitions.length > 0 && (
              <button
                type="submit"
                className="px-4 py-2 text-sm font-medium text-white bg-[#5c7cfa] rounded-md hover:bg-[#4c6ef5] disabled:opacity-50"
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
