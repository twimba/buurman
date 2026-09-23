import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useContractExtensions } from '@/hooks/useContractExtensionHooks';
import {
  useAddContractParty,
  useRemoveContractParty,
} from '@/hooks/useContractHooks';
import { ContractPaymentInstructionSection } from '@/components/contracts/ContractPaymentInstructionSection';
import { CalendarFeedResponseFeedType as CalendarFeedType } from '@/generated/models';
import { CalendarFeedButton } from '@/components/common/CalendarFeedPopover';
import { ConfirmDialog, RichTextDisplay } from '@buurman/ui';
import { RentTimeline } from '@/components/contracts/RentTimeline';
import { useTeam } from '@/context/TeamContext';
import {
  Home,
  User,
  ChevronDown,
  ChevronUp,
  Calendar,
  DollarSign,
  Plus,
  Trash2,
  Repeat,
} from 'lucide-react';
import { useFormatDate } from '@/hooks/useFormatDate';
import {
  AddContractPartyRequest,
  ContractResponse,
  ContractPartyRole,
} from '@/types/contract';
import { ContractPartyResponseRole } from '@/generated/models';
import { ContactSelector } from '@/components/common/ContactSelector';
import { PhoneInput } from '@/components/common/PhoneInput';
import type { CreateContactRequest } from '@/types/contact';
import CountryMetadataForm, {
  useCountryName,
} from '@/components/contracts/CountryMetadataForm';
import { useTranslation } from 'react-i18next';

interface ContractOverviewTabProps {
  contract: ContractResponse;
  contractId: string;
}

