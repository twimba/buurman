import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save, Plus, Trash2, UserPlus, ArrowLeft } from 'lucide-react';
import {
  ContractResponse,
  CreateContractRequest,
  ContractType,
  ContractPartyRole,
  ContractPartyRequest,
  PaymentFrequency,
  PARTY_ROLE_LABELS,
} from '@/types/contract';
import { CreateTenantRequest } from '@/types/tenant';
import { RichTextEditor } from '@/components/common/RichTextEditor';
import { MoneyInput } from '@/components/common/MoneyInput';
import { PropertySelector } from '@/components/common/PropertySelector';
import { TenantSelector } from '@/components/common/TenantSelector';
import { PhoneInput, validatePhoneE164 } from '@/components/common/PhoneInput';
import CountryMetadataForm, {
  useCountryName,
} from '@/components/contracts/CountryMetadataForm';
import { useTeamDefaults } from '@/hooks/useTeamDefaults';
import { useProperties } from '@/hooks/usePropertyHooks';
import {
  useAddContractParty,
  useRemoveContractParty,
  useChangePrimaryTenant,
} from '@/hooks/useContractHooks';

interface ContractFormProps {
  contract?: ContractResponse;
  onSubmit: (data: CreateContractRequest) => Promise<void>;
  isLoading: boolean;
  prefilledPropertyId?: string;
  prefilledTenantId?: string;
}

const ADDITIONAL_ROLES = [
  ContractPartyRole.GUARANTOR,
  ContractPartyRole.COSIGNER,
  ContractPartyRole.EXTRA_TENANT,
];

const EMPTY_NEW_TENANT: CreateTenantRequest = {
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
  tenantIdentifier: string;
  newTenant: CreateTenantRequest;
  role: ContractPartyRole;
}

// --- Inline tenant form for creating a new tenant within a party slot ---
const InlineTenantForm = ({
  value,
  onChange,
  onSwitchToSelect,
  errors,
  errorPrefix,
  disabled,
}: {
  value: CreateTenantRequest;
  onChange: (data: CreateTenantRequest) => void;
  onSwitchToSelect: () => void;
  errors: Record<string, string>;
  errorPrefix: string;
  disabled?: boolean;
}) => {
  const inputClass =
    'w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] text-sm';

  return (
    <div className="rounded-lg border border-[#d4dae6] dark:border-[#3a3f54] bg-[#f8f9fc] dark:bg-[#14161f] p-4 space-y-3">
      <button
        type="button"
        onClick={onSwitchToSelect}
        className="flex items-center gap-1 text-xs text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
        disabled={disabled}
      >
        <ArrowLeft className="h-3 w-3" />
        Select existing tenant
      </button>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            First Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={value.firstName}
            onChange={(e) => onChange({ ...value, firstName: e.target.value })}
            className={inputClass}
            placeholder="John"
            disabled={disabled}
          />
          {errors[`${errorPrefix}_firstName`] && (
            <p className="text-red-600 dark:text-red-400 text-xs mt-1">
              {errors[`${errorPrefix}_firstName`]}
            </p>
          )}
        </div>
        <div>
          <label className="block text-xs font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Last Name
          </label>
          <input
            type="text"
            value={value.lastName ?? ''}
            onChange={(e) => onChange({ ...value, lastName: e.target.value })}
            className={inputClass}
            placeholder="Doe"
            disabled={disabled}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Email
          </label>
          <input
            type="email"
            value={value.email}
            onChange={(e) => onChange({ ...value, email: e.target.value })}
            className={inputClass}
            placeholder="john@example.com"
            disabled={disabled}
          />
          {errors[`${errorPrefix}_email`] && (
            <p className="text-red-600 dark:text-red-400 text-xs mt-1">
              {errors[`${errorPrefix}_email`]}
            </p>
          )}
        </div>
        <div>
          <label className="block text-xs font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Phone
          </label>
          <PhoneInput
            value={value.phone ?? null}
            onChange={(e164) => onChange({ ...value, phone: e164 ?? '' })}
            error={errors[`${errorPrefix}_phone`]}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Tax Number
          </label>
          <input
            type="text"
            value={value.taxNumber ?? ''}
            onChange={(e) => onChange({ ...value, taxNumber: e.target.value })}
            className={inputClass}
            placeholder="123456789"
            disabled={disabled}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            ID Number
          </label>
          <input
            type="text"
            value={value.idNumber ?? ''}
            onChange={(e) => onChange({ ...value, idNumber: e.target.value })}
            className={inputClass}
            placeholder="AB123456"
            disabled={disabled}
          />
        </div>
      </div>
    </div>
  );
};

