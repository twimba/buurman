import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save, Plus, Trash2, UserPlus } from 'lucide-react';
import {
  ContractResponse,
  CreateContractRequest,
  ContractType,
  ContractPartyRole,
  ContractPartyRequest,
  PaymentFrequency,
  RentComponentFormItem,
  RentComponentType,
} from '@/types/contract';
import { CreateContactRequest } from '@/types/contact';
import { RichTextEditor } from '@buurman/ui';
import {
  FormStepGate,
  MobileFormStepperFooter,
  MobileFormStepperProvider,
  MobileStepperHeader,
} from '@/components/common/MobileFormStepper';
import { MoneyInput } from '@/components/common/MoneyInput';
import { PropertySelector } from '@/components/common/PropertySelector';
import { ContactSelector } from '@/components/common/ContactSelector';
import { validatePhoneE164 } from '@/components/common/PhoneInput';
import { InlineContactForm } from '@/components/contracts/InlineContactForm';
import CountryMetadataForm, {
  useCountryName,
} from '@/components/contracts/CountryMetadataForm';
import { RenewalConfigForm } from '@/components/contracts/RenewalConfigForm';
import { RentBreakdown } from '@/components/contracts/RentBreakdown';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useProperty } from '@/hooks/usePropertyHooks';
import {
  useAddContractParty,
  useRemoveContractParty,
  useChangePrimaryContact,
} from '@/hooks/useContractHooks';
import { useTranslation } from 'react-i18next';

interface ContractFormProps {
  contract?: ContractResponse;
  onSubmit: (data: CreateContractRequest) => Promise<void>;
  isLoading: boolean;
  prefilledPropertyId?: string;
  prefilledContactId?: string;
}

const ADDITIONAL_ROLES = [
  ContractPartyRole.GUARANTOR,
  ContractPartyRole.COSIGNER,
  ContractPartyRole.EXTRA_TENANT,
];

const EMPTY_NEW_CONTACT: CreateContactRequest = {
  contactType: 'INDIVIDUAL',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  taxNumber: '',
  idNumber: '',
};

interface PartyEntry {
  id: string;
  mode: 'select' | 'create';
  contactIdentifier: string;
  newContact: CreateContactRequest;
  role: ContractPartyRole;
}

// Convert empty optional strings to null before submission
function sanitizeContact(data: CreateContactRequest): CreateContactRequest {
  return {
    ...data,
    email: data.email?.trim() || undefined,
    phone: data.phone?.trim() || undefined,
  };
}

// Validate inline contact data, return errors keyed by prefix
function validateInlineContact(
  data: CreateContactRequest,
  prefix: string,
  t: (key: string) => string
): Record<string, string> {
  const errs: Record<string, string> = {};
  if (!data.firstName?.trim()) {
    errs[`${prefix}_firstName`] = t('form.validation.required');
  }
  if (
    data.email?.trim() &&
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email ?? '')
  ) {
    errs[`${prefix}_email`] = t('form.validation.invalidEmail');
  }
  if (data.phone) {
    const phoneErr = validatePhoneE164(data.phone);
    if (phoneErr) {
      errs[`${prefix}_phone`] = phoneErr;
    }
  }
  return errs;
}

