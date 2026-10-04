import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, Info, Plus, X } from 'lucide-react';
import { Button } from '@buurman/ui';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { LanguageSwitcher } from '../components/lease/LanguageSwitcher';
import { PreviewClauseList } from '../components/lease/PreviewClauseList';
import type { FocusRequest } from '../components/lease/PreviewClauseList';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { useLeasePreview } from '../hooks/useLeaseClauseTemplateHooks';
import { LEASE_COUNTRIES } from '../lib/leaseCountries';
import { LEASE_KIND_META, LEASE_KIND_ORDER } from '../lib/leaseKindMeta';
import {
  moveButtonFocus,
  moveClause,
  orderedClauses,
  reconcileChoices,
  toChoices,
  toggleClause,
  withChoices,
} from '../lib/leasePreviewClauses';
import { MAX_UI_TENANTS, buildPreviewRequest } from '../lib/leasePreviewForm';
import type { PreviewForm } from '../lib/leasePreviewForm';
import { decodePreviewForm, encodePreviewForm } from '../lib/leasePreviewUrl';
import {
  deriveWarnings,
  previewErrorMessage,
} from '../lib/leasePreviewWarnings';
import type {
  DocumentLanguage,
  LeaseKind,
  LeasePreviewClause,
  LeasePreviewClauseChoice,
} from '../generated/models';

const DEBOUNCE_MS = 400;
const NO_CLAUSES: LeasePreviewClause[] = [];

const INPUT_CLASS =
  'w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors';
const LABEL_CLASS = 'mb-1 block text-sm font-medium text-text-secondary';