export const ContractOverviewTab = ({
  contract,
  contractId,
}: ContractOverviewTabProps) => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const countryName = useCountryName(contract.countryCode);

  const [isMetadataExpanded, setIsMetadataExpanded] = useState(false);
  const [showAddParty, setShowAddParty] = useState(false);
  const [addPartyMode, setAddPartyMode] = useState<'select' | 'create'>(
    'select'
  );
  const [addPartyRole, setAddPartyRole] = useState<ContractPartyResponseRole>(
    ContractPartyResponseRole.EXTRA_TENANT
  );
  const [selectedContactId, setSelectedContactId] = useState('');
  const [newContactData, setNewContactData] = useState<CreateContactRequest>({
    contactType: 'INDIVIDUAL',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
  });
  const [removePartyTarget, setRemovePartyTarget] = useState<string | null>(
    null
  );

  const { data: extensionsPage } = useContractExtensions(contractId);
  const activeExtensions = useMemo(
    () =>
      (extensionsPage?.content ?? [])
        .filter((e) => e.status === 'ACTIVE' || e.status === 'SUPERSEDED')
        .sort((a, b) => a.extensionNumber - b.extensionNumber),
    [extensionsPage]
  );

  const addPartyMutation = useAddContractParty(contractId);
  const removePartyMutation = useRemoveContractParty(contractId);

  const handleAddParty = () => {
    const request: AddContractPartyRequest = {
      role: addPartyRole,
      ...(addPartyMode === 'select'
        ? { contactIdentifier: selectedContactId }
        : { newContact: newContactData }),
    };
    addPartyMutation.mutate(request, {
      onSuccess: () => {
        setShowAddParty(false);
        setSelectedContactId('');
        setNewContactData({
          contactType: 'INDIVIDUAL',
          firstName: '',
          lastName: '',
          email: '',
          phone: '',
        });
        setAddPartyMode('select');
        setAddPartyRole(ContractPartyResponseRole.EXTRA_TENANT);
      },
    });
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Property and Contact */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-text-primary">
            {t('overview.contractParties')}
          </h2>
          {canEditData && (
            <button
              onClick={() => setShowAddParty(!showAddParty)}
              className="flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600 transition-colors"
            >
              <Plus className="h-4 w-4" />
              {t('overview.addParty')}
            </button>
          )}
        </div>
        <div className="space-y-4">
          <div className="flex items-start gap-3">
            <Home className="h-5 w-5 text-text-muted mt-1" />
            <div className="flex-1">
              <p className="text-sm text-text-secondary">
                {t('overview.property')}
              </p>
              <button
                onClick={() =>
                  navigate(`/properties/${contract.property.identifier}`)
                }
                className="font-medium text-primary-500 hover:underline text-left"
              >
                {contract.property.street}, {contract.property.city}
              </button>
              <p className="text-xs text-text-secondary">
                #{contract.property.identifier}
              </p>
            </div>
          </div>
          {/* Contract Parties */}
          {contract.parties?.map((party) => (
            <div
              key={party.identifier}
              className="flex items-start gap-3 group"
            >
              <User className="h-5 w-5 text-text-muted mt-1" />
              <div className="flex-1">
                <p className="text-sm text-text-secondary">
                  {t(`enums.partyRoles.${party.role}`, party.role)}
                </p>
                <button
                  onClick={() =>
                    navigate(`/contacts/${party.contact.identifier}`)
                  }
                  className="font-medium text-primary-500 hover:underline text-left"
                >
                  {party.contact.firstName} {party.contact.lastName}
                </button>
                <p className="text-xs text-text-secondary">
                  #{party.contact.identifier}
                </p>
              </div>
              {canEditData && (
                <button
                  onClick={() => setRemovePartyTarget(party.identifier)}
                  className="opacity-0 group-hover:opacity-100 text-text-muted hover:text-error-text transition-all mt-1"
                  title={t('overview.removeParty.title')}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              )}
            </div>
          ))}

          {/* Add Party Inline Form */}
          {showAddParty && (
            <div className="border border-border-strong rounded-lg p-4 space-y-3 bg-surface-page">
              <div>
                <label className="block text-xs font-medium text-text-secondary mb-1">
                  {t('overview.addPartyForm.role')}
                </label>
                <select
                  value={addPartyRole}
                  onChange={(e) =>
                    setAddPartyRole(e.target.value as ContractPartyResponseRole)
                  }
                  className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                >
                  {Object.values(ContractPartyRole).map((value) => (
                    <option key={value} value={value}>
                      {t(`enums.partyRoles.${value}`, value)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex gap-2 text-xs">
                <button
                  type="button"
                  onClick={() => setAddPartyMode('select')}
                  className={`px-3 py-1.5 rounded-full transition-colors ${
                    addPartyMode === 'select'
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-inset text-text-secondary hover:bg-surface-card'
                  }`}
                >
                  {t('overview.addPartyForm.selectExisting')}
                </button>
                <button
                  type="button"
                  onClick={() => setAddPartyMode('create')}
                  className={`px-3 py-1.5 rounded-full transition-colors ${
                    addPartyMode === 'create'
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-inset text-text-secondary hover:bg-surface-card'
                  }`}
                >
                  {t('overview.addPartyForm.createNew')}
                </button>
              </div>

              {addPartyMode === 'select' ? (
                <div>
                  <label className="block text-xs font-medium text-text-secondary mb-1">
                    {t('overview.addPartyForm.contact')}
                  </label>
                  <ContactSelector
                    value={selectedContactId}
                    onChange={setSelectedContactId}
                  />
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">
                      {t('overview.addPartyForm.firstName')}{' '}
                      <span className="text-error-text">*</span>
                    </label>
                    <input
                      type="text"
                      value={newContactData.firstName ?? ''}
                      onChange={(e) =>
                        setNewContactData({
                          ...newContactData,
                          firstName: e.target.value,
                        })
                      }
                      className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                      placeholder="John"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">
                      {t('overview.addPartyForm.lastName')}
                    </label>
                    <input
                      type="text"
                      value={newContactData.lastName ?? ''}
                      onChange={(e) =>
                        setNewContactData({
                          ...newContactData,
                          lastName: e.target.value,
                        })
                      }
                      className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                      placeholder="Doe"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">
                      {t('overview.addPartyForm.email')}
                    </label>
                    <input
                      type="email"
                      value={newContactData.email ?? ''}
                      onChange={(e) =>
                        setNewContactData({
                          ...newContactData,
                          email: e.target.value,
                        })
                      }
                      className="w-full border border-border-strong rounded px-3 py-2 bg-surface-card text-text-primary text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                      placeholder="john@example.com"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-text-secondary mb-1">
                      {t('overview.addPartyForm.phone')}
                    </label>
                    <PhoneInput
                      value={newContactData.phone ?? null}
                      onChange={(e164) =>
                        setNewContactData({
                          ...newContactData,
                          phone: e164 ?? '',
                        })
                      }
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setShowAddParty(false)}
                  className="px-3 py-1.5 text-sm text-text-secondary hover:text-text-primary transition-colors"
                >
                  {t('common:buttons.cancel')}
                </button>
                <button
                  type="button"
                  onClick={handleAddParty}
                  disabled={
                    addPartyMutation.isPending ||
                    (addPartyMode === 'select' && !selectedContactId) ||
                    (addPartyMode === 'create' && !newContactData.firstName)
                  }
                  className="px-4 py-1.5 text-sm bg-primary-500 text-white rounded hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {addPartyMutation.isPending
                    ? t('overview.addPartyForm.adding')
                    : t('overview.addPartyForm.add')}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Remove Party Confirmation */}
      {removePartyTarget && (
        <ConfirmDialog
          title={t('overview.removeParty.title')}
          message={t('overview.removeParty.message')}
          confirmLabel={t('overview.removeParty.confirm')}
          variant="danger"
          isLoading={removePartyMutation.isPending}
          onConfirm={() => {
            removePartyMutation.mutate(removePartyTarget, {
              onSuccess: () => setRemovePartyTarget(null),
            });
          }}
          onCancel={() => setRemovePartyTarget(null)}
        />
      )}

      {/* Contract Dates */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-text-primary">
            {t('overview.importantDates')}
          </h2>
          {contract && (
            <CalendarFeedButton
              feedType={CalendarFeedType.CONTRACT}
              entityIdentifier={contract.identifier}
            />
          )}
        </div>
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <Calendar className="h-5 w-5 text-text-muted" />
            <div>
              <p className="text-sm text-text-secondary">
                {t('overview.startDate')}
              </p>
              <p className="font-medium text-text-primary">
                {formatDate(contract.startDate)}
              </p>
            </div>
          </div>
          {contract.signedDate && (
            <div className="flex items-center gap-3">
              <Calendar className="h-5 w-5 text-text-muted" />
              <div>
                <p className="text-sm text-text-secondary">
                  {t('overview.signedDate')}
                </p>
                <p className="font-medium text-text-primary">
                  {formatDate(contract.signedDate)}
                </p>
              </div>
            </div>
          )}
          {(contract.endDate || contract.effectiveEndDate) &&
            activeExtensions.length === 0 && (
              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-text-muted" />
                <div>
                  <p className="text-sm text-text-secondary">
                    {t('overview.endDate')}
                  </p>
                  <p className="font-medium text-text-primary">
                    {formatDate(
                      (contract.endDate ?? contract.effectiveEndDate) as string
                    )}
                  </p>
                </div>
              </div>
            )}
          {activeExtensions.length > 0 && contract.endDate && (
            <div>
              <div className="flex items-center gap-2 mb-3">
                <Calendar className="h-5 w-5 text-text-muted" />
                <p className="text-sm text-text-secondary">
                  {t('overview.endDate')}
                  <span className="ml-1.5 px-1.5 py-0.5 text-[10px] font-medium rounded-full bg-info-bg text-info-text">
                    {t('overview.extended', { count: contract.extensionCount })}
                  </span>
                </p>
              </div>
              <div className="relative pl-4 ml-2.5">
                {/* Vertical rail */}
                <div className="absolute left-[5px] top-[6px] bottom-[6px] w-px bg-gradient-to-b from-border-strong via-info-text/30 to-info-text/60" />

                {/* Original end date node */}
                <div className="relative flex items-start gap-3 pb-4">
                  <div className="absolute left-[-13px] top-[5px] w-[7px] h-[7px] rounded-full border-2 border-border-strong bg-surface-card z-10" />
                  <div className="min-w-0">
                    <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
                      {t('overview.original')}
                    </p>
                    <p className="text-sm font-medium text-text-secondary line-through decoration-text-muted/40">
                      {formatDate(contract.endDate)}
                    </p>
                  </div>
                </div>

                {/* Extension nodes */}
                {activeExtensions.map((ext, index) => {
                  const isLatest = index === activeExtensions.length - 1;
                  return (
                    <div
                      key={ext.identifier}
                      className={`relative flex items-start gap-3 ${isLatest ? '' : 'pb-4'}`}
                    >
                      {/* Node dot */}
                      <div
                        className={`absolute left-[-13px] z-10 ${
                          isLatest
                            ? 'top-[3px] w-[11px] h-[11px] rounded-full bg-info-text shadow-[0_0_0_3px_var(--color-info-bg)]'
                            : 'top-[5px] w-[7px] h-[7px] rounded-full bg-info-text/60 border-2 border-info-bg'
                        }`}
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="text-[11px] font-medium uppercase tracking-wider text-text-muted">
                            {t('overview.extension', {
                              number: ext.extensionNumber,
                            })}
                          </span>
                          {ext.triggerType === 'AUTO' && (
                            <span className="inline-flex items-center px-1 py-px text-[9px] font-semibold uppercase tracking-wider rounded bg-info-bg text-info-text">
                              {t('overview.auto')}
                            </span>
                          )}
                        </div>
                        <p
                          className={`text-sm font-semibold ${isLatest ? 'text-text-primary' : 'text-text-secondary line-through decoration-text-muted/40'}`}
                        >
                          {ext.newEndDate ? formatDate(ext.newEndDate) : '—'}
                        </p>
                        {ext.activatedAt && (
                          <p className="text-[10px] text-text-muted mt-0.5">
                            {t('overview.activated', {
                              date: formatDate(ext.activatedAt),
                            })}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Financial Terms */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">
          {t('overview.financialTerms')}
        </h2>
        <div className="space-y-4">
          <RentTimeline
            contractIdentifier={contractId}
            contractStatus={contract.status}
            currency={contract.rentAmountCurrency}
            currentRentAmount={contract.rentAmount}
            currentComponents={contract.rentComponents ?? []}
            paymentFrequency={contract.paymentFrequency}
            documentLanguages={contract.documentLanguages}
            countryCode={contract.countryCode}
          />
          {contract.rentComponents && contract.rentComponents.length > 0 && (
            <div>
              <h4 className="text-sm font-medium text-text-secondary mb-2">
                {t('overview.rentBreakdown')}
              </h4>
              <div className="bg-surface-secondary rounded-lg p-3 space-y-2">
                {contract.rentComponents.map((comp) => (
                  <div
                    key={comp.identifier}
                    className="flex justify-between items-center text-sm"
                  >
                    <span className="text-text-secondary">
                      {comp.componentTypeDisplayName}
                      {comp.description && (
                        <span className="text-text-tertiary ml-1">
                          ({comp.description})
                        </span>
                      )}
                    </span>
                    <span className="font-medium text-text-primary">
                      {comp.currency} {comp.amount.toFixed(2)}
                    </span>
                  </div>
                ))}
                <div className="flex justify-between items-center text-sm font-semibold pt-2 border-t border-border-default">
                  <span>{t('overview.total')}</span>
                  <span>
                    {contract.rentAmountCurrency}{' '}
                    {contract.rentAmount.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          )}
          {contract.depositAmount && (
            <div className="flex items-center gap-3">
              <DollarSign className="h-5 w-5 text-text-muted " />
              <div>
                <p className="text-sm text-text-secondary">
                  {t('overview.deposit')}
                </p>
                <p className="font-medium text-text-primary">
                  {contract.depositAmountCurrency ??
                    contract.rentAmountCurrency}{' '}
                  {contract.depositAmount.toFixed(2)}
                </p>
              </div>
            </div>
          )}
          {contract.securityDeposit && (
            <div className="flex items-center gap-3">
              <DollarSign className="h-5 w-5 text-text-muted " />
              <div>
                <p className="text-sm text-text-secondary">
                  {t('overview.securityDeposit')}
                </p>
                <p className="font-medium text-text-primary">
                  {contract.securityDepositCurrency ??
                    contract.rentAmountCurrency}{' '}
                  {contract.securityDeposit.toFixed(2)}
                </p>
              </div>
            </div>
          )}
          {contract.paymentDueDay && (
            <div>
              <p className="text-sm text-text-secondary">
                {t('overview.paymentDueDay')}
              </p>
              <p className="font-medium text-text-primary">
                {t('overview.dayOfPeriod', { day: contract.paymentDueDay })}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Payment Instructions */}
      <ContractPaymentInstructionSection
        contractIdentifier={contractId}
        contractStatus={contract.status}
      />

      {/* Renewal Configuration */}
      {contract.renewalMode && contract.renewalMode !== 'NONE' && (
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <div className="flex items-center gap-2 mb-4">
            <Repeat className="h-5 w-5 text-text-muted" />
            <h2 className="text-lg font-semibold text-text-primary">
              {t('overview.renewalConfig.title')}
            </h2>
          </div>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-sm text-text-secondary">
                {t('overview.renewalConfig.mode')}
              </p>
              <span
                className={`inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full ${
                  contract.renewalMode === 'AUTOMATIC'
                    ? 'bg-success-bg text-success-text'
                    : 'bg-info-bg text-info-text'
                }`}
              >
                {contract.renewalMode === 'AUTOMATIC'
                  ? t('overview.renewalConfig.automatic')
                  : t('overview.renewalConfig.manual')}
              </span>
            </div>
            {contract.renewalTermMonths != null && (
              <div className="flex items-center justify-between">
                <p className="text-sm text-text-secondary">
                  {t('overview.renewalConfig.renewalTerm')}
                </p>
                <p className="text-sm font-medium text-text-primary">
                  {t('overview.renewalConfig.month', {
                    count: contract.renewalTermMonths,
                  })}
                </p>
              </div>
            )}
            {contract.maxRenewals != null && (
              <div className="flex items-center justify-between">
                <p className="text-sm text-text-secondary">
                  {t('overview.renewalConfig.extensions')}
                </p>
                <p className="text-sm font-medium text-text-primary">
                  {contract.extensionsRemaining != null
                    ? t('overview.renewalConfig.remaining', {
                        remaining: contract.extensionsRemaining,
                        max: contract.maxRenewals,
                      })
                    : t('overview.renewalConfig.max', {
                        max: contract.maxRenewals,
                      })}
                </p>
              </div>
            )}
            {!contract.maxRenewals && contract.extensionsRemaining == null && (
              <div className="flex items-center justify-between">
                <p className="text-sm text-text-secondary">
                  {t('overview.renewalConfig.extensions')}
                </p>
                <p className="text-sm font-medium text-text-primary">
                  {t('overview.renewalConfig.unlimited')}
                </p>
              </div>
            )}
            {contract.rentAdjustmentType &&
              contract.rentAdjustmentType !== 'NONE' && (
                <div className="flex items-center justify-between">
                  <p className="text-sm text-text-secondary">
                    {t('overview.renewalConfig.rentAdjustment')}
                  </p>
                  <p className="text-sm font-medium text-text-primary">
                    {contract.rentAdjustmentType === 'FIXED_PERCENTAGE' &&
                    contract.rentAdjustmentValue != null
                      ? `+${contract.rentAdjustmentValue}%`
                      : contract.rentAdjustmentType === 'FIXED_AMOUNT' &&
                          contract.rentAdjustmentValue != null
                        ? `+${contract.rentAmountCurrency} ${contract.rentAdjustmentValue.toFixed(2)}`
                        : contract.rentAdjustmentType === 'MANUAL'
                          ? t('overview.renewalConfig.manual')
                          : '—'}
                  </p>
                </div>
              )}
            {(contract.landlordNoticeDays != null ||
              contract.tenantNoticeDays != null) && (
              <div className="pt-2 border-t border-border-default">
                <p className="text-[11px] font-medium uppercase tracking-wider text-text-muted mb-2">
                  {t('overview.renewalConfig.noticePeriods')}
                </p>
                <div className="space-y-2">
                  {contract.landlordNoticeDays != null && (
                    <div className="flex items-center justify-between">
                      <p className="text-sm text-text-secondary">
                        {t('overview.renewalConfig.landlord')}
                      </p>
                      <p className="text-sm font-medium text-text-primary">
                        {t('overview.renewalConfig.days', {
                          count: contract.landlordNoticeDays,
                        })}
                      </p>
                    </div>
                  )}
                  {contract.tenantNoticeDays != null && (
                    <div className="flex items-center justify-between">
                      <p className="text-sm text-text-secondary">
                        {t('overview.renewalConfig.contact')}
                      </p>
                      <p className="text-sm font-medium text-text-primary">
                        {t('overview.renewalConfig.days', {
                          count: contract.tenantNoticeDays,
                        })}
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )}
            {contract.requiresTenantConfirmation && (
              <div className="flex items-center justify-between pt-2 border-t border-border-default">
                <p className="text-sm text-text-secondary">
                  {t('overview.renewalConfig.contactConfirmation')}
                </p>
                <span className="inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full bg-warning-bg text-warning-text">
                  {t('overview.renewalConfig.required')}
                </span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Additional Terms */}
      {(contract.terminationNoticeDays ||
        contract.lateFeePercentage ||
        contract.tenantRemindersEnabled !== undefined) && (
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
          <h2 className="text-lg font-semibold text-text-primary mb-4">
            {t('overview.additionalTerms')}
          </h2>
          <div className="space-y-3">
            <div>
              <p className="text-sm text-text-secondary">
                {t('overview.tenantReminders')}
              </p>
              <p className="font-medium text-text-primary">
                {!contract.tenantRemindersEnabled
                  ? t('overview.remindersOff')
                  : contract.remindersPausedUntil &&
                      contract.remindersPausedUntil >=
                        new Date().toISOString().slice(0, 10)
                    ? t('overview.remindersPausedUntil', {
                        date: formatDate(contract.remindersPausedUntil),
                      })
                    : t('overview.remindersOn')}
              </p>
            </div>
            {contract.terminationNoticeDays && (
              <div>
                <p className="text-sm text-text-secondary">
                  {t('overview.terminationNotice')}
                </p>
                <p className="font-medium text-text-primary">
                  {t('overview.renewalConfig.days', {
                    count: contract.terminationNoticeDays,
                  })}
                </p>
              </div>
            )}
            {contract.lateFeePercentage && (
              <div>
                <p className="text-sm text-text-secondary">
                  {t('overview.lateFee')}
                </p>
                <p className="font-medium text-text-primary">
                  {contract.lateFeePercentage}%
                  <span className="ml-2 text-sm font-normal text-text-secondary">
                    {contract.lateFeeEnabled
                      ? t('overview.lateFeeActive', {
                          count: contract.lateFeeGraceDays ?? 0,
                        })
                      : t('overview.lateFeeInactive')}
                  </span>
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Terms and Conditions */}
      {contract.termsAndConditions && (
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
          <h2 className="text-lg font-semibold text-text-primary mb-4">
            {t('overview.termsAndConditions')}
          </h2>
          <RichTextDisplay content={contract.termsAndConditions} />
        </div>
      )}

      {/* Notes */}
      {contract.notes && (
        <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
          <h2 className="text-lg font-semibold text-text-primary mb-4">
            {t('overview.notes')}
          </h2>
          <RichTextDisplay content={contract.notes} />
        </div>
      )}

      {/* Country-Specific Details */}
      {contract.countryCode &&
        contract.countryMetadata &&
        Object.keys(contract.countryMetadata).length > 0 && (
          <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
            <h2 className="text-lg font-semibold text-text-primary mb-4">
              {countryName
                ? t('overview.countryDetails', { country: countryName })
                : t('overview.countrySpecificDetails')}
            </h2>
            <CountryMetadataForm
              countryCode={contract.countryCode}
              value={contract.countryMetadata}
              onChange={() => {}}
              currency={contract.rentAmountCurrency}
              disabled={true}
            />
          </div>
        )}

      {/* Metadata */}
      <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6 lg:col-span-2">
        <button
          onClick={() => setIsMetadataExpanded(!isMetadataExpanded)}
          className="w-full flex items-center justify-between text-left group"
        >
          <h2 className="text-lg font-semibold text-text-primary">
            {t('overview.metadata.title')}
          </h2>
          {isMetadataExpanded ? (
            <ChevronUp className="h-5 w-5 text-text-secondary group-hover:text-text-secondary " />
          ) : (
            <ChevronDown className="h-5 w-5 text-text-secondary group-hover:text-text-secondary " />
          )}
        </button>
        {isMetadataExpanded && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mt-4">
            <div>
              <span className="text-text-secondary">
                {t('overview.metadata.created')}
              </span>{' '}
              <span className="text-text-primary">
                {formatDate(contract.createdAt)} {t('overview.metadata.at')}{' '}
                {new Date(contract.createdAt).toLocaleTimeString()}
              </span>
            </div>
            <div>
              <span className="text-text-secondary">
                {t('overview.metadata.lastUpdated')}
              </span>{' '}
              <span className="text-text-primary">
                {formatDate(contract.updatedAt)} {t('overview.metadata.at')}{' '}
                {new Date(contract.updatedAt).toLocaleTimeString()}
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