export const ContractForm = ({
  contract,
  onSubmit,
  isLoading,
  prefilledPropertyId,
  prefilledContactId,
}: ContractFormProps) => {
  const { t } = useTranslation('contracts');
  const navigate = useNavigate();
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const isEditing = !!contract;

  // Primary contact state (create mode)
  const [primaryMode, setPrimaryMode] = useState<'select' | 'create'>('select');
  const [primaryContactId, setPrimaryContactId] = useState(
    prefilledContactId ?? ''
  );
  const [newPrimaryContact, setNewPrimaryContact] =
    useState<CreateContactRequest>({ ...EMPTY_NEW_CONTACT });

  // Additional parties state (create mode)
  const [additionalParties, setAdditionalParties] = useState<PartyEntry[]>([]);

  const [formData, setFormData] = useState<
    Omit<CreateContractRequest, 'parties' | 'rentAmount'> & {
      rentAmount: number | '';
      rentComponents: RentComponentFormItem[];
      breakdownMode: boolean;
    }
  >({
    propertyIdentifier:
      prefilledPropertyId || contract?.property.identifier || '',
    contractType: contract?.contractType ?? ContractType.FIXED_TERM,
    startDate: contract?.startDate ?? '',
    endDate: contract?.endDate ?? '',
    signedDate: contract?.signedDate ?? '',
    rentAmount: contract?.rentAmount ?? '',
    depositAmount: contract?.depositAmount ?? undefined,
    securityDeposit: contract?.securityDeposit ?? undefined,
    rentAmountCurrency: contract?.rentAmountCurrency || defaultCurrency,
    depositAmountCurrency: contract?.depositAmountCurrency || defaultCurrency,
    securityDepositCurrency:
      contract?.securityDepositCurrency || defaultCurrency,
    paymentFrequency: contract?.paymentFrequency ?? PaymentFrequency.MONTHLY,
    paymentDueDay: contract?.paymentDueDay ?? 1,
    terminationNoticeDays: contract?.terminationNoticeDays ?? 30,
    lateFeePercentage: contract?.lateFeePercentage ?? undefined,
    termsAndConditions: contract?.termsAndConditions ?? '',
    notes: contract?.notes ?? '',
    countryMetadata: contract?.countryMetadata ?? undefined,
    renewalMode: contract?.renewalMode ?? 'NONE',
    renewalTermMonths: contract?.renewalTermMonths ?? undefined,
    maxRenewals: contract?.maxRenewals ?? undefined,
    landlordNoticeDays: contract?.landlordNoticeDays ?? 30,
    tenantNoticeDays: contract?.tenantNoticeDays ?? 30,
    requiresTenantConfirmation: contract?.requiresTenantConfirmation ?? false,
    rentAdjustmentType: contract?.rentAdjustmentType ?? 'NONE',
    rentAdjustmentValue: contract?.rentAdjustmentValue ?? undefined,
    landlordType: contract?.landlordType ?? undefined,
    regionCode: contract?.regionCode ?? undefined,
    rentComponents:
      contract?.rentComponents?.map((c) => ({
        componentType: c.componentType,
        amount: c.amount,
        description: c.description,
      })) ?? [],
    breakdownMode: (contract?.rentComponents?.length ?? 0) > 0,
  });

  const [contractIdentifier, setContractIdentifier] = useState(
    contract?.identifier
  );

  // Track duplicate-blocking state from all inline contact forms
  const [dupBlockers, setDupBlockers] = useState<Record<string, boolean>>({});
  const anyDuplicateBlocking = Object.values(dupBlockers).some(Boolean);
  const handleDupBlocking = useCallback(
    (key: string) => (blocking: boolean) => {
      setDupBlockers((prev) => {
        if (prev[key] === blocking) {
          return prev;
        }
        return { ...prev, [key]: blocking };
      });
    },
    []
  );

  // Look up selected property's country for metadata form
  const { data: selectedProperty } = useProperty(formData.propertyIdentifier);
  const propertyCountryCode =
    contract?.countryCode || selectedProperty?.countryCode || undefined;
  const countryName = useCountryName(propertyCountryCode);

  // Sync currency fields when defaultCurrency loads asynchronously (create mode)
  useEffect(() => {
    if (!defaultCurrency || isEditing) {
      return;
    }
    /* eslint-disable react-hooks/set-state-in-effect */
    setFormData((prev) => {
      const fields = [
        'rentAmountCurrency',
        'depositAmountCurrency',
        'securityDepositCurrency',
      ] as const;
      const needsUpdate = fields.some((f) => !prev[f]);
      if (!needsUpdate) {
        return prev;
      }
      const updated = { ...prev };
      for (const f of fields) {
        if (!updated[f]) {
          (updated as Record<string, unknown>)[f] = defaultCurrency;
        }
      }
      return updated;
    });
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [defaultCurrency, isEditing]);

  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    if (contract && contract.identifier !== contractIdentifier) {
      setContractIdentifier(contract.identifier);
      setFormData({
        propertyIdentifier: contract.property.identifier,
        contractType: contract.contractType,
        startDate: contract.startDate,
        endDate: contract.endDate ?? '',
        signedDate: contract.signedDate ?? '',
        rentAmount: contract.rentAmount ?? '',
        depositAmount: contract.depositAmount ?? undefined,
        securityDeposit: contract.securityDeposit ?? undefined,
        rentAmountCurrency: contract.rentAmountCurrency,
        depositAmountCurrency:
          contract.depositAmountCurrency || defaultCurrency,
        securityDepositCurrency:
          contract.securityDepositCurrency || defaultCurrency,
        paymentFrequency: contract.paymentFrequency,
        paymentDueDay: contract.paymentDueDay ?? 1,
        terminationNoticeDays: contract.terminationNoticeDays ?? 30,
        lateFeePercentage: contract.lateFeePercentage ?? undefined,
        termsAndConditions: contract.termsAndConditions ?? '',
        notes: contract.notes ?? '',
        countryMetadata: contract.countryMetadata ?? undefined,
        rentComponents:
          contract.rentComponents?.map((c) => ({
            componentType: c.componentType,
            amount: c.amount as number | '',
            description: c.description,
          })) ?? [],
        breakdownMode: (contract.rentComponents?.length ?? 0) > 0,
      });
    }
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [contract, contractIdentifier, defaultCurrency]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.propertyIdentifier) {
      newErrors.propertyIdentifier = t('form.validation.propertyRequired');
    }

    // Validate primary contact
    if (!isEditing) {
      if (primaryMode === 'select' && !primaryContactId) {
        newErrors.primaryContact = t('form.validation.contactRequired');
      } else if (primaryMode === 'create') {
        Object.assign(
          newErrors,
          validateInlineContact(newPrimaryContact, 'primary', t)
        );
      }
    }

    if (!formData.startDate) {
      newErrors.startDate = t('form.validation.startDateRequired');
    }
    if (formData.breakdownMode) {
      const hasBaseRent = formData.rentComponents.some(
        (c) => c.componentType === RentComponentType.BASE_RENT
      );
      if (!hasBaseRent) {
        newErrors.rentComponents = t('form.validation.baseRentRequired');
      }
      const invalidAmounts = formData.rentComponents.some(
        (c) => typeof c.amount !== 'number' || c.amount <= 0
      );
      if (invalidAmounts) {
        newErrors.rentComponents = t(
          'form.validation.componentAmountsPositive'
        );
      }
      const missingOtherDesc = formData.rentComponents.some(
        (c) =>
          c.componentType === RentComponentType.OTHER &&
          (!c.description || !c.description.trim())
      );
      if (missingOtherDesc) {
        newErrors.rentComponents = t(
          'form.validation.otherDescriptionRequired'
        );
      }
      // Validate total > 0
      const total = formData.rentComponents.reduce(
        (sum, c) => sum + (typeof c.amount === 'number' ? c.amount : 0),
        0
      );
      if (total <= 0) {
        newErrors.rentAmount = t('form.validation.rentAmountPositive');
      }
    } else if (!formData.rentAmount || formData.rentAmount <= 0) {
      newErrors.rentAmount = t('form.validation.rentAmountPositive');
    }
    if (
      formData.rentAmount &&
      formData.rentAmount > 0 &&
      !(formData.rentAmountCurrency || defaultCurrency || '').trim()
    ) {
      newErrors.rentAmountCurrency = t('form.validation.rentCurrencyRequired');
    }
    if (
      formData.depositAmount &&
      formData.depositAmount > 0 &&
      !(formData.depositAmountCurrency || defaultCurrency || '').trim()
    ) {
      newErrors.depositAmountCurrency = t(
        'form.validation.depositCurrencyRequired'
      );
    }
    if (
      formData.securityDeposit &&
      formData.securityDeposit > 0 &&
      !(formData.securityDepositCurrency || defaultCurrency || '').trim()
    ) {
      newErrors.securityDepositCurrency = t(
        'form.validation.securityDepositCurrencyRequired'
      );
    }

    if (
      formData.contractType === ContractType.FIXED_TERM &&
      !formData.endDate
    ) {
      newErrors.endDate = t('form.validation.endDateRequired');
    }

    if (formData.startDate && formData.endDate) {
      if (new Date(formData.endDate) < new Date(formData.startDate)) {
        newErrors.endDate = t('form.validation.endDateAfterStart');
      }
    }

    if (
      formData.paymentDueDay &&
      (formData.paymentDueDay < 1 || formData.paymentDueDay > 31)
    ) {
      newErrors.paymentDueDay = t('form.validation.paymentDueDayRange');
    }

    // Validate additional parties
    if (!isEditing) {
      for (let i = 0; i < additionalParties.length; i++) {
        const p = additionalParties[i];
        if (p.mode === 'select' && !p.contactIdentifier) {
          newErrors[`party_${i}`] = t('form.validation.contactRequired');
        } else if (p.mode === 'create') {
          Object.assign(
            newErrors,
            validateInlineContact(p.newContact, `party_${i}`, t)
          );
        }
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const submitForm = async () => {
    if (!validate()) {
      return;
    }

    try {
      // Build parties array for create mode
      const parties: ContractPartyRequest[] = isEditing
        ? []
        : [
            primaryMode === 'select'
              ? {
                  contactIdentifier: primaryContactId,
                  role: ContractPartyRole.PRIMARY_TENANT,
                }
              : {
                  newContact: sanitizeContact(newPrimaryContact),
                  role: ContractPartyRole.PRIMARY_TENANT,
                },
            ...additionalParties
              .filter(
                (p) =>
                  (p.mode === 'select' && p.contactIdentifier) ||
                  p.mode === 'create'
              )
              .map((p) =>
                p.mode === 'select'
                  ? { contactIdentifier: p.contactIdentifier, role: p.role }
                  : { newContact: sanitizeContact(p.newContact), role: p.role }
              ),
          ];

      const rentComponents = formData.breakdownMode
        ? formData.rentComponents.map((c) => ({
            componentType: c.componentType,
            amount: typeof c.amount === 'number' ? c.amount : 0,
            description: c.description,
          }))
        : undefined;

      await onSubmit({
        ...formData,
        rentAmount: formData.rentAmount as number,
        endDate: formData.endDate || undefined,
        signedDate: formData.signedDate || undefined,
        parties,
        rentComponents,
      });
      if (contract) {
        navigate(`/contracts/${contract.identifier}`);
      } else {
        navigate('/contracts');
      }
    } catch (error) {
      console.error('Failed to save contract:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitForm();
  };

  const handleCmdEnter = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }
  };

  const handleChange = (field: string, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const addParty = (role: ContractPartyRole) => {
    setAdditionalParties((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        mode: 'select',
        contactIdentifier: '',
        newContact: { ...EMPTY_NEW_CONTACT },
        role,
      },
    ]);
  };

  const removeParty = (index: number) => {
    setAdditionalParties((prev) => prev.filter((_, i) => i !== index));
  };

  const updatePartyField = (index: number, updates: Partial<PartyEntry>) => {
    setAdditionalParties((prev) =>
      prev.map((p, i) => (i === index ? { ...p, ...updates } : p))
    );
    // Clear relevant errors
    const keysToRemove = Object.keys(errors).filter((k) =>
      k.startsWith(`party_${index}`)
    );
    if (keysToRemove.length > 0) {
      setErrors((prev) => {
        const next = { ...prev };
        keysToRemove.forEach((k) => delete next[k]);
        return next;
      });
    }
  };

  return (
    <MobileFormStepperProvider totalSteps={3}>
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-6"
    >
      <MobileStepperHeader
        labels={[
          t('form.propertyAndParties'),
          t('form.financialTerms'),
          t('form.termsAndConditions'),
        ]}
      />
      {/* Step 0: Property + Parties + Contract Details */}
      <FormStepGate step={0}>
      {/* Property Selection */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {isEditing ? t('form.property') : t('form.propertyAndParties')}
        </h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.property')} <span className="text-error-text">*</span>
            </label>
            <PropertySelector
              value={formData.propertyIdentifier}
              onChange={(value) => handleChange('propertyIdentifier', value)}
              disabled={isLoading}
            />
            {errors.propertyIdentifier && (
              <p className="text-error-text text-sm mt-1">
                {errors.propertyIdentifier}
              </p>
            )}
          </div>

          {/* Parties section — create mode: inline editors */}
          {!isEditing && (
            <>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  {t('form.primaryContact')}{' '}
                  <span className="text-error-text">*</span>
                </label>

                {primaryMode === 'select' ? (
                  <>
                    <ContactSelector
                      value={primaryContactId}
                      onChange={(value) => {
                        setPrimaryContactId(value);
                        if (errors.primaryContact) {
                          setErrors((prev) => {
                            const next = { ...prev };
                            delete next.primaryContact;
                            return next;
                          });
                        }
                      }}
                      disabled={isLoading}
                    />
                    {errors.primaryContact && (
                      <p className="text-error-text text-sm mt-1">
                        {errors.primaryContact}
                      </p>
                    )}
                    <button
                      type="button"
                      onClick={() => {
                        setPrimaryMode('create');
                        setPrimaryContactId('');
                        setErrors((prev) => {
                          const next = { ...prev };
                          delete next.primaryContact;
                          return next;
                        });
                      }}
                      className="mt-2 flex items-center gap-1 text-xs text-primary-500 hover:text-primary-600 transition-colors"
                      disabled={isLoading}
                    >
                      <UserPlus className="h-3 w-3" />
                      {t('form.createNewContact')}
                    </button>
                  </>
                ) : (
                  <InlineContactForm
                    value={newPrimaryContact}
                    onChange={(data) => {
                      setNewPrimaryContact(data);
                      // Clear related errors on change
                      const keysToRemove = Object.keys(errors).filter((k) =>
                        k.startsWith('primary_')
                      );
                      if (keysToRemove.length > 0) {
                        setErrors((prev) => {
                          const next = { ...prev };
                          keysToRemove.forEach((k) => delete next[k]);
                          return next;
                        });
                      }
                    }}
                    onSwitchToSelect={() => {
                      setPrimaryMode('select');
                      setNewPrimaryContact({ ...EMPTY_NEW_CONTACT });
                    }}
                    errors={errors}
                    errorPrefix="primary"
                    disabled={isLoading}
                    onBlockingChange={handleDupBlocking('primary')}
                  />
                )}
              </div>

              {/* Additional parties */}
              {additionalParties.map((party, index) => (
                <div key={party.id}>
                  {party.mode === 'select' ? (
                    <div>
                      <div className="flex gap-2 items-center">
                        <div className="flex-1">
                          <ContactSelector
                            value={party.contactIdentifier}
                            onChange={(value) =>
                              updatePartyField(index, {
                                contactIdentifier: value,
                              })
                            }
                            disabled={isLoading}
                          />
                        </div>
                        <select
                          value={party.role}
                          onChange={(e) =>
                            updatePartyField(index, {
                              role: e.target.value as ContractPartyRole,
                            })
                          }
                          className="w-40 border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary text-sm"
                          disabled={isLoading}
                        >
                          {ADDITIONAL_ROLES.map((role) => (
                            <option key={role} value={role}>
                              {t(`enums.partyRoles.${role}`, role)}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          onClick={() => removeParty(index)}
                          className="p-2 text-error-text hover:bg-error-bg rounded transition-colors"
                          disabled={isLoading}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      {errors[`party_${index}`] && (
                        <p className="text-error-text text-sm mt-1">
                          {errors[`party_${index}`]}
                        </p>
                      )}
                      <button
                        type="button"
                        onClick={() =>
                          updatePartyField(index, {
                            mode: 'create',
                            contactIdentifier: '',
                          })
                        }
                        className="mt-1 flex items-center gap-1 text-xs text-primary-500 hover:text-primary-600 transition-colors"
                        disabled={isLoading}
                      >
                        <UserPlus className="h-3 w-3" />
                        {t('form.createNewContact')}
                      </button>
                    </div>
                  ) : (
                    <div>
                      <div className="flex gap-2 items-center mb-2">
                        <select
                          value={party.role}
                          onChange={(e) =>
                            updatePartyField(index, {
                              role: e.target.value as ContractPartyRole,
                            })
                          }
                          className="w-40 border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary text-sm"
                          disabled={isLoading}
                        >
                          {ADDITIONAL_ROLES.map((role) => (
                            <option key={role} value={role}>
                              {t(`enums.partyRoles.${role}`, role)}
                            </option>
                          ))}
                        </select>
                        <div className="flex-1" />
                        <button
                          type="button"
                          onClick={() => removeParty(index)}
                          className="p-2 text-error-text hover:bg-error-bg rounded transition-colors"
                          disabled={isLoading}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      <InlineContactForm
                        value={party.newContact}
                        onChange={(data) =>
                          updatePartyField(index, { newContact: data })
                        }
                        onSwitchToSelect={() =>
                          updatePartyField(index, {
                            mode: 'select',
                            newContact: { ...EMPTY_NEW_CONTACT },
                          })
                        }
                        errors={errors}
                        errorPrefix={`party_${index}`}
                        disabled={isLoading}
                        onBlockingChange={handleDupBlocking(`party_${index}`)}
                      />
                    </div>
                  )}
                </div>
              ))}

              <div className="flex items-center gap-4">
                {ADDITIONAL_ROLES.map((role) => (
                  <button
                    key={role}
                    type="button"
                    onClick={() => addParty(role)}
                    className="flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600 transition-colors"
                    disabled={isLoading}
                  >
                    <Plus className="h-4 w-4" />
                    {t('common:buttons.add')}{' '}
                    {t(`enums.partyRoles.${role}`, role)}
                  </button>
                ))}
              </div>
            </>
          )}
        </div>
      </div>

      {/* Contract Parties — edit mode: separate section with live API calls */}
      {isEditing && contract && (
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            {t('form.contractParties')}
          </h3>
          <ContractPartiesEditor contract={contract} isLoading={isLoading} />
        </div>
      )}

      {/* Contract Details */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('form.contractDetails')}
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.contractType')}{' '}
              <span className="text-error-text">*</span>
            </label>
            <select
              value={formData.contractType}
              onChange={(e) =>
                handleChange('contractType', e.target.value as ContractType)
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              disabled={isLoading}
            >
              <option value={ContractType.FIXED_TERM}>
                {t('enums.contractTypes.FIXED_TERM')}
              </option>
              <option value={ContractType.INDEFINITE}>
                {t('enums.contractTypes.INDEFINITE')}
              </option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.startDate')} <span className="text-error-text">*</span>
            </label>
            <div className="flex gap-2">
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => handleChange('startDate', e.target.value)}
                className="flex-1 border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() =>
                  handleChange(
                    'startDate',
                    new Date().toISOString().split('T')[0]
                  )
                }
                className="px-3 py-2 text-sm bg-surface-inset hover:bg-surface-raised border border-border-strong rounded-md transition-colors"
                disabled={isLoading}
              >
                {t('form.today')}
              </button>
            </div>
            {errors.startDate && (
              <p className="text-error-text text-sm mt-1">{errors.startDate}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.endDate')}{' '}
              {formData.contractType === ContractType.FIXED_TERM && (
                <span className="text-error-text">*</span>
              )}
            </label>
            <input
              type="date"
              value={formData.endDate}
              onChange={(e) => handleChange('endDate', e.target.value)}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              disabled={isLoading}
            />
            {errors.endDate && (
              <p className="text-error-text text-sm mt-1">{errors.endDate}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.signedDate')}
            </label>
            <div className="flex gap-2">
              <input
                type="date"
                value={formData.signedDate}
                onChange={(e) => handleChange('signedDate', e.target.value)}
                className="flex-1 border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() =>
                  handleChange(
                    'signedDate',
                    new Date().toISOString().split('T')[0]
                  )
                }
                className="px-3 py-2 text-sm bg-surface-inset hover:bg-surface-raised border border-border-strong rounded-md transition-colors"
                disabled={isLoading}
              >
                {t('form.today')}
              </button>
            </div>
          </div>
        </div>
      </div>
      </FormStepGate>

      {/* Step 1: Financial + Payment Terms + Termination */}
      <FormStepGate step={1}>
      {/* Financial Terms */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('form.financialTerms')}
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <RentBreakdown
            value={formData.rentAmount || ''}
            onValueChange={(val) => handleChange('rentAmount', val)}
            components={formData.rentComponents}
            onComponentsChange={(components) =>
              setFormData((prev) => ({ ...prev, rentComponents: components }))
            }
            onTotalChange={(total) =>
              setFormData((prev) => ({ ...prev, rentAmount: total }))
            }
            currency={formData.rentAmountCurrency || defaultCurrency || ''}
            disabled={isLoading}
            error={!!errors.rentAmount || !!errors.rentAmountCurrency}
            breakdownMode={formData.breakdownMode}
            onBreakdownModeChange={(mode) =>
              setFormData((prev) => ({ ...prev, breakdownMode: mode }))
            }
          />
          {(errors.rentAmount ||
            errors.rentAmountCurrency ||
            errors.rentComponents) && (
            <p className="text-error-text text-sm mt-1 col-span-1 lg:col-span-2">
              {errors.rentAmount ||
                errors.rentAmountCurrency ||
                errors.rentComponents}
            </p>
          )}

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.depositAmount')}
            </label>
            <MoneyInput
              value={formData.depositAmount ?? undefined}
              onChange={(val) => handleChange('depositAmount', val)}
              currency={formData.depositAmountCurrency || defaultCurrency || ''}
              disabled={isLoading}
              error={!!errors.depositAmountCurrency}
            />
            {errors.depositAmountCurrency && (
              <p className="text-error-text text-sm mt-1">
                {errors.depositAmountCurrency}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.securityDeposit')}
            </label>
            <MoneyInput
              value={formData.securityDeposit ?? undefined}
              onChange={(val) => handleChange('securityDeposit', val)}
              currency={
                formData.securityDepositCurrency || defaultCurrency || ''
              }
              disabled={isLoading}
              error={!!errors.securityDepositCurrency}
            />
            {errors.securityDepositCurrency && (
              <p className="text-error-text text-sm mt-1">
                {errors.securityDepositCurrency}
              </p>
            )}
          </div>
        </div>

        {/* Renewal Configuration (inside Financial Terms) */}
        <div className="mt-6 pt-6 border-t border-border-default">
          <RenewalConfigForm
            renewalMode={formData.renewalMode ?? 'NONE'}
            renewalTermMonths={formData.renewalTermMonths}
            maxRenewals={formData.maxRenewals}
            landlordNoticeDays={formData.landlordNoticeDays}
            tenantNoticeDays={formData.tenantNoticeDays}
            requiresTenantConfirmation={formData.requiresTenantConfirmation}
            rentAdjustmentType={formData.rentAdjustmentType ?? 'NONE'}
            rentAdjustmentValue={formData.rentAdjustmentValue}
            countryCode={propertyCountryCode}
            regionCode={selectedProperty?.regionCode}
            onChange={handleChange}
          />
        </div>
      </div>

      {/* Payment Terms */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('form.paymentTerms')}
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.paymentFrequency')}{' '}
              <span className="text-error-text">*</span>
            </label>
            <select
              value={formData.paymentFrequency}
              onChange={(e) =>
                handleChange(
                  'paymentFrequency',
                  e.target.value as PaymentFrequency
                )
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              disabled={isLoading}
            >
              <option value={PaymentFrequency.MONTHLY}>
                {t('enums.paymentFrequencies.MONTHLY')}
              </option>
              <option value={PaymentFrequency.QUARTERLY}>
                {t('enums.paymentFrequencies.QUARTERLY')}
              </option>
              <option value={PaymentFrequency.ANNUALLY}>
                {t('enums.paymentFrequencies.ANNUALLY')}
              </option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.paymentDueDay')}
            </label>
            <input
              type="number"
              min="1"
              max="31"
              value={formData.paymentDueDay ?? ''}
              onChange={(e) =>
                handleChange(
                  'paymentDueDay',
                  e.target.value ? parseInt(e.target.value) : undefined
                )
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              placeholder="1"
              disabled={isLoading}
            />
            {errors.paymentDueDay && (
              <p className="text-error-text text-sm mt-1">
                {errors.paymentDueDay}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.lateFeePercentage')}
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              max="100"
              value={formData.lateFeePercentage ?? ''}
              onChange={(e) =>
                handleChange(
                  'lateFeePercentage',
                  e.target.value ? parseFloat(e.target.value) : undefined
                )
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              placeholder="2.5"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>

      {/* Termination */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('form.termination')}
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">
              {t('form.terminationNoticeDays')}
            </label>
            <input
              type="number"
              min="0"
              value={formData.terminationNoticeDays ?? ''}
              onChange={(e) =>
                handleChange(
                  'terminationNoticeDays',
                  e.target.value ? parseInt(e.target.value) : undefined
                )
              }
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
              placeholder="30"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>
      </FormStepGate>

      {/* Step 2: Terms + Notes + Country-Specific */}
      <FormStepGate step={2}>
      {/* Terms and Conditions */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('form.termsAndConditions')}
        </h3>
        <div>
          <RichTextEditor
            value={formData.termsAndConditions ?? ''}
            onChange={(value) => handleChange('termsAndConditions', value)}
            placeholder={t('form.termsAndConditionsPlaceholder')}
            onSubmit={submitForm}
          />
        </div>
      </div>

      {/* Notes */}
      <div>
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('form.notes')}
        </h3>
        <div>
          <RichTextEditor
            value={formData.notes ?? ''}
            onChange={(value) => handleChange('notes', value)}
            placeholder={t('form.notesPlaceholder')}
            onSubmit={submitForm}
          />
        </div>
      </div>

      {/* Country-Specific Rental Details */}
      {propertyCountryCode ? (
        <div>
          <h3 className="text-lg font-semibold text-text-primary mb-2">
            {countryName
              ? t('form.countryRentalDetails', { country: countryName })
              : t('overview.countrySpecificDetails')}
          </h3>
          <p className="text-sm text-text-secondary mb-4">
            {t('form.countryRegulatoryFields', {
              country: countryName || propertyCountryCode,
            })}
          </p>
          <CountryMetadataForm
            countryCode={propertyCountryCode}
            value={(formData.countryMetadata as Record<string, unknown>) ?? {}}
            onChange={(metadata) =>
              setFormData((prev) => ({
                ...prev,
                countryMetadata:
                  Object.keys(metadata).length > 0 ? metadata : undefined,
              }))
            }
            currency={formData.rentAmountCurrency || defaultCurrency || 'EUR'}
            disabled={isEditing && contract?.status !== 'DRAFT'}
          />
        </div>
      ) : formData.propertyIdentifier ? (
        <p className="text-sm text-text-muted italic">
          {t('form.setCountryForFields')}
        </p>
      ) : null}
      </FormStepGate>

      {/* Actions — md+ inline save bar (single-scroll desktop UX) */}
      <div className="hidden md:flex gap-2 justify-end mt-6 pt-6 border-t border-border-default">
        <button
          type="button"
          onClick={() => navigate('/contracts')}
          className="border border-border-strong px-4 py-2 rounded hover:bg-surface-inset transition-colors flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          {t('common:buttons.cancel')}
        </button>
        <div className="relative group/submit">
          <button
            type="submit"
            className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            disabled={isLoading || anyDuplicateBlocking}
          >
            <Save className="h-4 w-4" />
            {isLoading
              ? t('form.saving')
              : contract
                ? t('form.updateContract')
                : t('form.createContract')}
          </button>
          {anyDuplicateBlocking && (
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 text-xs font-medium text-white bg-neutral-800 dark:bg-neutral-700 rounded-lg whitespace-nowrap opacity-0 group-hover/submit:opacity-100 transition-opacity duration-150 shadow-lg pointer-events-none">
              {t('form.dismissDuplicateWarning')}
              <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-px border-4 border-transparent border-t-neutral-800 dark:border-t-neutral-700" />
            </div>
          )}
        </div>
      </div>

      {/* Phone: sticky stepper footer with Back/Continue/Save */}
      <MobileFormStepperFooter
        onSubmit={() => {
          const f = document.activeElement?.closest('form') as
            | HTMLFormElement
            | null;
          if (f) {
            f.requestSubmit();
          }
        }}
        onCancel={() => navigate('/contracts')}
        isSubmitting={isLoading || anyDuplicateBlocking}
        saveLabel={
          isLoading
            ? t('form.saving')
            : contract
              ? t('form.updateContract')
              : t('form.createContract')
        }
      />
    </form>
    </MobileFormStepperProvider>
  );
};

// --- Inline party editor for edit mode ---

const ContractPartiesEditor = ({
  contract,
  isLoading,
}: {
  contract: ContractResponse;
  isLoading: boolean;
}) => {
  const { t } = useTranslation('contracts');
  const [showAddForm, setShowAddForm] = useState(false);
  const [addMode, setAddMode] = useState<'select' | 'create'>('select');
  const [changingPrimary, setChangingPrimary] = useState(false);
  const [changePrimaryMode, setChangePrimaryMode] = useState<
    'select' | 'create'
  >('select');
  const [newContactId, setNewContactId] = useState('');
  const [newRole, setNewRole] = useState<ContractPartyRole>(
    ContractPartyRole.GUARANTOR
  );
  const [newPrimaryContactId, setNewPrimaryContactId] = useState('');
  const [inlineNewContact, setInlineNewContact] =
    useState<CreateContactRequest>({
      ...EMPTY_NEW_CONTACT,
    });
  const [inlineNewPrimary, setInlineNewPrimary] =
    useState<CreateContactRequest>({ ...EMPTY_NEW_CONTACT });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [dupBlockers, setDupBlockers] = useState<Record<string, boolean>>({});
  const handleDupBlocking = useCallback(
    (key: string) => (blocking: boolean) => {
      setDupBlockers((prev) => {
        if (prev[key] === blocking) {
          return prev;
        }
        return { ...prev, [key]: blocking };
      });
    },
    []
  );

  const addPartyMutation = useAddContractParty(contract.identifier);
  const removePartyMutation = useRemoveContractParty(contract.identifier);
  const changePrimaryMutation = useChangePrimaryContact(contract.identifier);

  const handleAddParty = async () => {
    try {
      if (addMode === 'select') {
        if (!newContactId) {
          return;
        }
        await addPartyMutation.mutateAsync({
          contactIdentifier: newContactId,
          role: newRole,
        });
      } else {
        const validationErrors = validateInlineContact(
          inlineNewContact,
          'add_party',
          t
        );
        if (Object.keys(validationErrors).length > 0) {
          setErrors(validationErrors);
          return;
        }
        await addPartyMutation.mutateAsync({
          newContact: sanitizeContact(inlineNewContact),
          role: newRole,
        });
      }
      resetAddForm();
    } catch {
      // Error is handled by mutation's onError callback (toast)
    }
  };

  const resetAddForm = () => {
    setNewContactId('');
    setNewRole(ContractPartyRole.GUARANTOR);
    setAddMode('select');
    setInlineNewContact({ ...EMPTY_NEW_CONTACT });
    setShowAddForm(false);
    setErrors({});
  };

  const handleRemoveParty = async (partyIdentifier: string) => {
    try {
      await removePartyMutation.mutateAsync(partyIdentifier);
    } catch {
      // Error is handled by mutation's onError callback (toast)
    }
  };

  const handleChangePrimary = async () => {
    try {
      if (changePrimaryMode === 'select') {
        if (!newPrimaryContactId) {
          return;
        }
        await changePrimaryMutation.mutateAsync({
          contactIdentifier: newPrimaryContactId,
        });
      } else {
        const validationErrors = validateInlineContact(
          inlineNewPrimary,
          'change_primary',
          t
        );
        if (Object.keys(validationErrors).length > 0) {
          setErrors(validationErrors);
          return;
        }
        await changePrimaryMutation.mutateAsync({
          newContact: sanitizeContact(inlineNewPrimary),
        });
      }
      resetChangePrimaryForm();
    } catch {
      // Error is handled by mutation's onError callback (toast)
    }
  };

  const resetChangePrimaryForm = () => {
    setNewPrimaryContactId('');
    setChangePrimaryMode('select');
    setInlineNewPrimary({ ...EMPTY_NEW_CONTACT });
    setChangingPrimary(false);
    setErrors({});
  };

  const parties = contract.parties ?? [];
  const isBusy =
    isLoading ||
    addPartyMutation.isPending ||
    removePartyMutation.isPending ||
    changePrimaryMutation.isPending;

  return (
    <div className="space-y-3">
      {/* Existing parties */}
      {parties.map((party) => (
        <div
          key={party.identifier}
          className="flex items-center gap-3 p-3 bg-surface-page dark:bg-surface-card rounded-lg"
        >
          <div className="flex-1 min-w-0">
            <span className="font-medium text-text-primary">
              {party.contact.firstName} {party.contact.lastName}
            </span>
            <span className="ml-2 text-xs px-2 py-0.5 rounded-full bg-surface-inset text-text-secondary">
              {t(`enums.partyRoles.${party.role}`, party.role)}
            </span>
          </div>
          {party.role === ContractPartyRole.PRIMARY_TENANT ? (
            <button
              type="button"
              onClick={() => setChangingPrimary(true)}
              className="text-xs text-primary-500 hover:text-primary-600 transition-colors whitespace-nowrap"
              disabled={isBusy}
            >
              {t('form.change')}
            </button>
          ) : (
            <button
              type="button"
              onClick={() => handleRemoveParty(party.identifier)}
              className="p-2 text-error-text hover:bg-error-bg rounded transition-colors"
              disabled={isBusy}
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      ))}

      {/* Change primary contact form */}
      {changingPrimary && (
        <div className="space-y-2">
          {changePrimaryMode === 'select' ? (
            <div className="flex gap-2 items-center">
              <div className="flex-1">
                <ContactSelector
                  value={newPrimaryContactId}
                  onChange={setNewPrimaryContactId}
                  disabled={isBusy}
                />
                <button
                  type="button"
                  onClick={() => {
                    setChangePrimaryMode('create');
                    setNewPrimaryContactId('');
                  }}
                  className="mt-1 flex items-center gap-1 text-xs text-primary-500 hover:text-primary-600 transition-colors"
                  disabled={isBusy}
                >
                  <UserPlus className="h-3 w-3" />
                  {t('form.createNewContact')}
                </button>
              </div>
              <button
                type="button"
                onClick={handleChangePrimary}
                className="px-3 py-2 bg-primary-500 text-white rounded hover:bg-primary-600 transition-colors text-sm whitespace-nowrap"
                disabled={isBusy || !newPrimaryContactId}
              >
                {t('common:buttons.confirm')}
              </button>
              <button
                type="button"
                onClick={resetChangePrimaryForm}
                className="p-2 text-text-secondary hover:bg-surface-inset rounded transition-colors"
                disabled={isBusy}
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <div className="space-y-2">
              <InlineContactForm
                value={inlineNewPrimary}
                onChange={(data) => {
                  setInlineNewPrimary(data);
                  setErrors({});
                }}
                onSwitchToSelect={() => {
                  setChangePrimaryMode('select');
                  setInlineNewPrimary({ ...EMPTY_NEW_CONTACT });
                  setErrors({});
                }}
                errors={errors}
                errorPrefix="change_primary"
                disabled={isBusy}
                onBlockingChange={handleDupBlocking('change_primary')}
              />
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={resetChangePrimaryForm}
                  className="px-3 py-2 border border-border-strong rounded text-sm hover:bg-surface-inset transition-colors"
                  disabled={isBusy}
                >
                  {t('common:buttons.cancel')}
                </button>
                <div className="relative group/confirm-primary">
                  <button
                    type="button"
                    onClick={handleChangePrimary}
                    className="px-3 py-2 bg-primary-500 text-white rounded hover:bg-primary-600 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                    disabled={isBusy || dupBlockers['change_primary']}
                  >
                    {t('common:buttons.confirm')}
                  </button>
                  {dupBlockers['change_primary'] && (
                    <div className="absolute bottom-full right-0 mb-2 px-3 py-2 text-xs font-medium text-white bg-neutral-800 dark:bg-neutral-700 rounded-lg whitespace-nowrap opacity-0 group-hover/confirm-primary:opacity-100 transition-opacity duration-150 shadow-lg pointer-events-none">
                      {t('form.dismissDuplicateFirst')}
                      <div className="absolute top-full right-4 -mt-px border-4 border-transparent border-t-neutral-800 dark:border-t-neutral-700" />
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Add party form */}
      {showAddForm && (
        <div>
          {addMode === 'select' ? (
            <div>
              <div className="flex gap-2 items-center">
                <div className="flex-1">
                  <ContactSelector
                    value={newContactId}
                    onChange={setNewContactId}
                    disabled={isBusy}
                  />
                </div>
                <select
                  value={newRole}
                  onChange={(e) =>
                    setNewRole(e.target.value as ContractPartyRole)
                  }
                  className="w-40 border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary text-sm"
                  disabled={isBusy}
                >
                  {ADDITIONAL_ROLES.map((role) => (
                    <option key={role} value={role}>
                      {t(`enums.partyRoles.${role}`, role)}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={handleAddParty}
                  className="px-3 py-2 bg-primary-500 text-white rounded hover:bg-primary-600 transition-colors text-sm whitespace-nowrap"
                  disabled={isBusy || !newContactId}
                >
                  {t('common:buttons.add')}
                </button>
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="p-2 text-text-secondary hover:bg-surface-inset rounded transition-colors"
                  disabled={isBusy}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <button
                type="button"
                onClick={() => {
                  setAddMode('create');
                  setNewContactId('');
                }}
                className="mt-1 flex items-center gap-1 text-xs text-primary-500 hover:text-primary-600 transition-colors"
                disabled={isBusy}
              >
                <UserPlus className="h-3 w-3" />
                {t('form.createNewContact')}
              </button>
            </div>
          ) : (
            <div>
              <div className="flex gap-2 items-center mb-2">
                <select
                  value={newRole}
                  onChange={(e) =>
                    setNewRole(e.target.value as ContractPartyRole)
                  }
                  className="w-40 border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 bg-surface-card text-text-primary text-sm"
                  disabled={isBusy}
                >
                  {ADDITIONAL_ROLES.map((role) => (
                    <option key={role} value={role}>
                      {t(`enums.partyRoles.${role}`, role)}
                    </option>
                  ))}
                </select>
                <div className="flex-1" />
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="p-2 text-text-secondary hover:bg-surface-inset rounded transition-colors"
                  disabled={isBusy}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <InlineContactForm
                value={inlineNewContact}
                onChange={(data) => {
                  setInlineNewContact(data);
                  setErrors({});
                }}
                onSwitchToSelect={() => {
                  setAddMode('select');
                  setInlineNewContact({ ...EMPTY_NEW_CONTACT });
                  setErrors({});
                }}
                errors={errors}
                errorPrefix="add_party"
                disabled={isBusy}
                onBlockingChange={handleDupBlocking('add_party')}
              />
              <div className="flex gap-2 justify-end mt-2">
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="px-3 py-2 border border-border-strong rounded text-sm hover:bg-surface-inset transition-colors"
                  disabled={isBusy}
                >
                  {t('common:buttons.cancel')}
                </button>
                <div className="relative group/confirm-add">
                  <button
                    type="button"
                    onClick={handleAddParty}
                    className="px-3 py-2 bg-primary-500 text-white rounded hover:bg-primary-600 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                    disabled={isBusy || dupBlockers['add_party']}
                  >
                    {t('common:buttons.add')}
                  </button>
                  {dupBlockers['add_party'] && (
                    <div className="absolute bottom-full right-0 mb-2 px-3 py-2 text-xs font-medium text-white bg-neutral-800 dark:bg-neutral-700 rounded-lg whitespace-nowrap opacity-0 group-hover/confirm-add:opacity-100 transition-opacity duration-150 shadow-lg pointer-events-none">
                      {t('form.dismissDuplicateFirst')}
                      <div className="absolute top-full right-4 -mt-px border-4 border-transparent border-t-neutral-800 dark:border-t-neutral-700" />
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {!showAddForm && !changingPrimary && (
        <div className="flex items-center gap-4">
          {ADDITIONAL_ROLES.map((role) => (
            <button
              key={role}
              type="button"
              onClick={() => {
                setNewRole(role);
                setShowAddForm(true);
              }}
              className="flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600 transition-colors"
              disabled={isBusy}
            >
              <Plus className="h-4 w-4" />
              {t('common:buttons.add')} {t(`enums.partyRoles.${role}`, role)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
