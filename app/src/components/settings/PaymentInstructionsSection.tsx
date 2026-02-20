import { useState } from 'react';
import {
  Plus,
  Edit,
  Trash2,
  CreditCard,
  Star,
  Building,
  Loader2,
} from 'lucide-react';
import { Button } from '../ui';
import { RichTextEditor } from '../common/RichTextEditor';
import { RichTextDisplay } from '../common/RichTextDisplay';
import {
  usePaymentInstructions,
  useCreatePaymentInstruction,
  useUpdatePaymentInstruction,
  useDeletePaymentInstruction,
} from '../../hooks/usePaymentInstructionHooks';
import {
  PaymentMethod,
  PaymentMethodLabels,
  PaymentInstructionResponse,
  CreatePaymentInstructionRequest,
  UpdatePaymentInstructionRequest,
} from '../../types/paymentInstruction';
import { useTeam } from '../../context/TeamContext';

const emptyForm: CreatePaymentInstructionRequest = {
  name: '',
  paymentMethod: PaymentMethod.BANK_TRANSFER,
  description: '',
  bankName: '',
  accountHolderName: '',
  iban: '',
  bicSwift: '',
  accountNumber: '',
  routingNumber: '',
  paymentReference: '',
  additionalDetails: '',
  isDefault: false,
};

