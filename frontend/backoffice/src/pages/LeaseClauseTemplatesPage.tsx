import { Fragment, useMemo, useState } from 'react';
import {
  Eye,
  Languages,
  Lock,
  Pencil,
  Pin,
  Plus,
  Trash2,
  X,
} from 'lucide-react';
import { Button, ConfirmDialog, StatusBadge } from '@buurman/ui';
import {
  useLeaseClauseTemplates,
  useCreateLeaseClauseTemplate,
  useUpdateLeaseClauseTemplate,
  useDeleteLeaseClauseTemplate,
} from '../hooks/useLeaseClauseTemplateHooks';
import { Link } from 'react-router-dom';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { DocumentLanguage, LeaseKind } from '../generated/models';
import type { LeaseClauseTemplateResponse } from '../generated/models';
import { LanguageSwitcher } from '../components/lease/LanguageSwitcher';
import { LegalDisclaimer } from '../components/lease/LegalDisclaimer';
import { LeaseKindBadge } from '../components/lease/LeaseKindBadge';
import { LeaseKindLegend } from '../components/lease/LeaseKindLegend';
import { Popover } from '../components/lease/Popover';
import { ResolvedTextCell } from '../components/lease/ResolvedTextCell';
import {
  DEFAULT_FILTERS,
  LANGUAGE_LABELS,
  applyFilters,
  behaviourFlags,
  groupTemplates,
  hasProblem,
  isEnglishFallback,
} from '../lib/leaseClauseTable';
import type {
  BehaviourFilter,
  BehaviourFlag,
  TableFilters,
} from '../lib/leaseClauseTable';
import { LEASE_COUNTRIES } from '../lib/leaseCountries';
import { previewLink } from '../lib/leasePreviewUrl';
import {
  LEASE_KIND_META,
  LEASE_KIND_ORDER,
  fallbackText,
} from '../lib/leaseKindMeta';

const COUNTRIES = LEASE_COUNTRIES;

const TH_CLASS =
  'text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary';

const INPUT_CLASS =
  'w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';

interface TemplateForm {
  // Empty only on a fresh create form: the kind must be chosen explicitly (no default).
  leaseKind: LeaseKind | '';
  clauseKey: string;
  titleI18nKey: string;
  bodyI18nKey: string;
  sortOrder: number;
  optional: boolean;
  defaultIncluded: boolean;
  pinned: boolean;
}

const emptyForm = (
  countryCode: string
): TemplateForm & { countryCode: string } => ({
  countryCode,
  leaseKind: '',
  clauseKey: '',
  titleI18nKey: '',
  bodyI18nKey: '',
  sortOrder: 0,
  optional: true,
  defaultIncluded: true,
  pinned: false,
});

/**
 * Backoffice management of the lease clause template library (BUUR-105). Residential and
 * commercial clause sets exist for every catalog country as drafts, not vetted by counsel;
 * other kinds use placeholder text — see
 * the banner below, which is load-bearing per the feature's own design doc and must stay visible
 * and non-dismissible on this page.
 */
