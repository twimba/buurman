import { useState } from 'react';
import {
  CreditCard,
  ChevronDown,
  ChevronUp,
  Plus,
  Edit,
  Trash2,
  Clock,
  Building,
  Loader2,
} from 'lucide-react';
import { Button } from '../ui';
import { RichTextEditor } from '../common/RichTextEditor';
import { RichTextDisplay } from '../common/RichTextDisplay';
import {
  useCurrentContractPaymentInstruction,
  useContractPaymentInstructions,
  useCreateContractPaymentInstruction,
  useDeleteContractPaymentInstruction,
  useUpdateContractPaymentInstruction,
} from '../../hooks/usePaymentInstructionHooks';
import { usePaymentInstructions } from '../../hooks/usePaymentInstructionHooks';
import {
  PaymentMethodLabels,
  PaymentMethod,
  ContractPaymentInstructionResponse,
  CreateContractPaymentInstructionRequest,
} from '../../types/paymentInstruction';
import { useTeam } from '../../context/TeamContext';
import { useFormatDate } from '../../hooks/useFormatDate';

interface Props {
  contractIdentifier: string;
}

export const ContractPaymentInstructionSection = ({
  contractIdentifier,
}: Props) => {
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const { data: current, isLoading: loadingCurrent } =
    useCurrentContractPaymentInstruction(contractIdentifier);
  const { data: history = [], isLoading: loadingHistory } =
    useContractPaymentInstructions(contractIdentifier);
  const { data: templates = [] } = usePaymentInstructions();
  const createMutation =
    useCreateContractPaymentInstruction(contractIdentifier);
  const updateMutation =
    useUpdateContractPaymentInstruction(contractIdentifier);
  const deleteMutation =
    useDeleteContractPaymentInstruction(contractIdentifier);

  const [showForm, setShowForm] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [useTemplate, setUseTemplate] = useState(true);
  const [selectedTemplate, setSelectedTemplate] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [notes, setNotes] = useState('');

  // Custom fields
  const [customName, setCustomName] = useState('');
  const [customDescription, setCustomDescription] = useState('');
  const [customPaymentMethod, setCustomPaymentMethod] =
    useState('BANK_TRANSFER');
  const [customBankName, setCustomBankName] = useState('');
  const [customAccountHolderName, setCustomAccountHolderName] = useState('');
  const [customIban, setCustomIban] = useState('');
  const [customBicSwift, setCustomBicSwift] = useState('');
  const [customPaymentReference, setCustomPaymentReference] = useState('');

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setUseTemplate(true);
    setSelectedTemplate('');
    setEffectiveFrom(new Date().toISOString().split('T')[0]);
    setNotes('');
    setCustomName('');
    setCustomDescription('');
    setCustomPaymentMethod('BANK_TRANSFER');
    setCustomBankName('');
    setCustomAccountHolderName('');
    setCustomIban('');
    setCustomBicSwift('');
    setCustomPaymentReference('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const data: CreateContractPaymentInstructionRequest = {
      effectiveFrom,
      notes: notes || undefined,
      ...(useTemplate
        ? { paymentInstructionIdentifier: selectedTemplate, isCustom: false }
        : {
            isCustom: true,
            customName,
            customDescription: customDescription || undefined,
            customPaymentMethod,
            customBankName: customBankName || undefined,
            customAccountHolderName: customAccountHolderName || undefined,
            customIban: customIban || undefined,
            customBicSwift: customBicSwift || undefined,
            customPaymentReference: customPaymentReference || undefined,
          }),
    };

    if (editingId) {
      updateMutation.mutate(
        { instructionId: editingId, data },
        { onSuccess: resetForm }
      );
    } else {
      createMutation.mutate(data, { onSuccess: resetForm });
    }
  };

  const handleChange = () => {
    if (current) {
      setEditingId(current.identifier);
    }
    setShowForm(true);
  };

  const selectedTemplateData = templates.find(
    (t) => t.identifier === selectedTemplate
  );

  if (loadingCurrent) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
        <div className="flex items-center gap-2">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
            Loading payment instructions...
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-lg shadow p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
          Payment Instructions
        </h2>
        {canEditData && !showForm && (
          <Button
            variant="secondary"
            leftIcon={current ? <Edit /> : <Plus />}
            onClick={current ? handleChange : () => setShowForm(true)}
          >
            {current ? 'Change' : 'Set Instructions'}
          </Button>
        )}
      </div>

      {/* Current Payment Instruction */}
      {current ? (
        <div className="space-y-3">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-lg bg-[#f1f3f9] dark:bg-[#1e2130]">
              {current.paymentMethod === 'BANK_TRANSFER' ||
              current.paymentMethod === 'DIRECT_DEBIT' ? (
                <Building className="h-5 w-5 text-primary-500 dark:text-primary-300" />
              ) : (
                <CreditCard className="h-5 w-5 text-primary-500 dark:text-primary-300" />
              )}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {current.name || 'Unnamed'}
                </span>
                <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]">
                  {PaymentMethodLabels[
                    current.paymentMethod as PaymentMethod
                  ] || current.paymentMethod}
                </span>
                {!current.isCustom && current.paymentInstructionIdentifier && (
                  <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300">
                    Template
                  </span>
                )}
                {current.isCustom && (
                  <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300">
                    Custom
                  </span>
                )}
              </div>
              {current.description && (
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                  {current.description}
                </p>
              )}
              {current.iban && (
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1 font-mono">
                  IBAN: {current.iban.replace(/(.{4})/g, '$1 ').trim()}
                </p>
              )}
              {current.accountHolderName && (
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Holder: {current.accountHolderName}
                </p>
              )}
              {current.paymentReference && (
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Ref: {current.paymentReference}
                </p>
              )}
              <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-2">
                Effective from {formatDate(current.effectiveFrom)}
              </p>
            </div>
          </div>

          {/* History Toggle */}
          {history.length > 1 && (
            <button
              onClick={() => setShowHistory(!showHistory)}
              className="flex items-center gap-1 text-sm text-primary-500 dark:text-primary-300 hover:underline mt-2"
            >
              <Clock className="h-3.5 w-3.5" />
              {showHistory ? 'Hide' : 'View'} History ({history.length} entries)
              {showHistory ? (
                <ChevronUp className="h-3.5 w-3.5" />
              ) : (
                <ChevronDown className="h-3.5 w-3.5" />
              )}
            </button>
          )}
        </div>
      ) : (
        !showForm && (
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
            No payment instructions configured for this contract.
          </p>
        )
      )}

      {/* History Timeline */}
      {showHistory && !loadingHistory && (
        <div className="mt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f] pt-4">
          <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-3">
            Payment Instruction History
          </h3>
          <div className="space-y-3">
            {history.map((entry, index) => (
              <HistoryEntry
                key={entry.identifier}
                entry={entry}
                isCurrent={index === 0 && !entry.effectiveTo}
                formatDate={formatDate}
                canDelete={canEditData && !!entry.effectiveTo}
                onDelete={() => {
                  deleteMutation.mutate(entry.identifier);
                }}
              />
            ))}
          </div>
        </div>
      )}

      {/* Create/Change Form */}
      {showForm && (
        <div className="mt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f] pt-4">
          <h3 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-3">
            {editingId
              ? 'Change Payment Instructions'
              : 'Set Payment Instructions'}
          </h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Template vs Custom Toggle */}
            <div className="flex rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
              <button
                type="button"
                className={`flex-1 py-2 text-sm font-medium ${
                  useTemplate
                    ? 'bg-primary-500 text-white'
                    : 'bg-white dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]'
                }`}
                onClick={() => setUseTemplate(true)}
              >
                Use Template
              </button>
              <button
                type="button"
                className={`flex-1 py-2 text-sm font-medium ${
                  !useTemplate
                    ? 'bg-primary-500 text-white'
                    : 'bg-white dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]'
                }`}
                onClick={() => setUseTemplate(false)}
              >
                Custom
              </button>
            </div>

            {useTemplate ? (
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Select Template *
                </label>
                <select
                  required
                  value={selectedTemplate}
                  onChange={(e) => setSelectedTemplate(e.target.value)}
                  className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                >
                  <option value="">Choose a payment method...</option>
                  {templates.map((t) => (
                    <option key={t.identifier} value={t.identifier}>
                      {t.name} (
                      {PaymentMethodLabels[t.paymentMethod as PaymentMethod] ||
                        t.paymentMethod}
                      ){t.isDefault ? ' - Default' : ''}
                    </option>
                  ))}
                </select>
                {selectedTemplateData && (
                  <div className="mt-2 p-3 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-lg text-sm">
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {selectedTemplateData.name}
                    </p>
                    {selectedTemplateData.description && (
                      <p className="text-[#6b7194] dark:text-[#8b90a8] mt-1">
                        {selectedTemplateData.description}
                      </p>
                    )}
                    {selectedTemplateData.iban && (
                      <p className="text-[#6b7194] dark:text-[#8b90a8] font-mono mt-1">
                        IBAN: {selectedTemplateData.iban}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      Name *
                    </label>
                    <input
                      type="text"
                      required
                      value={customName}
                      onChange={(e) => setCustomName(e.target.value)}
                      className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      Method *
                    </label>
                    <select
                      required
                      value={customPaymentMethod}
                      onChange={(e) => setCustomPaymentMethod(e.target.value)}
                      className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                    >
                      {Object.entries(PaymentMethodLabels).map(
                        ([val, label]) => (
                          <option key={val} value={val}>
                            {label}
                          </option>
                        )
                      )}
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                    Description
                  </label>
                  <textarea
                    value={customDescription}
                    onChange={(e) => setCustomDescription(e.target.value)}
                    rows={2}
                    className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                  />
                </div>
                {(customPaymentMethod === 'BANK_TRANSFER' ||
                  customPaymentMethod === 'DIRECT_DEBIT') && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                        Bank Name
                      </label>
                      <input
                        type="text"
                        value={customBankName}
                        onChange={(e) => setCustomBankName(e.target.value)}
                        className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                        Account Holder
                      </label>
                      <input
                        type="text"
                        value={customAccountHolderName}
                        onChange={(e) =>
                          setCustomAccountHolderName(e.target.value)
                        }
                        className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                        IBAN
                      </label>
                      <input
                        type="text"
                        value={customIban}
                        onChange={(e) => setCustomIban(e.target.value)}
                        maxLength={34}
                        className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                        BIC/SWIFT
                      </label>
                      <input
                        type="text"
                        value={customBicSwift}
                        onChange={(e) => setCustomBicSwift(e.target.value)}
                        maxLength={11}
                        className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
                      />
                    </div>
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                    Payment Reference
                  </label>
                  <input
                    type="text"
                    value={customPaymentReference}
                    onChange={(e) => setCustomPaymentReference(e.target.value)}
                    className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Effective From *
              </label>
              <input
                type="date"
                required
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="w-full md:w-1/2 rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Notes (reason for change)
              </label>
              <RichTextEditor
                value={notes}
                onChange={setNotes}
                placeholder="Optional reason..."
              />
            </div>

            <div className="flex gap-2 justify-end">
              <Button type="button" variant="secondary" onClick={resetForm}>
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {createMutation.isPending || updateMutation.isPending
                  ? 'Saving...'
                  : editingId
                    ? 'Change Instructions'
                    : 'Set Instructions'}
              </Button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

// History entry sub-component
const HistoryEntry = ({
  entry,
  isCurrent,
  formatDate,
  canDelete,
  onDelete,
}: {
  entry: ContractPaymentInstructionResponse;
  isCurrent: boolean;
  formatDate: (date: string) => string;
  canDelete: boolean;
  onDelete: () => void;
}) => {
  return (
    <div
      className={`relative pl-6 pb-3 border-l-2 ${
        isCurrent
          ? 'border-primary-500 dark:border-primary-300'
          : 'border-[#e2e6f0] dark:border-[#2a2e3f]'
      }`}
    >
      <div
        className={`absolute left-[-5px] top-1 h-2 w-2 rounded-full ${
          isCurrent
            ? 'bg-primary-500 dark:bg-primary-300'
            : 'bg-[#9ca0b8] dark:bg-[#5c6180]'
        }`}
      />
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span
              className={`text-sm font-medium ${
                isCurrent
                  ? 'text-[#1a1d2e] dark:text-[#eef0f6]'
                  : 'text-[#6b7194] dark:text-[#8b90a8]'
              }`}
            >
              {entry.name || 'Unnamed'}
            </span>
            <span className="px-1.5 py-0.5 text-xs rounded bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]">
              {PaymentMethodLabels[entry.paymentMethod as PaymentMethod] ||
                entry.paymentMethod}
            </span>
            {isCurrent && (
              <span className="px-1.5 py-0.5 text-xs rounded bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 font-medium">
                Current
              </span>
            )}
          </div>
          <p className="text-xs text-[#9ca0b8] dark:text-[#5c6180] mt-1">
            {formatDate(entry.effectiveFrom)}
            {entry.effectiveTo
              ? ` - ${formatDate(entry.effectiveTo)}`
              : ' - Present'}
          </p>
          {entry.notes && (
            <RichTextDisplay
              content={entry.notes}
              className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-0.5 italic"
            />
          )}
        </div>
        {canDelete && (
          <button
            onClick={onDelete}
            className="p-1 rounded hover:bg-red-50 dark:hover:bg-red-900/20 text-[#9ca0b8] hover:text-red-600"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  );
};