export const PaymentInstructionsSection = () => {
  const { data: instructions = [], isLoading } = usePaymentInstructions();
  const createMutation = useCreatePaymentInstruction();
  const deleteMutation = useDeletePaymentInstruction();
  const { canEditData } = useTeam();

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] =
    useState<CreatePaymentInstructionRequest>(emptyForm);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  const handleCreate = () => {
    setEditingId(null);
    setFormData(emptyForm);
    setShowForm(true);
  };

  const handleEdit = (pi: PaymentInstructionResponse) => {
    setEditingId(pi.identifier);
    setFormData({
      name: pi.name,
      paymentMethod: pi.paymentMethod,
      description: pi.description || '',
      bankName: pi.bankName || '',
      accountHolderName: pi.accountHolderName || '',
      iban: pi.iban || '',
      bicSwift: pi.bicSwift || '',
      accountNumber: pi.accountNumber || '',
      routingNumber: pi.routingNumber || '',
      paymentReference: pi.paymentReference || '',
      additionalDetails: pi.additionalDetails || '',
      isDefault: pi.isDefault,
    });
    setShowForm(true);
  };

  const submitForm = () => {
    if (editingId) {
      // handled by child component
    } else {
      createMutation.mutate(formData, {
        onSuccess: () => {
          setShowForm(false);
          setFormData(emptyForm);
        },
      });
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

  const handleDelete = (id: string) => {
    deleteMutation.mutate(id, {
      onSuccess: () => setConfirmDeleteId(null),
    });
  };

  const showBankFields =
    formData.paymentMethod === PaymentMethod.BANK_TRANSFER ||
    formData.paymentMethod === PaymentMethod.DIRECT_DEBIT ||
    formData.paymentMethod === PaymentMethod.IDEAL_WERO ||
    formData.paymentMethod === PaymentMethod.ZELLE;

  if (isLoading) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-12 text-center">
        <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary-500" />
        <p className="mt-2 text-[#6b7194] dark:text-[#8b90a8]">Loading...</p>
      </div>
    );
  }

  return (
    <div className="mt-6 space-y-6">
      <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-6">
        <div className="flex items-center gap-3 mb-2">
          <CreditCard className="h-6 w-6 text-primary-500 dark:text-primary-300" />
          <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Payment Instructions
          </h2>
        </div>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
          Define reusable payment instruction templates for your contracts
        </p>
        {canEditData && (
          <Button variant="primary" leftIcon={<Plus />} onClick={handleCreate}>
            Add Instruction
          </Button>
        )}
      </div>

      {/* Form Modal */}
      {showForm && (
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            {editingId ? 'Edit Payment Instruction' : 'New Payment Instruction'}
          </h3>
          {editingId ? (
            <PaymentInstructionEditForm
              identifier={editingId}
              initialData={formData}
              onCancel={() => {
                setShowForm(false);
                setEditingId(null);
              }}
            />
          ) : (
            <form
              onSubmit={handleSubmit}
              onKeyDown={handleCmdEnter}
              className="space-y-4"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                    Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) =>
                      setFormData({ ...formData, name: e.target.value })
                    }
                    className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                    placeholder="e.g., Main Bank Account"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                    Payment Instruction *
                  </label>
                  <select
                    required
                    value={formData.paymentMethod}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        paymentMethod: e.target.value as PaymentMethod,
                      })
                    }
                    className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                  >
                    {Object.entries(PaymentMethodLabels).map(([val, label]) => (
                      <option key={val} value={val}>
                        {label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Description
                </label>
                <RichTextEditor
                  value={formData.description || ''}
                  onChange={(val) =>
                    setFormData({ ...formData, description: val })
                  }
                  placeholder="Instructions for the tenant..."
                  onSubmit={submitForm}
                />
              </div>

              {showBankFields && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      Bank Name
                    </label>
                    <input
                      type="text"
                      value={formData.bankName}
                      onChange={(e) =>
                        setFormData({ ...formData, bankName: e.target.value })
                      }
                      className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      Account Holder
                    </label>
                    <input
                      type="text"
                      value={formData.accountHolderName}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          accountHolderName: e.target.value,
                        })
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
                      value={formData.iban}
                      onChange={(e) =>
                        setFormData({ ...formData, iban: e.target.value })
                      }
                      maxLength={34}
                      className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      BIC / SWIFT
                    </label>
                    <input
                      type="text"
                      value={formData.bicSwift}
                      onChange={(e) =>
                        setFormData({ ...formData, bicSwift: e.target.value })
                      }
                      maxLength={11}
                      className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      Account Number
                    </label>
                    <input
                      type="text"
                      value={formData.accountNumber}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          accountNumber: e.target.value,
                        })
                      }
                      className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                      Routing Number
                    </label>
                    <input
                      type="text"
                      value={formData.routingNumber}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          routingNumber: e.target.value,
                        })
                      }
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
                  value={formData.paymentReference}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      paymentReference: e.target.value,
                    })
                  }
                  className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
                  placeholder="Reference to include with payment"
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="isDefault"
                  checked={formData.isDefault || false}
                  onChange={(e) =>
                    setFormData({ ...formData, isDefault: e.target.checked })
                  }
                  className="rounded border-[#e2e6f0] dark:border-[#2a2e3f]"
                />
                <label
                  htmlFor="isDefault"
                  className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
                >
                  Set as default payment method
                </label>
              </div>

              <div className="flex gap-2 justify-end">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setShowForm(false);
                    setFormData(emptyForm);
                  }}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Creating...' : 'Create'}
                </Button>
              </div>
            </form>
          )}
        </div>
      )}

      {/* Instruction List */}
      {instructions.length === 0 && !showForm ? (
        <div className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm p-12 text-center">
          <CreditCard className="h-12 w-12 mx-auto text-[#9ca0b8] dark:text-[#5c6180] mb-3" />
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-1">
            No Payment Instructions
          </h3>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
            Add your first payment method to use with contracts.
          </p>
          {canEditData && (
            <Button
              variant="primary"
              leftIcon={<Plus />}
              onClick={handleCreate}
            >
              Add Payment Instruction
            </Button>
          )}
        </div>
      ) : (
        <div className="grid gap-4">
          {instructions.map((pi) => (
            <div
              key={pi.identifier}
              className="bg-white dark:bg-[#14161f] rounded-xl shadow-sm border border-[#e2e6f0] dark:border-[#2a2e3f] p-5"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-[#f1f3f9] dark:bg-[#1e2130]">
                    {pi.paymentMethod === 'BANK_TRANSFER' ||
                    pi.paymentMethod === 'DIRECT_DEBIT' ||
                    pi.paymentMethod === 'IDEAL_WERO' ||
                    pi.paymentMethod === 'ZELLE' ? (
                      <Building className="h-5 w-5 text-primary-500 dark:text-primary-300" />
                    ) : (
                      <CreditCard className="h-5 w-5 text-primary-500 dark:text-primary-300" />
                    )}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                        {pi.name}
                      </h3>
                      {pi.isDefault && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300">
                          <Star className="h-3 w-3" />
                          Default
                        </span>
                      )}
                      <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]">
                        {PaymentMethodLabels[
                          pi.paymentMethod as PaymentMethod
                        ] || pi.paymentMethod}
                      </span>
                    </div>
                    {pi.description && (
                      <RichTextDisplay
                        content={pi.description}
                        className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1"
                      />
                    )}
                    {pi.iban && (
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1 font-mono">
                        IBAN: {pi.iban.replace(/(.{4})/g, '$1 ').trim()}
                      </p>
                    )}
                    {pi.accountNumber && (
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1 font-mono">
                        Account: {pi.accountNumber}
                        {pi.routingNumber && ` / Routing: ${pi.routingNumber}`}
                      </p>
                    )}
                    {pi.accountHolderName && (
                      <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                        Holder: {pi.accountHolderName}
                      </p>
                    )}
                  </div>
                </div>
                {canEditData && (
                  <div className="flex gap-1">
                    <button
                      onClick={() => handleEdit(pi)}
                      className="p-2 rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] hover:text-primary-500"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    {confirmDeleteId === pi.identifier ? (
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleDelete(pi.identifier)}
                          className="px-2 py-1 text-xs font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                          disabled={deleteMutation.isPending}
                        >
                          Confirm
                        </button>
                        <button
                          onClick={() => setConfirmDeleteId(null)}
                          className="px-2 py-1 text-xs text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] rounded"
                        >
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => setConfirmDeleteId(pi.identifier)}
                        className="p-2 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// Sub-component for editing (uses its own mutation hook with the identifier)
const PaymentInstructionEditForm = ({
  identifier,
  initialData,
  onCancel,
}: {
  identifier: string;
  initialData: CreatePaymentInstructionRequest;
  onCancel: () => void;
}) => {
  const updateMutation = useUpdatePaymentInstruction(identifier);
  const [formData, setFormData] =
    useState<UpdatePaymentInstructionRequest>(initialData);

  const showBankFields =
    formData.paymentMethod === PaymentMethod.BANK_TRANSFER ||
    formData.paymentMethod === PaymentMethod.DIRECT_DEBIT ||
    formData.paymentMethod === PaymentMethod.IDEAL_WERO ||
    formData.paymentMethod === PaymentMethod.ZELLE;

  const submitForm = () => {
    updateMutation.mutate(formData, { onSuccess: onCancel });
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

  return (
    <form
      onSubmit={handleSubmit}
      onKeyDown={handleCmdEnter}
      className="space-y-4"
    >
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Name
          </label>
          <input
            type="text"
            value={formData.name || ''}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
            Payment Instruction
          </label>
          <select
            value={formData.paymentMethod || PaymentMethod.BANK_TRANSFER}
            onChange={(e) =>
              setFormData({
                ...formData,
                paymentMethod: e.target.value as PaymentMethod,
              })
            }
            className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
          >
            {Object.entries(PaymentMethodLabels).map(([val, label]) => (
              <option key={val} value={val}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
          Description
        </label>
        <RichTextEditor
          value={formData.description || ''}
          onChange={(val) => setFormData({ ...formData, description: val })}
          placeholder="Instructions for the tenant..."
          onSubmit={submitForm}
        />
      </div>

      {showBankFields && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Bank Name
            </label>
            <input
              type="text"
              value={formData.bankName || ''}
              onChange={(e) =>
                setFormData({ ...formData, bankName: e.target.value })
              }
              className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Account Holder
            </label>
            <input
              type="text"
              value={formData.accountHolderName || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  accountHolderName: e.target.value,
                })
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
              value={formData.iban || ''}
              onChange={(e) =>
                setFormData({ ...formData, iban: e.target.value })
              }
              maxLength={34}
              className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              BIC / SWIFT
            </label>
            <input
              type="text"
              value={formData.bicSwift || ''}
              onChange={(e) =>
                setFormData({ ...formData, bicSwift: e.target.value })
              }
              maxLength={11}
              className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Account Number
            </label>
            <input
              type="text"
              value={formData.accountNumber || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  accountNumber: e.target.value,
                })
              }
              className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6] font-mono"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
              Routing Number
            </label>
            <input
              type="text"
              value={formData.routingNumber || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  routingNumber: e.target.value,
                })
              }
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
          value={formData.paymentReference || ''}
          onChange={(e) =>
            setFormData({ ...formData, paymentReference: e.target.value })
          }
          className="w-full rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#1e2130] px-3 py-2 text-sm text-[#1a1d2e] dark:text-[#eef0f6]"
        />
      </div>

      <div className="flex items-center gap-2">
        <input
          type="checkbox"
          id="editIsDefault"
          checked={formData.isDefault || false}
          onChange={(e) =>
            setFormData({ ...formData, isDefault: e.target.checked })
          }
          className="rounded border-[#e2e6f0] dark:border-[#2a2e3f]"
        />
        <label
          htmlFor="editIsDefault"
          className="text-sm text-[#3d4463] dark:text-[#c4c8db]"
        >
          Set as default
        </label>
      </div>

      <div className="flex gap-2 justify-end">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={updateMutation.isPending}>
          {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
        </Button>
      </div>
    </form>
  );
};
