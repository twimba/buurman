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
import { Button, RichTextEditor, RichTextDisplay } from '@buurman/ui';
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
import { ContractStatus } from '../../types/contract';
import { useTranslation } from 'react-i18next';

interface Props {
  contractIdentifier: string;
  readOnly?: boolean;
  contractStatus?: ContractStatus;
  contractStartDate?: string;
  contractSignedDate?: string;
}

const computeMinEffectiveDate = (
  contractStatus?: ContractStatus,
  contractStartDate?: string,
  contractSignedDate?: string
): string => {
  const today = new Date().toISOString().split('T')[0];
  if (contractStatus === ContractStatus.DRAFT) {
    const candidates = [today];
    if (contractStartDate) {
      candidates.push(contractStartDate);
    }
    if (contractSignedDate) {
      candidates.push(contractSignedDate);
    }
    candidates.sort();
    return candidates[0];
  }
  return today;
};

export const ContractPaymentInstructionSection = ({
  contractIdentifier,
  readOnly = false,
  contractStatus,
  contractStartDate,
  contractSignedDate,
}: Props) => {
  const { t } = useTranslation('contracts');
  const { canEditData } = useTeam();
  const canModify = canEditData && !readOnly;
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
  const minEffectiveDate = computeMinEffectiveDate(
    contractStatus,
    contractStartDate,
    contractSignedDate
  );
  const [effectiveFrom, setEffectiveFrom] = useState(minEffectiveDate);
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
  const [customAccountNumber, setCustomAccountNumber] = useState('');
  const [customRoutingNumber, setCustomRoutingNumber] = useState('');
  const [customPaymentReference, setCustomPaymentReference] = useState('');

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setUseTemplate(true);
    setSelectedTemplate('');
    setEffectiveFrom(minEffectiveDate);
    setNotes('');
    setCustomName('');
    setCustomDescription('');
    setCustomPaymentMethod('BANK_TRANSFER');
    setCustomBankName('');
    setCustomAccountHolderName('');
    setCustomIban('');
    setCustomBicSwift('');
    setCustomAccountNumber('');
    setCustomRoutingNumber('');
    setCustomPaymentReference('');
  };

  const submitForm = () => {
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
            customAccountNumber: customAccountNumber || undefined,
            customRoutingNumber: customRoutingNumber || undefined,
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  const handleChange = () => {
    if (current) {
      setEditingId(current.identifier);
    }
    setShowForm(true);
  };

  const selectedTemplateData = templates.find(
    (tpl) => tpl.identifier === selectedTemplate
  );

  if (loadingCurrent) {
    return (
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center gap-2">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="text-sm text-text-secondary">
            {t('overview.paymentInstructions.loading')}
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-text-primary flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-text-muted " />
          {t('overview.paymentInstructions.title')}
        </h2>
        {canModify && !showForm && (
          <Button
            variant="secondary"
            leftIcon={current ? <Edit /> : <Plus />}
            onClick={current ? handleChange : () => setShowForm(true)}
          >
            {current
              ? t('common:buttons.edit')
              : t('overview.paymentInstruction')}
          </Button>
        )}
      </div>

      {/* Current Payment Instruction */}
      {current ? (
        <div className="space-y-3">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-lg bg-surface-inset">
              {current.paymentMethod === 'BANK_TRANSFER' ||
              current.paymentMethod === 'DIRECT_DEBIT' ||
              current.paymentMethod === 'IDEAL_WERO' ||
              current.paymentMethod === 'ZELLE' ? (
                <Building className="h-5 w-5 text-primary-500 dark:text-primary-300" />
              ) : (
                <CreditCard className="h-5 w-5 text-primary-500 dark:text-primary-300" />
              )}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-text-primary">
                  {current.name || 'Unnamed'}
                </span>
                <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-surface-inset text-text-secondary">
                  {PaymentMethodLabels[
                    current.paymentMethod as PaymentMethod
                  ] || current.paymentMethod}
                </span>
                {!current.isCustom && current.paymentInstructionIdentifier && (
                  <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-info-bg text-info-text">
                    {t('overview.paymentInstructions.template')}
                  </span>
                )}
                {current.isCustom && (
                  <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-info-bg text-info-text">
                    {t('overview.paymentInstructions.custom')}
                  </span>
                )}
              </div>
              {current.description && (
                <p className="text-sm text-text-secondary mt-1">
                  {current.description}
                </p>
              )}
              {current.iban && (
                <p className="text-sm text-text-secondary mt-1 font-mono">
                  {t('overview.paymentInstructions.iban')}: {current.iban.replace(/(.{4})/g, '$1').trim()}
                </p>
              )}
              {current.accountNumber && (
                <p className="text-sm text-text-secondary mt-1 font-mono">
                  {t('overview.paymentInstructions.account')}: {current.accountNumber}
                  {current.routingNumber &&
                    ` / ${t('overview.paymentInstructions.routing')}: ${current.routingNumber}`}
                </p>
              )}
              {current.accountHolderName && (
                <p className="text-sm text-text-secondary">
                  {t('overview.paymentInstructions.holder')}: {current.accountHolderName}
                </p>
              )}
              {current.paymentReference && (
                <p className="text-sm text-text-secondary">
                  {t('overview.paymentInstructions.ref')}: {current.paymentReference}
                </p>
              )}
              <p className="text-xs text-text-muted mt-2">
                {t('overview.paymentInstructions.effectiveFrom', { date: formatDate(current.effectiveFrom) })}
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
              {showHistory ? t('overview.paymentInstructions.hideHistory') : t('overview.paymentInstructions.viewHistory')}{' '}{t('overview.paymentInstructions.historyCount', { count: history.length })}
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
          <p className="text-sm text-text-secondary">
            {t('overview.noPaymentInstruction')}
          </p>
        )
      )}

      {/* History Timeline */}
      {showHistory && !loadingHistory && (
        <div className="mt-4 border-t border-border-default pt-4">
          <h3 className="text-sm font-semibold text-text-secondary mb-3">
            {t('overview.paymentInstructions.historyTitle')}
          </h3>
          <div className="space-y-3">
            {history.map((entry, index) => (
              <HistoryEntry
                key={entry.identifier}
                entry={entry}
                isCurrent={index === 0 && !entry.effectiveTo}
                formatDate={formatDate}
                canDelete={
                  canModify &&
                  !!entry.effectiveTo &&
                  !(
                    contractStatus !== ContractStatus.DRAFT &&
                    entry.effectiveFrom < new Date().toISOString().split('T')[0]
                  )
                }
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
        <div className="mt-4 border-t border-border-default pt-4">
          <h3 className="text-sm font-semibold text-text-secondary mb-3">
            {editingId
              ? t('overview.paymentInstructions.changeTitle')
              : t('overview.paymentInstructions.setTitle')}
          </h3>
          <form
            onSubmit={handleSubmit}
            onKeyDown={handleCmdEnter}
            className="space-y-4"
          >
            {/* Template vs Custom Toggle */}
            <div className="flex rounded-lg border border-border-default overflow-hidden">
              <button
                type="button"
                className={`flex-1 py-2 text-sm font-medium ${
                  useTemplate
                    ? 'bg-primary-500 text-white'
                    : 'bg-surface-card text-text-secondary'
                }`}
                onClick={() => setUseTemplate(true)}
              >
                {t('overview.paymentInstructions.useTemplate')}
              </button>
              <button
                type="button"
                className={`flex-1 py-2 text-sm font-medium ${
                  !useTemplate
                    ? 'bg-primary-500 text-white'
                    : 'bg-surface-card text-text-secondary'
                }`}
                onClick={() => setUseTemplate(false)}
              >
                {t('overview.paymentInstructions.customOption')}
              </button>
            </div>

            {useTemplate ? (
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('overview.paymentInstructions.selectTemplate')}
                </label>
                <select
                  required
                  value={selectedTemplate}
                  onChange={(e) => setSelectedTemplate(e.target.value)}
                  className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
                >
                  <option value="">{t('overview.paymentInstructions.choosePlaceholder')}</option>
                  {templates.map((tpl) => (
                    <option key={tpl.identifier} value={tpl.identifier}>
                      {tpl.name} (
                      {PaymentMethodLabels[tpl.paymentMethod as PaymentMethod] ||
                        tpl.paymentMethod}
                      ){tpl.isDefault ? ` - ${t('overview.paymentInstructions.default')}` : ''}
                    </option>
                  ))}
                </select>
                {selectedTemplateData && (
                  <div className="mt-2 p-3 bg-surface-inset rounded-lg text-sm">
                    <p className="font-medium text-text-primary">
                      {selectedTemplateData.name}
                    </p>
                    {selectedTemplateData.description && (
                      <p className="text-text-secondary mt-1">
                        {selectedTemplateData.description}
                      </p>
                    )}
                    {selectedTemplateData.iban && (
                      <p className="text-text-secondary font-mono mt-1">
                        {t('overview.paymentInstructions.iban')}: {selectedTemplateData.iban}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-text-secondary mb-1">
                      {t('overview.paymentInstructions.name')}
                    </label>
                    <input
                      type="text"
                      required
                      value={customName}
                      onChange={(e) => setCustomName(e.target.value)}
                      className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-text-secondary mb-1">
                      {t('overview.paymentInstructions.method')}
                    </label>
                    <select
                      required
                      value={customPaymentMethod}
                      onChange={(e) => setCustomPaymentMethod(e.target.value)}
                      className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
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
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    {t('overview.paymentInstructions.description')}
                  </label>
                  <textarea
                    value={customDescription}
                    onChange={(e) => setCustomDescription(e.target.value)}
                    rows={2}
                    className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
                  />
                </div>
                {(customPaymentMethod === 'BANK_TRANSFER' ||
                  customPaymentMethod === 'DIRECT_DEBIT' ||
                  customPaymentMethod === 'IDEAL_WERO' ||
                  customPaymentMethod === 'ZELLE') && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1">
                        {t('overview.paymentInstructions.bankName')}
                      </label>
                      <input
                        type="text"
                        value={customBankName}
                        onChange={(e) => setCustomBankName(e.target.value)}
                        className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1">
                        {t('overview.paymentInstructions.accountHolder')}
                      </label>
                      <input
                        type="text"
                        value={customAccountHolderName}
                        onChange={(e) =>
                          setCustomAccountHolderName(e.target.value)
                        }
                        className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1">
                        {t('overview.paymentInstructions.ibanField')}
                      </label>
                      <input
                        type="text"
                        value={customIban}
                        onChange={(e) => setCustomIban(e.target.value)}
                        maxLength={34}
                        className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1">
                        {t('overview.paymentInstructions.bicSwift')}
                      </label>
                      <input
                        type="text"
                        value={customBicSwift}
                        onChange={(e) => setCustomBicSwift(e.target.value)}
                        maxLength={11}
                        className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1">
                        {t('overview.paymentInstructions.accountNumber')}
                      </label>
                      <input
                        type="text"
                        value={customAccountNumber}
                        onChange={(e) => setCustomAccountNumber(e.target.value)}
                        className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text-secondary mb-1">
                        {t('overview.paymentInstructions.routingNumber')}
                      </label>
                      <input
                        type="text"
                        value={customRoutingNumber}
                        onChange={(e) => setCustomRoutingNumber(e.target.value)}
                        className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary font-mono"
                      />
                    </div>
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    {t('overview.paymentInstructions.paymentReference')}
                  </label>
                  <input
                    type="text"
                    value={customPaymentReference}
                    onChange={(e) => setCustomPaymentReference(e.target.value)}
                    className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('overview.paymentInstructions.effectiveFromField')}
              </label>
              <input
                type="date"
                required
                min={minEffectiveDate}
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="w-full md:w-1/2 rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                {t('overview.paymentInstructions.notesField')}
              </label>
              <RichTextEditor
                value={notes}
                onChange={setNotes}
                placeholder={t('overview.paymentInstructions.notesPlaceholder')}
                onSubmit={submitForm}
              />
            </div>

            <div className="flex gap-2 justify-end">
              <Button type="button" variant="secondary" onClick={resetForm}>
                {t('common:buttons.cancel')}
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {createMutation.isPending || updateMutation.isPending
                  ? t('overview.paymentInstructions.saving')
                  : editingId
                    ? t('overview.paymentInstructions.changeInstructions')
                    : t('overview.paymentInstructions.setInstructions')}
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
  const { t } = useTranslation('contracts');
  return (
    <div
      className={`relative pl-6 pb-3 border-l-2 ${
        isCurrent
          ? 'border-primary-500 dark:border-primary-300'
          : 'border-border-default'
      }`}
    >
      <div
        className={`absolute left-[-5px] top-1 h-2 w-2 rounded-full ${
          isCurrent
            ? 'bg-primary-500 dark:bg-primary-300'
            : 'bg-neutral-400 dark:bg-neutral-500'
        }`}
      />
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span
              className={`text-sm font-medium ${
                isCurrent ? 'text-text-primary' : 'text-text-secondary'
              }`}
            >
              {entry.name || 'Unnamed'}
            </span>
            <span className="px-1.5 py-0.5 text-xs rounded bg-surface-inset text-text-secondary">
              {PaymentMethodLabels[entry.paymentMethod as PaymentMethod] ||
                entry.paymentMethod}
            </span>
            {isCurrent && (
              <span className="px-1.5 py-0.5 text-xs rounded bg-success-bg text-success-text font-medium">
                {t('overview.paymentInstructions.current')}
              </span>
            )}
          </div>
          <p className="text-xs text-text-muted mt-1">
            {formatDate(entry.effectiveFrom)}
            {entry.effectiveTo
              ? ` - ${formatDate(entry.effectiveTo)}`
              : ` - ${t('overview.paymentInstructions.present')}`}
          </p>
          {entry.notes && (
            <RichTextDisplay
              content={entry.notes}
              className="text-xs text-text-secondary mt-0.5 italic"
            />
          )}
        </div>
        {canDelete && (
          <button
            onClick={onDelete}
            className="p-1 rounded hover:bg-error-bg text-text-muted hover:text-error-text"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  );
};