export const LeaseClauseTemplatesPage = () => {
  const [language, setLanguage] = useState<DocumentLanguage>(
    DocumentLanguage.en
  );
  const [filters, setFilters] = useState<TableFilters>(DEFAULT_FILTERS);
  const countryCode =
    filters.country === 'all' ? COUNTRIES[0].code : filters.country;
  const loadedCountries = useMemo(
    () =>
      filters.country === 'all'
        ? COUNTRIES.map((c) => c.code)
        : [filters.country],
    [filters.country]
  );
  const {
    data: templates,
    isLoading,
    failedCountries,
    isRefreshing,
    refetch,
  } = useLeaseClauseTemplates(loadedCountries, language);
  const createTemplate = useCreateLeaseClauseTemplate();
  const updateTemplate = useUpdateLeaseClauseTemplate();
  const deleteTemplate = useDeleteLeaseClauseTemplate();

  const [showForm, setShowForm] = useState(false);
  const [editingTemplate, setEditingTemplate] =
    useState<LeaseClauseTemplateResponse | null>(null);
  const [form, setForm] = useState(emptyForm(countryCode));
  const [deleteTarget, setDeleteTarget] =
    useState<LeaseClauseTemplateResponse | null>(null);

  const openCreate = (country?: string, kind?: LeaseKind) => {
    setEditingTemplate(null);
    setForm({
      ...emptyForm(country ?? countryCode),
      leaseKind: kind ?? '',
    });
    setShowForm(true);
  };

  const openEdit = (template: LeaseClauseTemplateResponse) => {
    setEditingTemplate(template);
    setForm({
      countryCode: template.countryCode,
      leaseKind: template.leaseKind,
      clauseKey: template.clauseKey,
      titleI18nKey: template.titleI18nKey,
      bodyI18nKey: template.bodyI18nKey,
      sortOrder: template.sortOrder,
      optional: template.optional,
      defaultIncluded: template.defaultIncluded,
      pinned: template.pinned,
    });
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingTemplate(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.leaseKind) {
      return;
    }
    const data = {
      countryCode: form.countryCode,
      leaseKind: form.leaseKind,
      clauseKey: form.clauseKey,
      titleI18nKey: form.titleI18nKey,
      bodyI18nKey: form.bodyI18nKey,
      sortOrder: form.sortOrder,
      optional: form.optional,
      defaultIncluded: form.defaultIncluded,
      pinned: form.pinned,
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
  const visible = useMemo(
    () => applyFilters(templates, filters, language),
    [templates, filters, language]
  );
  const groups = useMemo(() => groupTemplates(visible), [visible]);
  const problemCount = useMemo(
    () => templates.filter((t) => hasProblem(t, language)).length,
    [templates, language]
  );
  const setFilter = <K extends keyof TableFilters>(
    key: K,
    value: TableFilters[K]
  ) => setFilters((f) => ({ ...f, [key]: value }));
  const filtersActive =
    JSON.stringify(filters) !== JSON.stringify(DEFAULT_FILTERS);

  return (
    <div>
      {/* Legal content disclaimer — persistent and non-dismissible per the feature's design doc:
          "do not ship this page without it". No close button, no auto-hide. */}
      <LegalDisclaimer className="mb-6" />

      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">
            Lease Clause Templates
          </h1>
          <p className="text-sm text-text-secondary mt-1 max-w-3xl">
            Title and summary come from the message bundles (
            <code className="font-mono text-xs">
              document-lease-agreement*.properties
            </code>
            ). The summary is the short text shown next to the clause in the
            contract&apos;s clause list; the full legal wording lives in the
            country documents, edited in code. This page controls which clauses
            exist, their order and their rules.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to={previewLink(
              filters.country === 'all' ? undefined : filters.country,
              filters.kind === 'all' ? undefined : filters.kind,
              language
            )}
            className="inline-flex items-center gap-1.5 rounded-lg border border-border-default px-3 py-1.5 text-sm font-medium text-text-primary hover:bg-surface-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
          >
            <Eye className="h-4 w-4" aria-hidden="true" />
            Preview agreement
          </Link>
          <Button
            variant="primary"
            size="sm"
            leftIcon={<Plus />}
            onClick={() => openCreate()}
          >
            Add Clause Template
          </Button>
        </div>
      </div>

      <div
        role="search"
        aria-label="Clause filters"
        className="mb-3 flex flex-wrap items-end gap-4"
      >
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-text-secondary">
            Country
          </span>
          <select
            value={filters.country}
            onChange={(e) => setFilter('country', e.target.value)}
            className={INPUT_CLASS}
          >
            <option value="all">All countries</option>
            {COUNTRIES.map((c) => (
              <option key={c.code} value={c.code}>
                {c.name} ({c.code})
              </option>
            ))}
          </select>
        </label>
        <div>
          <span className="mb-1 flex items-center gap-1 text-sm font-medium text-text-secondary">
            <label htmlFor="kind-filter">Kind</label>
            <LeaseKindLegend />
          </span>
          <select
            id="kind-filter"
            value={filters.kind}
            onChange={(e) =>
              setFilter('kind', e.target.value as TableFilters['kind'])
            }
            className={INPUT_CLASS}
          >
            <option value="all">All kinds</option>
            {LEASE_KIND_ORDER.map((kind) => (
              <option key={kind} value={kind}>
                {LEASE_KIND_META[kind].label}
              </option>
            ))}
          </select>
        </div>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-text-secondary">
            Behaviour
          </span>
          <select
            value={filters.behaviour}
            onChange={(e) =>
              setFilter('behaviour', e.target.value as BehaviourFilter)
            }
            className={INPUT_CLASS}
          >
            <option value="all">Any</option>
            <option value="optional">Optional</option>
            <option value="required">Required</option>
            <option value="pinned">Pinned</option>
          </select>
        </label>
        <label className="flex items-center gap-2 pb-2 text-sm text-text-secondary cursor-pointer">
          <input
            type="checkbox"
            checked={filters.problemsOnly}
            onChange={(e) => setFilter('problemsOnly', e.target.checked)}
            className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
          />
          Problems only
          <span className="text-xs text-text-muted">({problemCount})</span>
        </label>
        <div className="ml-auto">
          <LanguageSwitcher
            value={language}
            onChange={setLanguage}
            className={INPUT_CLASS}
          />
        </div>
      </div>
      <p aria-live="polite" className="mb-4 text-xs text-text-muted">
        {visible.length} of {templates.length} clauses
        {isRefreshing && ' · Updating…'}
        {filtersActive && (
          <>
            {' · '}
            <button
              type="button"
              onClick={() => setFilters(DEFAULT_FILTERS)}
              className="text-primary-600 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40 rounded"
            >
              Reset filters
            </button>
          </>
        )}
      </p>

      {failedCountries.length > 0 && (
        <div
          role="alert"
          className="mb-4 rounded-lg border border-error-border bg-error-bg px-4 py-3 text-sm text-error-text"
        >
          Couldn&apos;t load clause templates for {failedCountries.join(', ')}
          {templates.length > 0 ? '; showing what loaded.' : '.'}{' '}
          <button
            type="button"
            onClick={() => refetch()}
            className="font-medium underline"
          >
            Retry
          </button>
        </div>
      )}

      {isLoading ? (
        <LoadingSpinner message="Loading clause templates..." />
      ) : groups.length === 0 &&
        failedCountries.length > 0 ? null : groups.length === 0 ? (
        <div className="rounded-lg border border-border-default bg-surface-card px-4 py-12 text-center text-sm text-text-muted">
          {filtersActive
            ? 'No clauses match these filters.'
            : 'No clause templates yet.'}
        </div>
      ) : (
        <div
          aria-busy={isRefreshing}
          className={`bg-surface-card rounded-lg border border-border-default overflow-hidden transition-opacity ${
            isRefreshing ? 'opacity-60' : ''
          }`}
        >
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-default">
                  <th scope="col" className={`${TH_CLASS} w-12`}>
                    #
                  </th>
                  <th scope="col" className={`${TH_CLASS} w-[16%]`}>
                    Clause
                  </th>
                  <th scope="col" className={`${TH_CLASS} w-[24%]`}>
                    Title
                  </th>
                  <th scope="col" className={TH_CLASS}>
                    Summary (shown in clause list)
                  </th>
                  <th scope="col" className={`${TH_CLASS} w-40`}>
                    Behaviour
                  </th>
                  <th scope="col" className={`${TH_CLASS} w-28 text-right`}>
                    Actions
                  </th>
                </tr>
              </thead>
              {groups.map((country) => (
                <tbody key={country.countryCode}>
                  <tr className="bg-surface-inset">
                    <th
                      scope="colgroup"
                      colSpan={6}
                      className="px-4 py-2 text-left text-sm font-semibold text-text-primary"
                    >
                      {COUNTRIES.find((c) => c.code === country.countryCode)
                        ?.name ?? country.countryCode}{' '}
                      <span className="ml-1 rounded-full bg-surface-card px-2 py-0.5 text-xs font-normal text-text-secondary">
                        {country.count}
                      </span>
                    </th>
                  </tr>
                  {country.kinds.map((group) => (
                    <Fragment key={group.kind}>
                      <tr className="border-t border-border-default">
                        <th
                          scope="colgroup"
                          colSpan={6}
                          className="pl-8 pr-4 py-2 text-left"
                        >
                          <span className="flex items-center gap-2 text-xs font-normal text-text-muted">
                            <LeaseKindBadge kind={group.kind} />
                            <LeaseKindLegend current={group.kind} />
                            <span>
                              falls back to {fallbackText(group.kind)} ·{' '}
                              {group.rows.length} clause
                              {group.rows.length === 1 ? '' : 's'}
                            </span>
                            <Link
                              to={previewLink(
                                country.countryCode,
                                group.kind,
                                language
                              )}
                              aria-label={`Preview ${country.countryCode} ${LEASE_KIND_META[group.kind].label} agreement`}
                              className="ml-auto rounded-md p-1 text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
                            >
                              <Eye className="h-4 w-4" aria-hidden="true" />
                            </Link>
                            <button
                              type="button"
                              onClick={() =>
                                openCreate(country.countryCode, group.kind)
                              }
                              aria-label={`Add clause to ${country.countryCode} ${LEASE_KIND_META[group.kind].label}`}
                              className="rounded-md p-1 text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
                            >
                              <Plus className="h-4 w-4" aria-hidden="true" />
                            </button>
                          </span>
                        </th>
                      </tr>
                      {group.rows.map((template) => (
                        <ClauseRow
                          key={template.identifier}
                          template={template}
                          language={language}
                          onLanguage={setLanguage}
                          onEdit={() => openEdit(template)}
                          onDelete={() => setDeleteTarget(template)}
                        />
                      ))}
                    </Fragment>
                  ))}
                </tbody>
              ))}
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
                {editingTemplate
                  ? 'Edit Clause Template'
                  : 'Add Clause Template'}
              </h2>
              <button
                onClick={closeForm}
                aria-label="Close"
                className="p-1 rounded-md text-text-secondary hover:text-text-primary hover:bg-surface-inset transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Country
                </label>
                <select
                  required
                  disabled={!!editingTemplate}
                  value={form.countryCode}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, countryCode: e.target.value }))
                  }
                  className={
                    INPUT_CLASS +
                    (editingTemplate ? ' opacity-50 cursor-not-allowed' : '')
                  }
                >
                  {COUNTRIES.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.name} ({c.code})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label
                  htmlFor="lease-kind-select"
                  className="block text-sm font-medium text-text-secondary mb-1"
                >
                  Lease Kind
                </label>
                <select
                  id="lease-kind-select"
                  aria-describedby="lease-kind-help"
                  required
                  disabled={!!editingTemplate}
                  value={form.leaseKind}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      leaseKind: e.target.value as LeaseKind,
                    }))
                  }
                  className={
                    INPUT_CLASS +
                    (editingTemplate ? ' opacity-50 cursor-not-allowed' : '')
                  }
                >
                  <option value="" disabled>
                    Select a lease kind…
                  </option>
                  {LEASE_KIND_ORDER.map((kind) => (
                    <option key={kind} value={kind}>
                      {LEASE_KIND_META[kind].label} ({kind})
                    </option>
                  ))}
                </select>
                <p
                  id="lease-kind-help"
                  aria-live="polite"
                  className="mt-1 text-xs text-text-muted"
                >
                  {form.leaseKind ? (
                    <>
                      {LEASE_KIND_META[form.leaseKind].oneLine}{' '}
                      <span className="font-medium">Applies when:</span>{' '}
                      {LEASE_KIND_META[form.leaseKind].appliesWhen} Falls back
                      to: {fallbackText(form.leaseKind)}.
                    </>
                  ) : (
                    'Pick the kind of contract this clause belongs to.'
                  )}
                </p>
              </div>
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
                <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.pinned}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, pinned: e.target.checked }))
                    }
                    className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
                  />
                  Pinned (fixed position in the lease, not reorderable)
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