// Convert empty optional strings to null before submission
function sanitizeTenant(data: CreateTenantRequest): CreateTenantRequest {
  return {
    ...data,
    email: data.email?.trim() || undefined,
    phone: data.phone?.trim() || undefined,
  };
}

// Validate inline tenant data, return errors keyed by prefix
function validateInlineTenant(
  data: CreateTenantRequest,
  prefix: string
): Record<string, string> {
  const errs: Record<string, string> = {};
  if (!data.firstName.trim()) {
    errs[`${prefix}_firstName`] = 'Required';
  }
  if (
    data.email?.trim() &&
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email ?? '')
  ) {
    errs[`${prefix}_email`] = 'Invalid email';
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
  prefilledTenantId,
}: ContractFormProps) => {
  const navigate = useNavigate();
  const { defaultCurrency } = useTeamDefaults();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const isEditing = !!contract;

  // Primary tenant state (create mode)
  const [primaryMode, setPrimaryMode] = useState<'select' | 'create'>('select');
  const [primaryTenantId, setPrimaryTenantId] = useState(
    prefilledTenantId ?? ''
  );
  const [newPrimaryTenant, setNewPrimaryTenant] = useState<CreateTenantRequest>(
    { ...EMPTY_NEW_TENANT }
  );

  // Additional parties state (create mode)
  const [additionalParties, setAdditionalParties] = useState<PartyEntry[]>([]);

  const [formData, setFormData] = useState<
    Omit<CreateContractRequest, 'parties' | 'rentAmount'> & {
      rentAmount: number | '';
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
    autoRenewal: contract?.autoRenewal ?? false,
    renewalNoticeDays: contract?.renewalNoticeDays ?? 30,
    terminationNoticeDays: contract?.terminationNoticeDays ?? 30,
    lateFeePercentage: contract?.lateFeePercentage ?? undefined,
    termsAndConditions: contract?.termsAndConditions ?? '',
    notes: contract?.notes ?? '',
    countryMetadata: contract?.countryMetadata ?? undefined,
  });

  const [contractIdentifier, setContractIdentifier] = useState(
    contract?.identifier
  );

  // Look up selected property's country for metadata form
  const { data: propertiesPage } = useProperties();
  const selectedProperty = useMemo(() => {
    if (!propertiesPage?.content || !formData.propertyIdentifier) {
      return undefined;
    }
    return propertiesPage.content.find(
      (p) => p.identifier === formData.propertyIdentifier
    );
  }, [propertiesPage, formData.propertyIdentifier]);
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
        autoRenewal: contract.autoRenewal,
        renewalNoticeDays: contract.renewalNoticeDays ?? 30,
        terminationNoticeDays: contract.terminationNoticeDays ?? 30,
        lateFeePercentage: contract.lateFeePercentage ?? undefined,
        termsAndConditions: contract.termsAndConditions ?? '',
        notes: contract.notes ?? '',
        countryMetadata: contract.countryMetadata ?? undefined,
      });
    }
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [contract, contractIdentifier, defaultCurrency]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.propertyIdentifier) {
      newErrors.propertyIdentifier = 'Property is required';
    }

    // Validate primary tenant
    if (!isEditing) {
      if (primaryMode === 'select' && !primaryTenantId) {
        newErrors.primaryTenant = 'Primary tenant is required';
      } else if (primaryMode === 'create') {
        Object.assign(
          newErrors,
          validateInlineTenant(newPrimaryTenant, 'primary')
        );
      }
    }

    if (!formData.startDate) {
      newErrors.startDate = 'Start date is required';
    }
    if (!formData.rentAmount || formData.rentAmount <= 0) {
      newErrors.rentAmount = 'Rent amount must be greater than 0';
    }
    if (
      formData.rentAmount &&
      formData.rentAmount > 0 &&
      !(formData.rentAmountCurrency || defaultCurrency || '').trim()
    )
      newErrors.rentAmountCurrency = 'Rent currency is required';
    if (
      formData.depositAmount &&
      formData.depositAmount > 0 &&
      !(formData.depositAmountCurrency || defaultCurrency || '').trim()
    )
      newErrors.depositAmountCurrency = 'Deposit currency is required';
    if (
      formData.securityDeposit &&
      formData.securityDeposit > 0 &&
      !(formData.securityDepositCurrency || defaultCurrency || '').trim()
    )
      newErrors.securityDepositCurrency =
        'Security deposit currency is required';

    if (
      formData.contractType === ContractType.FIXED_TERM &&
      !formData.endDate
    ) {
      newErrors.endDate = 'End date is required for fixed-term contracts';
    }

    if (formData.startDate && formData.endDate) {
      if (new Date(formData.endDate) < new Date(formData.startDate)) {
        newErrors.endDate = 'End date must be on or after start date';
      }
    }

    if (
      formData.paymentDueDay &&
      (formData.paymentDueDay < 1 || formData.paymentDueDay > 31)
    ) {
      newErrors.paymentDueDay = 'Payment due day must be between 1 and 31';
    }

    // Validate additional parties
    if (!isEditing) {
      for (let i = 0; i < additionalParties.length; i++) {
        const p = additionalParties[i];
        if (p.mode === 'select' && !p.tenantIdentifier) {
          newErrors[`party_${i}`] = 'Tenant is required';
        } else if (p.mode === 'create') {
          Object.assign(
            newErrors,
            validateInlineTenant(p.newTenant, `party_${i}`)
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
                  tenantIdentifier: primaryTenantId,
                  role: ContractPartyRole.PRIMARY_TENANT,
                }
              : {
                  newTenant: sanitizeTenant(newPrimaryTenant),
                  role: ContractPartyRole.PRIMARY_TENANT,
                },
            ...additionalParties
              .filter(
                (p) =>
                  (p.mode === 'select' && p.tenantIdentifier) ||
                  p.mode === 'create'
              )
              .map((p) =>
                p.mode === 'select'
                  ? { tenantIdentifier: p.tenantIdentifier, role: p.role }
                  : { newTenant: sanitizeTenant(p.newTenant), role: p.role }
              ),
          ];

      await onSubmit({
        ...formData,
        endDate: formData.endDate || null,
        signedDate: formData.signedDate || null,
        parties,
      } as CreateContractRequest);
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

  const handleChange = (
    field: string,
    value: string | number | boolean | undefined
  ) => {
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
        tenantIdentifier: '',
        newTenant: { ...EMPTY_NEW_TENANT },
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
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-6"
    >
      {/* Property Selection */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          {isEditing ? 'Property' : 'Property & Parties'}
        </h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Property <span className="text-red-500">*</span>
            </label>
            <PropertySelector
              value={formData.propertyIdentifier}
              onChange={(value) => handleChange('propertyIdentifier', value)}
              disabled={isLoading}
            />
            {errors.propertyIdentifier && (
              <p className="text-red-600 text-sm mt-1">
                {errors.propertyIdentifier}
              </p>
            )}
          </div>

          {/* Parties section — create mode: inline editors */}
          {!isEditing && (
            <>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Primary Tenant <span className="text-red-500">*</span>
                </label>

                {primaryMode === 'select' ? (
                  <>
                    <TenantSelector
                      value={primaryTenantId}
                      onChange={(value) => {
                        setPrimaryTenantId(value);
                        if (errors.primaryTenant) {
                          setErrors((prev) => {
                            const next = { ...prev };
                            delete next.primaryTenant;
                            return next;
                          });
                        }
                      }}
                      disabled={isLoading}
                    />
                    {errors.primaryTenant && (
                      <p className="text-red-600 text-sm mt-1">
                        {errors.primaryTenant}
                      </p>
                    )}
                    <button
                      type="button"
                      onClick={() => {
                        setPrimaryMode('create');
                        setPrimaryTenantId('');
                        setErrors((prev) => {
                          const next = { ...prev };
                          delete next.primaryTenant;
                          return next;
                        });
                      }}
                      className="mt-2 flex items-center gap-1 text-xs text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
                      disabled={isLoading}
                    >
                      <UserPlus className="h-3 w-3" />
                      Create new tenant
                    </button>
                  </>
                ) : (
                  <InlineTenantForm
                    value={newPrimaryTenant}
                    onChange={(data) => {
                      setNewPrimaryTenant(data);
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
                      setNewPrimaryTenant({ ...EMPTY_NEW_TENANT });
                    }}
                    errors={errors}
                    errorPrefix="primary"
                    disabled={isLoading}
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
                          <TenantSelector
                            value={party.tenantIdentifier}
                            onChange={(value) =>
                              updatePartyField(index, {
                                tenantIdentifier: value,
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
                          className="w-40 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
                          disabled={isLoading}
                        >
                          {ADDITIONAL_ROLES.map((role) => (
                            <option key={role} value={role}>
                              {PARTY_ROLE_LABELS[role]}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          onClick={() => removeParty(index)}
                          className="p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
                          disabled={isLoading}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      {errors[`party_${index}`] && (
                        <p className="text-red-600 text-sm mt-1">
                          {errors[`party_${index}`]}
                        </p>
                      )}
                      <button
                        type="button"
                        onClick={() =>
                          updatePartyField(index, {
                            mode: 'create',
                            tenantIdentifier: '',
                          })
                        }
                        className="mt-1 flex items-center gap-1 text-xs text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
                        disabled={isLoading}
                      >
                        <UserPlus className="h-3 w-3" />
                        Create new tenant
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
                          className="w-40 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
                          disabled={isLoading}
                        >
                          {ADDITIONAL_ROLES.map((role) => (
                            <option key={role} value={role}>
                              {PARTY_ROLE_LABELS[role]}
                            </option>
                          ))}
                        </select>
                        <div className="flex-1" />
                        <button
                          type="button"
                          onClick={() => removeParty(index)}
                          className="p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
                          disabled={isLoading}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      <InlineTenantForm
                        value={party.newTenant}
                        onChange={(data) =>
                          updatePartyField(index, { newTenant: data })
                        }
                        onSwitchToSelect={() =>
                          updatePartyField(index, {
                            mode: 'select',
                            newTenant: { ...EMPTY_NEW_TENANT },
                          })
                        }
                        errors={errors}
                        errorPrefix={`party_${index}`}
                        disabled={isLoading}
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
                    className="flex items-center gap-1 text-sm text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
                    disabled={isLoading}
                  >
                    <Plus className="h-4 w-4" />
                    Add {PARTY_ROLE_LABELS[role]}
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
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            Contract Parties
          </h3>
          <ContractPartiesEditor contract={contract} isLoading={isLoading} />
        </div>
      )}

      {/* Contract Details */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Contract Details
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Contract Type <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.contractType}
              onChange={(e) =>
                handleChange('contractType', e.target.value as ContractType)
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              disabled={isLoading}
            >
              <option value={ContractType.FIXED_TERM}>Fixed Term</option>
              <option value={ContractType.INDEFINITE}>Indefinite</option>
              <option value={ContractType.FURNISHED}>Furnished</option>
              <option value={ContractType.UNFURNISHED}>Unfurnished</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Start Date <span className="text-red-500">*</span>
            </label>
            <div className="flex gap-2">
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => handleChange('startDate', e.target.value)}
                className="flex-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
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
                className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md transition-colors"
                disabled={isLoading}
              >
                Today
              </button>
            </div>
            {errors.startDate && (
              <p className="text-red-600 text-sm mt-1">{errors.startDate}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              End Date{' '}
              {formData.contractType === ContractType.FIXED_TERM && (
                <span className="text-red-500">*</span>
              )}
            </label>
            <input
              type="date"
              value={formData.endDate}
              onChange={(e) => handleChange('endDate', e.target.value)}
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              disabled={isLoading}
            />
            {errors.endDate && (
              <p className="text-red-600 text-sm mt-1">{errors.endDate}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Signed Date
            </label>
            <div className="flex gap-2">
              <input
                type="date"
                value={formData.signedDate}
                onChange={(e) => handleChange('signedDate', e.target.value)}
                className="flex-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
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
                className="px-3 py-2 text-sm bg-[#f1f3f9] dark:bg-[#1e2130] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54] border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md transition-colors"
                disabled={isLoading}
              >
                Today
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Financial Terms */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Financial Terms
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Rent Amount <span className="text-red-500">*</span>
            </label>
            <MoneyInput
              value={formData.rentAmount || undefined}
              onChange={(val) => handleChange('rentAmount', val ?? '')}
              currency={formData.rentAmountCurrency || defaultCurrency || ''}
              disabled={isLoading}
              error={!!errors.rentAmount || !!errors.rentAmountCurrency}
            />
            {(errors.rentAmount || errors.rentAmountCurrency) && (
              <p className="text-red-600 text-sm mt-1">
                {errors.rentAmount || errors.rentAmountCurrency}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Deposit Amount
            </label>
            <MoneyInput
              value={formData.depositAmount ?? undefined}
              onChange={(val) => handleChange('depositAmount', val)}
              currency={formData.depositAmountCurrency || defaultCurrency || ''}
              disabled={isLoading}
              error={!!errors.depositAmountCurrency}
            />
            {errors.depositAmountCurrency && (
              <p className="text-red-600 text-sm mt-1">
                {errors.depositAmountCurrency}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Security Deposit
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
              <p className="text-red-600 text-sm mt-1">
                {errors.securityDepositCurrency}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Payment Terms */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Payment Terms
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Payment Frequency <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.paymentFrequency}
              onChange={(e) =>
                handleChange(
                  'paymentFrequency',
                  e.target.value as PaymentFrequency
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              disabled={isLoading}
            >
              <option value={PaymentFrequency.MONTHLY}>Monthly</option>
              <option value={PaymentFrequency.QUARTERLY}>Quarterly</option>
              <option value={PaymentFrequency.ANNUALLY}>Annually</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Payment Due Day (1-31)
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
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="1"
              disabled={isLoading}
            />
            {errors.paymentDueDay && (
              <p className="text-red-600 text-sm mt-1">
                {errors.paymentDueDay}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Late Fee Percentage
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
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="2.5"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>

      {/* Renewal and Termination */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Renewal and Termination
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="flex items-center">
            <input
              type="checkbox"
              id="autoRenewal"
              checked={formData.autoRenewal}
              onChange={(e) => handleChange('autoRenewal', e.target.checked)}
              className="h-4 w-4 text-[#5c7cfa] focus:ring-blue-500 border-[#c9cfd9] rounded"
              disabled={isLoading}
            />
            <label
              htmlFor="autoRenewal"
              className="ml-2 block text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
            >
              Auto-renewal
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Renewal Notice Days
            </label>
            <input
              type="number"
              min="0"
              value={formData.renewalNoticeDays ?? ''}
              onChange={(e) =>
                handleChange(
                  'renewalNoticeDays',
                  e.target.value ? parseInt(e.target.value) : undefined
                )
              }
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="30"
              disabled={isLoading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Termination Notice Days
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
              className="w-full border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa]"
              placeholder="30"
              disabled={isLoading}
            />
          </div>
        </div>
      </div>

      {/* Terms and Conditions */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Terms and Conditions
        </h3>
        <div>
          <RichTextEditor
            value={formData.termsAndConditions ?? ''}
            onChange={(value) => handleChange('termsAndConditions', value)}
            placeholder="Enter contract terms and conditions"
            onSubmit={submitForm}
          />
        </div>
      </div>

      {/* Notes */}
      <div>
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Notes
        </h3>
        <div>
          <RichTextEditor
            value={formData.notes ?? ''}
            onChange={(value) => handleChange('notes', value)}
            placeholder="Add any additional notes"
            onSubmit={submitForm}
          />
        </div>
      </div>

      {/* Country-Specific Rental Details */}
      {propertyCountryCode ? (
        <div>
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
            {countryName
              ? `${countryName} Rental Details`
              : 'Country-Specific Details'}
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Regulatory fields specific to {countryName || propertyCountryCode}
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
        <p className="text-sm text-gray-400 dark:text-gray-500 italic">
          Set a country on the selected property to see country-specific
          regulatory fields.
        </p>
      ) : null}

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
        <button
          type="button"
          onClick={() => navigate('/contracts')}
          className="border border-[#c9cfd9] dark:border-[#3a3f54] px-4 py-2 rounded hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="bg-[#5c7cfa] text-white px-4 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading
            ? 'Saving...'
            : contract
              ? 'Update Contract'
              : 'Create Contract'}
        </button>
      </div>
    </form>
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
  const [showAddForm, setShowAddForm] = useState(false);
  const [addMode, setAddMode] = useState<'select' | 'create'>('select');
  const [changingPrimary, setChangingPrimary] = useState(false);
  const [changePrimaryMode, setChangePrimaryMode] = useState<
    'select' | 'create'
  >('select');
  const [newTenantId, setNewTenantId] = useState('');
  const [newRole, setNewRole] = useState<ContractPartyRole>(
    ContractPartyRole.GUARANTOR
  );
  const [newPrimaryTenantId, setNewPrimaryTenantId] = useState('');
  const [inlineNewTenant, setInlineNewTenant] = useState<CreateTenantRequest>({
    ...EMPTY_NEW_TENANT,
  });
  const [inlineNewPrimary, setInlineNewPrimary] = useState<CreateTenantRequest>(
    { ...EMPTY_NEW_TENANT }
  );
  const [errors, setErrors] = useState<Record<string, string>>({});

  const addPartyMutation = useAddContractParty(contract.identifier);
  const removePartyMutation = useRemoveContractParty(contract.identifier);
  const changePrimaryMutation = useChangePrimaryTenant(contract.identifier);

  const handleAddParty = async () => {
    try {
      if (addMode === 'select') {
        if (!newTenantId) {
          return;
        }
        await addPartyMutation.mutateAsync({
          tenantIdentifier: newTenantId,
          role: newRole,
        });
      } else {
        const validationErrors = validateInlineTenant(
          inlineNewTenant,
          'add_party'
        );
        if (Object.keys(validationErrors).length > 0) {
          setErrors(validationErrors);
          return;
        }
        await addPartyMutation.mutateAsync({
          newTenant: sanitizeTenant(inlineNewTenant),
          role: newRole,
        });
      }
      resetAddForm();
    } catch {
      // Error is handled by mutation's onError callback (toast)
    }
  };

  const resetAddForm = () => {
    setNewTenantId('');
    setNewRole(ContractPartyRole.GUARANTOR);
    setAddMode('select');
    setInlineNewTenant({ ...EMPTY_NEW_TENANT });
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
        if (!newPrimaryTenantId) {
          return;
        }
        await changePrimaryMutation.mutateAsync({
          tenantIdentifier: newPrimaryTenantId,
        });
      } else {
        const validationErrors = validateInlineTenant(
          inlineNewPrimary,
          'change_primary'
        );
        if (Object.keys(validationErrors).length > 0) {
          setErrors(validationErrors);
          return;
        }
        await changePrimaryMutation.mutateAsync({
          newTenant: sanitizeTenant(inlineNewPrimary),
        });
      }
      resetChangePrimaryForm();
    } catch {
      // Error is handled by mutation's onError callback (toast)
    }
  };

  const resetChangePrimaryForm = () => {
    setNewPrimaryTenantId('');
    setChangePrimaryMode('select');
    setInlineNewPrimary({ ...EMPTY_NEW_TENANT });
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
          className="flex items-center gap-3 p-3 bg-[#f8f9fc] dark:bg-[#1a1d2e] rounded-lg"
        >
          <div className="flex-1 min-w-0">
            <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {party.tenant.firstName} {party.tenant.lastName}
            </span>
            <span className="ml-2 text-xs px-2 py-0.5 rounded-full bg-[#e8ecf4] dark:bg-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8]">
              {PARTY_ROLE_LABELS[party.role]}
            </span>
          </div>
          {party.role === ContractPartyRole.PRIMARY_TENANT ? (
            <button
              type="button"
              onClick={() => setChangingPrimary(true)}
              className="text-xs text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors whitespace-nowrap"
              disabled={isBusy}
            >
              Change
            </button>
          ) : (
            <button
              type="button"
              onClick={() => handleRemoveParty(party.identifier)}
              className="p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
              disabled={isBusy}
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      ))}

      {/* Change primary tenant form */}
      {changingPrimary && (
        <div className="space-y-2">
          {changePrimaryMode === 'select' ? (
            <div className="flex gap-2 items-center">
              <div className="flex-1">
                <TenantSelector
                  value={newPrimaryTenantId}
                  onChange={setNewPrimaryTenantId}
                  disabled={isBusy}
                />
                <button
                  type="button"
                  onClick={() => {
                    setChangePrimaryMode('create');
                    setNewPrimaryTenantId('');
                  }}
                  className="mt-1 flex items-center gap-1 text-xs text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
                  disabled={isBusy}
                >
                  <UserPlus className="h-3 w-3" />
                  Create new tenant
                </button>
              </div>
              <button
                type="button"
                onClick={handleChangePrimary}
                className="px-3 py-2 bg-[#5c7cfa] text-white rounded hover:bg-[#4c6ef5] transition-colors text-sm whitespace-nowrap"
                disabled={isBusy || !newPrimaryTenantId}
              >
                Confirm
              </button>
              <button
                type="button"
                onClick={resetChangePrimaryForm}
                className="p-2 text-[#6b7194] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
                disabled={isBusy}
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <div className="space-y-2">
              <InlineTenantForm
                value={inlineNewPrimary}
                onChange={(data) => {
                  setInlineNewPrimary(data);
                  setErrors({});
                }}
                onSwitchToSelect={() => {
                  setChangePrimaryMode('select');
                  setInlineNewPrimary({ ...EMPTY_NEW_TENANT });
                  setErrors({});
                }}
                errors={errors}
                errorPrefix="change_primary"
                disabled={isBusy}
              />
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={resetChangePrimaryForm}
                  className="px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                  disabled={isBusy}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleChangePrimary}
                  className="px-3 py-2 bg-[#5c7cfa] text-white rounded hover:bg-[#4c6ef5] transition-colors text-sm"
                  disabled={isBusy}
                >
                  Confirm
                </button>
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
                  <TenantSelector
                    value={newTenantId}
                    onChange={setNewTenantId}
                    disabled={isBusy}
                  />
                </div>
                <select
                  value={newRole}
                  onChange={(e) =>
                    setNewRole(e.target.value as ContractPartyRole)
                  }
                  className="w-40 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
                  disabled={isBusy}
                >
                  {ADDITIONAL_ROLES.map((role) => (
                    <option key={role} value={role}>
                      {PARTY_ROLE_LABELS[role]}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={handleAddParty}
                  className="px-3 py-2 bg-[#5c7cfa] text-white rounded hover:bg-[#4c6ef5] transition-colors text-sm whitespace-nowrap"
                  disabled={isBusy || !newTenantId}
                >
                  Add
                </button>
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="p-2 text-[#6b7194] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
                  disabled={isBusy}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <button
                type="button"
                onClick={() => {
                  setAddMode('create');
                  setNewTenantId('');
                }}
                className="mt-1 flex items-center gap-1 text-xs text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
                disabled={isBusy}
              >
                <UserPlus className="h-3 w-3" />
                Create new tenant
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
                  className="w-40 border border-[#c9cfd9] dark:border-[#3a3f54] rounded px-3 py-2 focus:border-[#5c7cfa] focus:ring-1 focus:ring-[#5c7cfa] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] text-sm"
                  disabled={isBusy}
                >
                  {ADDITIONAL_ROLES.map((role) => (
                    <option key={role} value={role}>
                      {PARTY_ROLE_LABELS[role]}
                    </option>
                  ))}
                </select>
                <div className="flex-1" />
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="p-2 text-[#6b7194] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded transition-colors"
                  disabled={isBusy}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <InlineTenantForm
                value={inlineNewTenant}
                onChange={(data) => {
                  setInlineNewTenant(data);
                  setErrors({});
                }}
                onSwitchToSelect={() => {
                  setAddMode('select');
                  setInlineNewTenant({ ...EMPTY_NEW_TENANT });
                  setErrors({});
                }}
                errors={errors}
                errorPrefix="add_party"
                disabled={isBusy}
              />
              <div className="flex gap-2 justify-end mt-2">
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
                  disabled={isBusy}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleAddParty}
                  className="px-3 py-2 bg-[#5c7cfa] text-white rounded hover:bg-[#4c6ef5] transition-colors text-sm"
                  disabled={isBusy}
                >
                  Add
                </button>
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
              className="flex items-center gap-1 text-sm text-[#5c7cfa] hover:text-[#4c6ef5] transition-colors"
              disabled={isBusy}
            >
              <Plus className="h-4 w-4" />
              Add {PARTY_ROLE_LABELS[role]}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
