import { useState } from 'react';
import { AlertTriangle, Pencil, Plus, Trash2, X } from 'lucide-react';
import { Button, ConfirmDialog } from '@buurman/ui';
import {
  useLeaseClauseTemplates,
  useCreateLeaseClauseTemplate,
  useUpdateLeaseClauseTemplate,
  useDeleteLeaseClauseTemplate,
} from '../hooks/useLeaseClauseTemplateHooks';
import { LoadingSpinner } from '../components/LoadingSpinner';
import type { LeaseClauseTemplateResponse } from '../generated/models';

// The 7 countries seeded with placeholder lease clause templates (BUUR-105).
const COUNTRIES = [
  { code: 'NL', name: 'Netherlands' },
  { code: 'DE', name: 'Germany' },
  { code: 'FR', name: 'France' },
  { code: 'ES', name: 'Spain' },
  { code: 'PT', name: 'Portugal' },
  { code: 'BE', name: 'Belgium' },
  { code: 'GB', name: 'United Kingdom' },
];

const TH_CLASS =
  'text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary';

const INPUT_CLASS =
  'w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';

interface TemplateForm {
  clauseKey: string;
  titleI18nKey: string;
  bodyI18nKey: string;
  sortOrder: number;
  optional: boolean;
  defaultIncluded: boolean;
}

const emptyForm = (countryCode: string): TemplateForm & { countryCode: string } => ({
  countryCode,
  clauseKey: '',
  titleI18nKey: '',
  bodyI18nKey: '',
  sortOrder: 0,
  optional: true,
  defaultIncluded: true,
});

/**
 * Backoffice management of the lease clause template library (BUUR-105). The clause bodies
 * seeded with this feature are placeholder legal-boilerplate text, not vetted legal content — see
 * the banner below, which is load-bearing per the feature's own design doc and must stay visible
 * and non-dismissible on this page.
 */