const FLAG_META: Record<
  BehaviourFlag,
  { label: string; icon?: React.ReactNode; color: 'gray' | 'amber' | 'blue' }
> = {
  required: {
    label: 'Required',
    icon: <Lock className="h-3 w-3" aria-hidden="true" />,
    color: 'gray',
  },
  pinned: {
    label: 'Pinned',
    icon: <Pin className="h-3 w-3" aria-hidden="true" />,
    color: 'blue',
  },
  defaultOff: { label: 'Default off', color: 'amber' },
};

interface ClauseRowProps {
  template: LeaseClauseTemplateResponse;
  language: DocumentLanguage;
  onLanguage: (language: DocumentLanguage) => void;
  onEdit: () => void;
  onDelete: () => void;
}

const ClauseRow = ({
  template,
  language,
  onLanguage,
  onEdit,
  onDelete,
}: ClauseRowProps) => {
  const fallback = isEnglishFallback(template, language);
  const name = `${template.clauseKey}, ${template.countryCode} ${LEASE_KIND_META[template.leaseKind].label}`;
  return (
    <tr className="border-t border-border-default align-top">
      <td className="px-4 py-3 text-sm tabular-nums text-text-muted">
        {template.sortOrder}
      </td>
      <td className="px-4 py-3">
        <p className="text-sm font-medium text-text-primary">
          {template.clauseKey}
        </p>
        <p className="text-xs text-text-muted">v{template.version}</p>
      </td>
      <td className="px-4 py-3">
        <div className="flex items-start gap-1">
          <ResolvedTextCell
            text={template.titleText}
            i18nKey={template.titleI18nKey}
            englishFallback={fallback}
          />
          {template.missingLanguages.length > 0 && (
            <Popover
              label={`Translation status for ${name}`}
              trigger={<Languages className="h-4 w-4" aria-hidden="true" />}
              widthClassName="w-72"
            >
              {(close) => (
                <>
                  <h3 className="text-sm font-semibold text-text-primary">
                    Missing translations
                  </h3>
                  <p className="mt-1 text-xs text-text-muted">
                    These languages do not define the title or summary
                    themselves and show the English text. Select one to view it.
                  </p>
                  <ul className="mt-2 flex flex-wrap gap-1.5">
                    {template.missingLanguages.map((lang) => (
                      <li key={lang}>
                        <button
                          type="button"
                          onClick={() => {
                            onLanguage(lang);
                            close();
                          }}
                          className="rounded bg-warning-bg px-2 py-0.5 text-xs text-warning-text focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
                        >
                          {lang.toUpperCase()} {LANGUAGE_LABELS[lang]}
                        </button>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </Popover>
          )}
        </div>
      </td>
      <td className="px-4 py-3">
        <ResolvedTextCell
          text={template.bodyText}
          i18nKey={template.bodyI18nKey}
          englishFallback={fallback}
          clamp
        />
      </td>
      <td className="px-4 py-3">
        <div className="flex flex-wrap gap-1">
          {behaviourFlags(template).map((flag) => (
            <StatusBadge
              key={flag}
              label={FLAG_META[flag].label}
              icon={FLAG_META[flag].icon}
              color={FLAG_META[flag].color}
              size="xs"
            />
          ))}
        </div>
      </td>
      <td className="px-4 py-3 text-right">
        <div className="flex items-center justify-end gap-1">
          <button
            type="button"
            onClick={onEdit}
            aria-label={`Edit ${name}`}
            title="Edit"
            className="p-1.5 rounded-md text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
          >
            <Pencil className="h-4 w-4" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={onDelete}
            aria-label={`Delete ${name}`}
            title="Delete"
            className="p-1.5 rounded-md text-text-secondary hover:text-error-text hover:bg-error-bg transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
          >
            <Trash2 className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      </td>
    </tr>
  );
};