const Field = ({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) => (
  <label className="block">
    <span className={LABEL_CLASS}>{label}</span>
    {children}
    {error && (
      <span className="mt-1 block text-xs text-red-700" role="note">
        {error}
      </span>
    )}
  </label>
);

const Section = ({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) => (
  <fieldset className="space-y-3 border-0 p-0">
    <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-text-secondary">
      {title}
    </legend>
    {children}
  </fieldset>
);

const WARNING_STYLE = {
  warning: 'border-warning-border bg-warning-bg text-warning-text',
  info: 'border-border-default bg-surface-inset text-text-secondary',
  error: 'border-red-300 bg-red-50 text-red-800',
} as const;

export const LeaseAgreementPreviewPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  // The local form is the source of truth while editing; the URL follows it (debounced) and is
  // only read on mount and when it changes from elsewhere (e.g. a link from the templates page).
  const [form, setForm] = useState(() => decodePreviewForm(searchParams));
  const [choices, setChoices] = useState<LeasePreviewClauseChoice[] | null>(
    null
  );
  const writtenQuery = useRef(searchParams.toString());
  useEffect(() => {
    const query = searchParams.toString();
    if (query !== writtenQuery.current) {
      writtenQuery.current = query;
      setForm(decodePreviewForm(searchParams));
      setChoices(null);
    }
  }, [searchParams]);
  const debouncedForm = useDebouncedValue(form, DEBOUNCE_MS);
  useEffect(() => {
    const query = encodePreviewForm(debouncedForm).toString();
    if (query !== writtenQuery.current) {
      writtenQuery.current = query;
      setSearchParams(new URLSearchParams(query), { replace: true });
    }
  }, [debouncedForm, setSearchParams]);

  const [focusRequest, setFocusRequest] = useState<FocusRequest | null>(null);

  const update = (patch: Partial<PreviewForm>) => {
    const next = { ...form, ...patch };
    if (next.country !== form.country || next.kind !== form.kind) {
      setChoices(null);
    }
    setForm(next);
  };
  const setTenant = (index: number, value: string) =>
    update({
      tenantNames: form.tenantNames.map((t, i) => (i === index ? value : t)),
    });

  // Last successful clause list for the current country + kind, kept across errors.
  const [base, setBase] = useState<{
    scope: string;
    clauses: LeasePreviewClause[];
  }>({ scope: '', clauses: [] });
  const scope = (f: PreviewForm) => `${f.country}|${f.kind}`;
  const baseClauses = base.scope === scope(form) ? base.clauses : NO_CLAUSES;
  const effectiveChoices = useMemo(
    () => reconcileChoices(choices, baseClauses),
    [choices, baseClauses]
  );

  const live = useMemo(
    () => ({ form, choices: effectiveChoices }),
    [form, effectiveChoices]
  );
  const settled = useDebouncedValue(live, DEBOUNCE_MS);
  const built = useMemo(
    () => buildPreviewRequest(settled.form, settled.choices),
    [settled]
  );
  const liveBuild = useMemo(
    () => buildPreviewRequest(form, effectiveChoices),
    [form, effectiveChoices]
  );
  const fieldErrors = liveBuild.ok ? {} : liveBuild.fieldErrors;
  const preview = useLeasePreview(built.ok ? built.request : null);

  const data = preview.data;
  const stale = preview.isPlaceholderData;
  const settledScope = scope(settled.form);
  if (
    data &&
    !stale &&
    !preview.isError &&
    (base.clauses !== data.clauses || base.scope !== settledScope)
  ) {
    setBase({ scope: settledScope, clauses: data.clauses });
  }

  const isUpdating = settled !== live || preview.isFetching;
  const clauses = useMemo(
    () => orderedClauses(withChoices(baseClauses, effectiveChoices)),
    [baseClauses, effectiveChoices]
  );
  const warnings =
    data && !preview.isError && !stale
      ? deriveWarnings(
          {
            country: settled.form.country,
            language: settled.form.language,
            kind: settled.form.kind,
          },
          data
        )
      : [];
  const showHtml =
    data?.html && data.availability.startsWith('AVAILABLE') && !preview.isError;
  const status = (preview.error as { response?: { status?: number } } | null)
    ?.response?.status;
  const retryable = preview.isError && (status === undefined || status >= 500);

  const changeClauses = (next: LeasePreviewClause[]) => {
    setChoices(toChoices(next));
  };
  const moveAndFocus = (key: string, direction: -1 | 1) => {
    const moved = moveClause(clauses, key, direction);
    if (moved === clauses) {
      return;
    }
    changeClauses(moved);
    setFocusRequest((prev) => ({
      clauseKey: key,
      button: moveButtonFocus(orderedClauses(moved), key, direction),
      nonce: (prev?.nonce ?? 0) + 1,
    }));
  };

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Link
          to="/lease-clause-templates"
          className="inline-flex items-center gap-1 text-sm text-text-secondary hover:text-primary-600"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Back to templates
        </Link>
        <h1 className="text-2xl font-bold text-text-primary">
          Preview agreement
        </h1>
        <span className="rounded-md border border-red-400 px-2 py-0.5 text-xs font-semibold text-red-700">
          SAMPLE: preview with sample data
        </span>
      </div>

      <div
        role="alert"
        className="mb-4 flex items-start gap-3 rounded-lg border border-warning-border bg-warning-bg px-4 py-3"
      >
        <AlertTriangle className="mt-0.5 h-5 w-5 flex-shrink-0 text-warning-text" />
        <p className="text-sm text-warning-text">
          NL residential clauses are draft legal text pending counsel review;
          other countries use placeholder text. Not vetted legal content.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[340px_1fr]">
        <form
          aria-label="Preview options"
          onSubmit={(e) => e.preventDefault()}
          className="space-y-6 lg:max-h-[calc(100vh-14rem)] lg:overflow-y-auto lg:pr-2"
        >
          <Section title="Agreement">
            <Field label="Country">
              <select
                className={INPUT_CLASS}
                value={form.country}
                onChange={(e) => update({ country: e.target.value })}
              >
                {LEASE_COUNTRIES.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.code} {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Kind">
              <select
                className={INPUT_CLASS}
                value={form.kind}
                onChange={(e) => update({ kind: e.target.value as LeaseKind })}
              >
                {LEASE_KIND_ORDER.map((kind) => (
                  <option key={kind} value={kind}>
                    {LEASE_KIND_META[kind].label}
                  </option>
                ))}
              </select>
            </Field>
            <LanguageSwitcher
              value={form.language}
              onChange={(language: DocumentLanguage) => update({ language })}
              className={INPUT_CLASS}
            />
          </Section>

          <Section title="Contract">
            <Field label="Contract type">
              <select
                className={INPUT_CLASS}
                value={form.contractType}
                onChange={(e) =>
                  update({
                    contractType: e.target.value as PreviewForm['contractType'],
                  })
                }
              >
                <option value="FIXED_TERM">Fixed term</option>
                <option value="INDEFINITE">Indefinite</option>
              </select>
            </Field>
            <Field label="Start date" error={fieldErrors.startDate}>
              <input
                type="date"
                className={INPUT_CLASS}
                value={form.startDate}
                onChange={(e) => update({ startDate: e.target.value })}
              />
            </Field>
            {form.contractType === 'FIXED_TERM' && (
              <Field label="End date" error={fieldErrors.endDate}>
                <input
                  type="date"
                  className={INPUT_CLASS}
                  value={form.endDate}
                  onChange={(e) => update({ endDate: e.target.value })}
                />
              </Field>
            )}
            <div className="grid grid-cols-[1fr_5rem] gap-2">
              <Field label="Base rent (monthly)" error={fieldErrors.rentAmount}>
                <input
                  inputMode="decimal"
                  className={INPUT_CLASS}
                  value={form.rentAmount}
                  onChange={(e) => update({ rentAmount: e.target.value })}
                />
              </Field>
              <Field label="Currency" error={fieldErrors.currency}>
                <input
                  maxLength={3}
                  className={INPUT_CLASS}
                  value={form.currency}
                  onChange={(e) =>
                    update({ currency: e.target.value.toUpperCase() })
                  }
                />
              </Field>
            </div>
            <Field
              label="Utilities advance (optional)"
              error={fieldErrors.utilitiesAmount}
            >
              <input
                inputMode="decimal"
                className={INPUT_CLASS}
                value={form.utilitiesAmount}
                onChange={(e) => update({ utilitiesAmount: e.target.value })}
              />
            </Field>
            <Field label="Deposit (optional)" error={fieldErrors.depositAmount}>
              <input
                inputMode="decimal"
                className={INPUT_CLASS}
                value={form.depositAmount}
                onChange={(e) => update({ depositAmount: e.target.value })}
              />
            </Field>
            <Field
              label="Payment day of month"
              error={fieldErrors.paymentDueDay}
            >
              <input
                inputMode="numeric"
                className={INPUT_CLASS}
                value={form.paymentDueDay}
                onChange={(e) => update({ paymentDueDay: e.target.value })}
              />
            </Field>
          </Section>

          <Section title="Parties and property">
            <Field label="Landlord name" error={fieldErrors.landlordName}>
              <input
                className={INPUT_CLASS}
                value={form.landlordName}
                onChange={(e) => update({ landlordName: e.target.value })}
              />
            </Field>
            {form.tenantNames.map((name, index) => (
              <div key={index} className="flex items-end gap-2">
                <div className="flex-1">
                  <Field
                    label={`Tenant ${index + 1}`}
                    error={
                      name.trim() === '' ? fieldErrors.tenantNames : undefined
                    }
                  >
                    <input
                      className={INPUT_CLASS}
                      value={name}
                      onChange={(e) => setTenant(index, e.target.value)}
                    />
                  </Field>
                </div>
                {form.tenantNames.length > 1 && (
                  <button
                    type="button"
                    aria-label={`Remove tenant ${index + 1}`}
                    onClick={() =>
                      update({
                        tenantNames: form.tenantNames.filter(
                          (_, i) => i !== index
                        ),
                      })
                    }
                    className="mb-1 rounded p-2 text-text-secondary hover:bg-surface-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/40"
                  >
                    <X className="h-4 w-4" aria-hidden="true" />
                  </button>
                )}
              </div>
            ))}
            {form.tenantNames.length < MAX_UI_TENANTS && (
              <Button
                type="button"
                variant="secondary"
                size="sm"
                leftIcon={<Plus />}
                onClick={() =>
                  update({
                    tenantNames: [
                      ...form.tenantNames,
                      `Sample Tenant ${form.tenantNames.length + 1}`,
                    ],
                  })
                }
              >
                Add tenant
              </Button>
            )}
            <Field label="Property address" error={fieldErrors.propertyAddress}>
              <input
                className={INPUT_CLASS}
                value={form.propertyAddress}
                onChange={(e) => update({ propertyAddress: e.target.value })}
              />
            </Field>
          </Section>

          <Section title="Clauses">
            {clauses.length === 0 ? (
              <p className="text-sm text-text-muted">
                No clauses for this selection.
              </p>
            ) : (
              <PreviewClauseList
                clauses={clauses}
                disabled={stale}
                focusRequest={focusRequest}
                onToggle={(key) => changeClauses(toggleClause(clauses, key))}
                onMove={moveAndFocus}
              />
            )}
            {choices !== null && (
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => setChoices(null)}
              >
                Reset to defaults
              </Button>
            )}
          </Section>
        </form>

        <section aria-label="Agreement preview" className="min-w-0">
          <div className="mb-3 space-y-2">
            <div role="status" aria-live="polite" className="space-y-2">
              <span className="sr-only">{isUpdating ? 'Updating…' : ''}</span>
              {liveBuild.ok === false && liveBuild.errors.length > 0 && (
                <p className="rounded-lg border border-warning-border bg-warning-bg px-3 py-2 text-sm text-warning-text">
                  Fix the highlighted options to update the preview.
                </p>
              )}
              {warnings.map((w) => (
                <p
                  key={w.id}
                  className={`flex items-start gap-2 rounded-lg border px-3 py-2 text-sm ${WARNING_STYLE[w.severity]}`}
                >
                  <Info
                    className="mt-0.5 h-4 w-4 flex-shrink-0"
                    aria-hidden="true"
                  />
                  {w.message}
                </p>
              ))}
            </div>
            {preview.isError && (
              <div
                role="alert"
                className={`flex items-start justify-between gap-3 rounded-lg border px-3 py-2 text-sm ${WARNING_STYLE.error}`}
              >
                <span>{previewErrorMessage(preview.error)}</span>
                {retryable && (
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => preview.refetch()}
                  >
                    Retry
                  </Button>
                )}
              </div>
            )}
          </div>

          <div
            aria-busy={isUpdating}
            className="relative rounded-lg border border-border-default bg-surface-inset"
          >
            {isUpdating && (
              <div
                aria-hidden="true"
                className="absolute right-3 top-3 z-10 flex items-center gap-2 rounded-md bg-surface-card px-2 py-1 text-xs text-text-secondary shadow"
              >
                <LoadingSpinner />
                Updating…
              </div>
            )}
            {showHtml ? (
              <iframe
                title="Lease agreement preview (sample data)"
                sandbox=""
                referrerPolicy="no-referrer"
                srcDoc={data?.html ?? ''}
                className={`h-[calc(100vh-18rem)] min-h-[600px] w-full rounded-lg bg-white transition-opacity ${isUpdating ? 'opacity-60' : ''}`}
              />
            ) : (
              <div className="flex min-h-[300px] items-center justify-center p-6 text-sm text-text-muted">
                {data || preview.isError
                  ? 'No document to show.'
                  : 'Loading preview…'}
              </div>
            )}
          </div>
          {data?.kindUsed && data.languageUsed && (
            <p className="mt-2 text-xs text-text-muted">
              Rendered: {LEASE_KIND_META[data.kindUsed].label},{' '}
              {data.languageUsed.toUpperCase()},{' '}
              {clauses.filter((c) => c.included).length} clauses
            </p>
          )}
        </section>
      </div>
    </div>
  );
};