export const LeaseClauseTemplatesPage = () => {
  const [countryCode, setCountryCode] = useState(COUNTRIES[0].code);
  const { data: templates, isLoading } = useLeaseClauseTemplates(countryCode);
  const createTemplate = useCreateLeaseClauseTemplate(countryCode);
  const updateTemplate = useUpdateLeaseClauseTemplate(countryCode);
  const deleteTemplate = useDeleteLeaseClauseTemplate(countryCode);

  const [showForm, setShowForm] = useState(false);
  const [editingTemplate, setEditingTemplate] =
    useState<LeaseClauseTemplateResponse | null>(null);
  const [form, setForm] = useState(emptyForm(countryCode));
  const [deleteTarget, setDeleteTarget] =
    useState<LeaseClauseTemplateResponse | null>(null);

  const openCreate = () => {
    setEditingTemplate(null);
    setForm(emptyForm(countryCode));
    setShowForm(true);
  };

  const openEdit = (template: LeaseClauseTemplateResponse) => {
    setEditingTemplate(template);
    setForm({
      countryCode: template.countryCode,
      clauseKey: template.clauseKey,
      titleI18nKey: template.titleI18nKey,
      bodyI18nKey: template.bodyI18nKey,
      sortOrder: template.sortOrder,
      optional: template.optional,
      defaultIncluded: template.defaultIncluded,
    });
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingTemplate(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const data = {
      countryCode: form.countryCode,
      clauseKey: form.clauseKey,
      titleI18nKey: form.titleI18nKey,
      bodyI18nKey: form.bodyI18nKey,
      sortOrder: form.sortOrder,
      optional: form.optional,
      defaultIncluded: form.defaultIncluded,
    };
    if (editingTemplate) {
      updateTemplate.mutate(
        { identifier: editingTemplate.identifier, data },
        { onSuccess: closeForm }
      );
    } else {
      createTemplate.mutate(data, { onSuccess: closeForm });
    }
  };

  const handleDelete = () => {
    if (!deleteTarget) {
      return;
    }
    deleteTemplate.mutate(deleteTarget.identifier, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  const isSaving = createTemplate.isPending || updateTemplate.isPending;
  const sortedTemplates = (templates ?? [])
    .slice()
    .sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div>
      {/* Legal content disclaimer — persistent and non-dismissible per the feature's design doc:
          "do not ship this page without it". No close button, no auto-hide. */}
      <div
        role="alert"
        className="mb-6 flex items-start gap-3 rounded-lg border border-warning-border bg-warning-bg px-4 py-3"
      >
        <AlertTriangle className="h-5 w-5 flex-shrink-0 text-warning-text mt-0.5" />
        <p className="text-sm text-warning-text">
          Clause bodies reference placeholder i18n keys with example
          legal-boilerplate text. This is not vetted legal content — review
          with qualified counsel before relying on generated leases in
          production.
        </p>
      </div>

      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">
            Lease Clause Templates
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage the lease clause library by country. Bodies are i18n key
            references — edit the actual translated text in the
            .properties bundles.
          </p>
        </div>
        <Button
          variant="primary"
          size="sm"
          leftIcon={<Plus />}
          onClick={openCreate}
        >
          Add Clause Template
        </Button>
      </div>

      {/* Country filter */}
      <div className="mb-4 max-w-xs">
        <label className="block text-sm font-medium text-text-secondary mb-1">
          Country
        </label>
        <select
          value={countryCode}
          onChange={(e) => setCountryCode(e.target.value)}
          className={INPUT_CLASS}
        >
          {COUNTRIES.map((c) => (
            <option key={c.code} value={c.code}>
              {c.name} ({c.code})
            </option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <LoadingSpinner message="Loading clause templates..." />
      ) : (
        <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-default">
                  <th className={TH_CLASS}>Order</th>
                  <th className={TH_CLASS}>Clause Key</th>
                  <th className={TH_CLASS}>Title Key</th>
                  <th className={TH_CLASS}>Body Key</th>
                  <th className={TH_CLASS}>Optional</th>
                  <th className={TH_CLASS}>Default Included</th>
                  <th className={TH_CLASS}>Version</th>
                  <th className={`${TH_CLASS} text-right`}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sortedTemplates.length === 0 ? (
                  <tr>
                    <td
                      colSpan={8}
                      className="px-4 py-12 text-center text-sm text-text-muted"
                    >
                      No clause templates for this country yet.
                    </td>
                  </tr>
                ) : (
                  sortedTemplates.map((template) => (
                    <tr
                      key={template.identifier}
                      className="border-b border-border-default last:border-b-0"
                    >
                      <td className="px-4 py-3 text-sm text-text-secondary">
                        {template.sortOrder}
                      </td>
                      <td className="px-4 py-3 text-sm font-medium text-text-primary">
                        {template.clauseKey}
                      </td>
                      <td className="px-4 py-3 text-sm font-mono text-text-secondary">
                        {template.titleI18nKey}
                      </td>
                      <td className="px-4 py-3 text-sm font-mono text-text-secondary">
                        {template.bodyI18nKey}
                      </td>
                      <td className="px-4 py-3 text-sm text-text-secondary">
                        {template.optional ? 'Yes' : 'No'}
                      </td>
                      <td className="px-4 py-3 text-sm text-text-secondary">
                        {template.defaultIncluded ? 'Yes' : 'No'}
                      </td>
                      <td className="px-4 py-3 text-sm text-text-secondary">
                        v{template.version}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => openEdit(template)}
                            className="p-1.5 rounded-md text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 transition-colors"
                            title="Edit"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => setDeleteTarget(template)}
                            className="p-1.5 rounded-md text-text-secondary hover:text-error-text hover:bg-error-bg transition-colors"
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Template form modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black/40" onClick={closeForm} />
          <div className="relative bg-surface-card rounded-lg border border-border-default shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
              <h2 className="text-lg font-semibold text-text-primary">
                {editingTemplate ? 'Edit Clause Template' : 'Add Clause Template'}
              </h2>
              <button
                onClick={closeForm}
                className="p-1 rounded-md text-text-secondary hover:text-text-primary hover:bg-surface-inset transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Clause Key
                </label>
                <input
                  type="text"
                  required
                  disabled={!!editingTemplate}
                  value={form.clauseKey}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, clauseKey: e.target.value }))
                  }
                  placeholder="e.g. parties"
                  className={
                    INPUT_CLASS +
                    (editingTemplate ? ' opacity-50 cursor-not-allowed' : '')
                  }
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Title i18n Key
                  </label>
                  <input
                    type="text"
                    required
                    value={form.titleI18nKey}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, titleI18nKey: e.target.value }))
                    }
                    placeholder="document-lease-agreement.parties.title"
                    className={INPUT_CLASS}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Body i18n Key
                  </label>
                  <input
                    type="text"
                    required
                    value={form.bodyI18nKey}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, bodyI18nKey: e.target.value }))
                    }
                    placeholder="document-lease-agreement.parties.body"
                    className={INPUT_CLASS}
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Sort Order
                </label>
                <input
                  type="number"
                  required
                  value={form.sortOrder}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      sortOrder: Number(e.target.value),
                    }))
                  }
                  className={INPUT_CLASS + ' max-w-[10rem]'}
                />
              </div>
              <div className="space-y-2">
                <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.optional}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, optional: e.target.checked }))
                    }
                    className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
                  />
                  Optional (landlord can remove this clause)
                </label>
                <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.defaultIncluded}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        defaultIncluded: e.target.checked,
                      }))
                    }
                    className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
                  />
                  Included by default
                </label>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <Button variant="ghost" size="sm" onClick={closeForm}>
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="submit"
                  isLoading={isSaving}
                >
                  {editingTemplate ? 'Save Changes' : 'Add Template'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete Clause Template"
          message={`Are you sure you want to delete the "${deleteTarget.clauseKey}" clause template for ${deleteTarget.countryCode}? This action cannot be undone.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteTemplate.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
};
