import { useState, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Pencil, Trash2, Plus, X, CheckCircle, Layers } from "lucide-react";
import { PageHeader, Button, ConfirmDialog, RefreshButton } from "@buurman/ui";
import { format } from "date-fns";
import { RichTextEditor } from "../components/RichTextEditor";
import { RichTextDisplay } from "../components/RichTextDisplay";
import {
  useRentRegulationCountries,
  useUpdateCountry,
  useDeleteCountry,
  useReviewCountry,
  useRentRegulationRegions,
  useCreateRegion,
  useUpdateRegion,
  useDeleteRegion,
  useRentRegulationRules,
  useCreateRule,
  useBulkCreateRules,
  useUpdateRule,
  useDeleteRule,
} from "../hooks/useRentRegulationHooks";
import { LoadingSpinner } from "../components/LoadingSpinner";
import type {
  RentRegulationCountryResponse,
  RentRegulationRegionResponse,
  RentRegulationRuleResponse,
  CreateRuleRequest,
} from "../api/rentRegulations";

type Tab = "overview" | "regions" | "rules";

const countryCodeToFlag = (code: string): string =>
  code
    .toUpperCase()
    .split("")
    .map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65))
    .join("");

const TAB_CLASS = (active: boolean) =>
  `px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
    active
      ? "border-primary-500 text-primary-600"
      : "border-transparent text-text-secondary hover:text-text-secondary hover:border-border-default"
  }`;

const INPUT_CLASS =
  "w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors";

const TH_CLASS =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary";

const CURRENT_YEAR = new Date().getFullYear();

export const RentRegulationCountryDetailPage = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const countryCode = code ?? "";

  const {
    data: countries,
    isLoading: countriesLoading,
    isFetching,
    refetch,
  } = useRentRegulationCountries();
  const deleteCountry = useDeleteCountry();
  const reviewCountry = useReviewCountry();

  const [activeTab, setActiveTab] = useState<Tab>("overview");
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const country = countries?.find((c) => c.countryCode === countryCode);

  if (countriesLoading) {
    return <LoadingSpinner message="Loading country..." />;
  }

  if (!country) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Country not found.</p>
        <button
          onClick={() => navigate("/rent-regulations")}
          className="mt-4 text-sm text-primary-500 hover:underline"
        >
          Back to rent regulations
        </button>
      </div>
    );
  }

  const handleDelete = () => {
    deleteCountry.mutate(countryCode, {
      onSuccess: () => navigate("/rent-regulations"),
    });
  };

  const handleReview = () => {
    reviewCountry.mutate(countryCode);
  };

  return (
    <div>
      <PageHeader
        title={country.countryName}
        subtitle={country.countryCode}
        avatar={
          <span className="text-2xl">
            {countryCodeToFlag(country.countryCode)}
          </span>
        }
        backTo="/rent-regulations"
        actions={
          <div className="flex items-center gap-2">
            <RefreshButton
              onClick={() => refetch()}
              isRefreshing={isFetching}
            />
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<CheckCircle />}
              onClick={handleReview}
              isLoading={reviewCountry.isPending}
            >
              Mark as Reviewed
            </Button>
            <Button
              variant="danger"
              size="sm"
              onClick={() => setShowDeleteDialog(true)}
            >
              Delete
            </Button>
          </div>
        }
      />

      {/* Tabs */}
      <div className="border-b border-border-default mb-6">
        <nav className="flex gap-0">
          <button
            className={TAB_CLASS(activeTab === "overview")}
            onClick={() => setActiveTab("overview")}
          >
            Overview
          </button>
          {country.hasRegionalRegulations && (
            <button
              className={TAB_CLASS(activeTab === "regions")}
              onClick={() => setActiveTab("regions")}
            >
              Regions
            </button>
          )}
          <button
            className={TAB_CLASS(activeTab === "rules")}
            onClick={() => setActiveTab("rules")}
          >
            Rules
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === "overview" && (
        <OverviewTab country={country} countryCode={countryCode} />
      )}
      {activeTab === "regions" && country.hasRegionalRegulations && (
        <RegionsTab countryCode={countryCode} />
      )}
      {activeTab === "rules" && <RulesTab countryCode={countryCode} />}

      {/* Delete confirmation */}
      {showDeleteDialog && (
        <ConfirmDialog
          title="Delete Country"
          message={`Are you sure you want to delete "${country.countryName}"? All associated regions and rules will also be deleted. This action cannot be undone.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteCountry.isPending}
          onConfirm={handleDelete}
          onCancel={() => setShowDeleteDialog(false)}
        />
      )}
    </div>
  );
};

// ── Overview Tab ─────────────────────────────────────────────────────

function OverviewTab({
  country,
  countryCode,
}: {
  country: RentRegulationCountryResponse;
  countryCode: string;
}) {
  const updateCountry = useUpdateCountry();
  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState(country.countryName);
  const [editSummary, setEditSummary] = useState(country.summary ?? "");
  const [editRegional, setEditRegional] = useState(
    country.hasRegionalRegulations,
  );

  const startEditing = () => {
    setEditName(country.countryName);
    setEditSummary(country.summary ?? "");
    setEditRegional(country.hasRegionalRegulations);
    setIsEditing(true);
  };

  const handleSave = () => {
    updateCountry.mutate(
      {
        code: countryCode,
        data: {
          countryName: editName,
          hasRegionalRegulations: editRegional,
          summary: editSummary || undefined,
        },
      },
      { onSuccess: () => setIsEditing(false) },
    );
  };

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-text-primary">
            Country Details
          </h3>
          {!isEditing && (
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Pencil />}
              onClick={startEditing}
            >
              Edit
            </Button>
          )}
        </div>

        {isEditing ? (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Country Name
              </label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className={INPUT_CLASS + " max-w-md"}
                autoFocus
              />
            </div>
            <div>
              <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                <input
                  type="checkbox"
                  checked={editRegional}
                  onChange={(e) => setEditRegional(e.target.checked)}
                  className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
                />
                Has regional regulations
              </label>
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-1">
                Summary
              </label>
              <RichTextEditor
                value={editSummary}
                onChange={setEditSummary}
                placeholder="Brief overview of rent regulations..."
              />
            </div>
            <div className="flex gap-2">
              <Button
                onClick={handleSave}
                size="sm"
                isLoading={updateCountry.isPending}
              >
                Save
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsEditing(false)}
              >
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            <DetailRow label="Country Code" value={country.countryCode} />
            <DetailRow label="Country Name" value={country.countryName} />
            <DetailRow
              label="Regional Regulations"
              value={country.hasRegionalRegulations ? "Yes" : "No"}
            />
            <DetailRow
              label="Last Reviewed"
              value={
                country.lastReviewedAt
                  ? format(
                      new Date(country.lastReviewedAt),
                      "dd MMM yyyy HH:mm",
                    )
                  : "Never"
              }
            />
            <DetailRow
              label="Status"
              value={
                country.stale ? (
                  <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-warning-bg text-warning-text ring-1 ring-warning-border">
                    May be outdated
                  </span>
                ) : (
                  <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-success-bg text-success-text ring-1 ring-success-border">
                    Verified
                  </span>
                )
              }
            />
            {country.summary && (
              <div className="pt-2">
                <p className="text-xs text-text-secondary mb-1">Summary</p>
                <RichTextDisplay
                  content={country.summary}
                  className="text-sm text-text-secondary"
                />
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-center justify-between py-1.5 border-b border-border-subtle last:border-b-0">
      <span className="text-xs text-text-secondary">{label}</span>
      <span className="text-sm font-medium text-text-primary">{value}</span>
    </div>
  );
}

// ── Regions Tab ──────────────────────────────────────────────────────

function RegionsTab({ countryCode }: { countryCode: string }) {
  const { data: regions, isLoading } = useRentRegulationRegions(countryCode);
  const createRegion = useCreateRegion();
  const updateRegion = useUpdateRegion();
  const deleteRegion = useDeleteRegion();

  const [showForm, setShowForm] = useState(false);
  const [editingRegion, setEditingRegion] =
    useState<RentRegulationRegionResponse | null>(null);
  const [regionCode, setRegionCode] = useState("");
  const [regionName, setRegionName] = useState("");
  const [regionSummary, setRegionSummary] = useState("");
  const [deleteTarget, setDeleteTarget] =
    useState<RentRegulationRegionResponse | null>(null);

  const openCreate = () => {
    setEditingRegion(null);
    setRegionCode("");
    setRegionName("");
    setRegionSummary("");
    setShowForm(true);
  };

  const openEdit = (region: RentRegulationRegionResponse) => {
    setEditingRegion(region);
    setRegionCode(region.regionCode);
    setRegionName(region.regionName);
    setRegionSummary(region.summary ?? "");
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingRegion(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingRegion) {
      updateRegion.mutate(
        {
          countryCode,
          regionCode: editingRegion.regionCode,
          data: {
            regionName,
            summary: regionSummary || undefined,
          },
        },
        { onSuccess: closeForm },
      );
    } else {
      createRegion.mutate(
        {
          countryCode,
          data: {
            regionCode: regionCode.toUpperCase(),
            regionName,
            summary: regionSummary || undefined,
          },
        },
        { onSuccess: closeForm },
      );
    }
  };

  const handleDelete = () => {
    if (!deleteTarget) {
      return;
    }
    deleteRegion.mutate(
      { countryCode, regionCode: deleteTarget.regionCode },
      { onSuccess: () => setDeleteTarget(null) },
    );
  };

  const isSaving = createRegion.isPending || updateRegion.isPending;

  if (isLoading) {
    return <LoadingSpinner message="Loading regions..." />;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-text-primary">Regions</h3>
        <Button
          variant="secondary"
          size="sm"
          leftIcon={<Plus />}
          onClick={openCreate}
        >
          Add Region
        </Button>
      </div>

      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border-default">
              <th className={TH_CLASS}>Code</th>
              <th className={TH_CLASS}>Name</th>
              <th className={TH_CLASS}>Summary</th>
              <th className={`${TH_CLASS} text-right`}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {(regions ?? []).length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-12 text-center text-sm text-text-muted"
                >
                  No regions yet.
                </td>
              </tr>
            ) : (
              (regions ?? []).map((region) => (
                <tr
                  key={region.identifier}
                  className="border-b border-border-default last:border-b-0"
                >
                  <td className="px-4 py-3">
                    <span className="text-sm font-mono text-text-secondary">
                      {region.regionCode}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-sm font-medium text-text-primary">
                      {region.regionName}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    {region.summary ? (
                      <RichTextDisplay
                        content={region.summary}
                        className="text-sm text-text-secondary max-w-xs [&>*]:truncate"
                      />
                    ) : (
                      <span className="text-sm text-text-secondary">--</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => openEdit(region)}
                        className="p-1.5 rounded-md text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 transition-colors"
                        title="Edit"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => setDeleteTarget(region)}
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

      {/* Region Form Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black/40" onClick={closeForm} />
          <div className="relative bg-surface-card rounded-lg border border-border-default shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
              <h2 className="text-lg font-semibold text-text-primary">
                {editingRegion ? "Edit Region" : "Add Region"}
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
                  Region Code
                </label>
                <input
                  type="text"
                  required
                  maxLength={10}
                  value={regionCode}
                  disabled={!!editingRegion}
                  onChange={(e) => setRegionCode(e.target.value.toUpperCase())}
                  placeholder="NH"
                  className={
                    INPUT_CLASS +
                    (editingRegion ? " opacity-50 cursor-not-allowed" : "")
                  }
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Region Name
                </label>
                <input
                  type="text"
                  required
                  value={regionName}
                  onChange={(e) => setRegionName(e.target.value)}
                  placeholder="Noord-Holland"
                  className={INPUT_CLASS}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Summary
                  <span className="text-text-muted font-normal ml-1">
                    (optional)
                  </span>
                </label>
                <RichTextEditor
                  value={regionSummary}
                  onChange={setRegionSummary}
                  placeholder="Brief overview of regional regulations..."
                />
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
                  {editingRegion ? "Save Changes" : "Add Region"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete Region"
          message={`Are you sure you want to delete "${deleteTarget.regionName}"?`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteRegion.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

// ── Rules Tab ────────────────────────────────────────────────────────

interface RuleForm {
  year: number;
  propertyCategory: string;
  sector: string;
  maxIncreasePercentage: string;
  maxIncreaseType: string;
  indexName: string;
  indexValue: string;
  effectiveDate: string;
  noticePeriodDays: string;
  frequency: string;
  additionalConditions: string;
  sourceUrl: string;
  notes: string;
}

const emptyRuleForm: RuleForm = {
  year: CURRENT_YEAR,
  propertyCategory: "RESIDENTIAL",
  sector: "",
  maxIncreasePercentage: "",
  maxIncreaseType: "PERCENTAGE",
  indexName: "",
  indexValue: "",
  effectiveDate: "",
  noticePeriodDays: "",
  frequency: "ANNUAL",
  additionalConditions: "",
  sourceUrl: "",
  notes: "",
};

function ruleFormToRequest(form: RuleForm): CreateRuleRequest {
  return {
    year: form.year,
    propertyCategory: form.propertyCategory,
    sector: form.sector || undefined,
    maxIncreasePercentage: form.maxIncreasePercentage
      ? Number(form.maxIncreasePercentage)
      : undefined,
    maxIncreaseType: form.maxIncreaseType,
    indexName: form.indexName || undefined,
    indexValue: form.indexValue ? Number(form.indexValue) : undefined,
    effectiveDate: form.effectiveDate || undefined,
    noticePeriodDays: form.noticePeriodDays
      ? Number(form.noticePeriodDays)
      : undefined,
    frequency: form.frequency,
    additionalConditions: form.additionalConditions || undefined,
    sourceUrl: form.sourceUrl || undefined,
    notes: form.notes || undefined,
  };
}

function ruleToForm(rule: RentRegulationRuleResponse): RuleForm {
  return {
    year: rule.year,
    propertyCategory: rule.propertyCategory,
    sector: rule.sector ?? "",
    maxIncreasePercentage:
      rule.maxIncreasePercentage != null
        ? String(rule.maxIncreasePercentage)
        : "",
    maxIncreaseType: rule.maxIncreaseType,
    indexName: rule.indexName ?? "",
    indexValue: rule.indexValue != null ? String(rule.indexValue) : "",
    effectiveDate: rule.effectiveDate ?? "",
    noticePeriodDays:
      rule.noticePeriodDays != null ? String(rule.noticePeriodDays) : "",
    frequency: rule.frequency,
    additionalConditions: rule.additionalConditions ?? "",
    sourceUrl: rule.sourceUrl ?? "",
    notes: rule.notes ?? "",
  };
}

interface BulkRow {
  year: number;
  maxIncreasePercentage: string;
  maxIncreaseType: string;
  indexValue: string;
  effectiveDate: string;
  sourceUrl: string;
}

function RulesTab({ countryCode }: { countryCode: string }) {
  const { data: rules, isLoading } = useRentRegulationRules(countryCode);
  const createRule = useCreateRule();
  const bulkCreate = useBulkCreateRules();
  const updateRule = useUpdateRule(countryCode);
  const deleteRule = useDeleteRule(countryCode);

  const [showForm, setShowForm] = useState(false);
  const [editingRule, setEditingRule] =
    useState<RentRegulationRuleResponse | null>(null);
  const [form, setForm] = useState<RuleForm>(emptyRuleForm);
  const [deleteTarget, setDeleteTarget] =
    useState<RentRegulationRuleResponse | null>(null);
  const [bulkMode, setBulkMode] = useState(false);
  const [bulkRows, setBulkRows] = useState<BulkRow[]>(() =>
    Array.from({ length: 7 }, (_, i) => ({
      year: CURRENT_YEAR - 6 + i,
      maxIncreasePercentage: "",
      maxIncreaseType: "PERCENTAGE",
      indexValue: "",
      effectiveDate: "",
      sourceUrl: "",
    })),
  );

  // Group rules by year descending
  const groupedRules = useMemo(() => {
    const all = rules ?? [];
    const groups = new Map<number, RentRegulationRuleResponse[]>();
    for (const rule of all) {
      const existing = groups.get(rule.year) ?? [];
      existing.push(rule);
      groups.set(rule.year, existing);
    }
    return Array.from(groups.entries()).sort(([a], [b]) => b - a);
  }, [rules]);

  const openCreate = () => {
    setEditingRule(null);
    setForm(emptyRuleForm);
    setShowForm(true);
  };

  const openEdit = (rule: RentRegulationRuleResponse) => {
    setEditingRule(rule);
    setForm(ruleToForm(rule));
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingRule(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const request = ruleFormToRequest(form);
    if (editingRule) {
      updateRule.mutate(
        { identifier: editingRule.identifier, data: request },
        { onSuccess: closeForm },
      );
    } else {
      createRule.mutate(
        { countryCode, data: request },
        { onSuccess: closeForm },
      );
    }
  };

  const handleDelete = () => {
    if (!deleteTarget) {
      return;
    }
    deleteRule.mutate(deleteTarget.identifier, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  const handleBulkSubmit = () => {
    const filledRows = bulkRows.filter(
      (r) => r.maxIncreasePercentage || r.indexValue,
    );
    if (filledRows.length === 0) {
      return;
    }
    const bulkRules: CreateRuleRequest[] = filledRows.map((row) => ({
      year: row.year,
      propertyCategory: "RESIDENTIAL",
      maxIncreasePercentage: row.maxIncreasePercentage
        ? Number(row.maxIncreasePercentage)
        : undefined,
      maxIncreaseType: row.maxIncreaseType,
      indexValue: row.indexValue ? Number(row.indexValue) : undefined,
      effectiveDate: row.effectiveDate || undefined,
      sourceUrl: row.sourceUrl || undefined,
      frequency: "ANNUAL",
    }));
    bulkCreate.mutate(
      { countryCode, data: { rules: bulkRules } },
      { onSuccess: () => setBulkMode(false) },
    );
  };

  const updateBulkRow = (
    index: number,
    field: keyof BulkRow,
    value: string | number,
  ) => {
    setBulkRows((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      return next;
    });
  };

  const isSaving = createRule.isPending || updateRule.isPending;

  if (isLoading) {
    return <LoadingSpinner message="Loading rules..." />;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-text-primary">Rules</h3>
        <div className="flex gap-2">
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<Layers />}
            onClick={() => setBulkMode(!bulkMode)}
          >
            {bulkMode ? "Cancel Bulk" : "Bulk Entry"}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<Plus />}
            onClick={openCreate}
          >
            Add Rule
          </Button>
        </div>
      </div>

      {/* Bulk Entry Mode */}
      {bulkMode && (
        <div className="bg-surface-card rounded-lg border border-border-default mb-6">
          <div className="p-4 border-b border-border-default">
            <h4 className="text-sm font-semibold text-text-primary">
              Bulk Rule Entry
            </h4>
            <p className="text-xs text-text-secondary mt-1">
              Fill in the rows for the years you want to add. Empty rows will be
              skipped.
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-default">
                  <th className={TH_CLASS}>Year</th>
                  <th className={TH_CLASS}>Max Increase %</th>
                  <th className={TH_CLASS}>Type</th>
                  <th className={TH_CLASS}>Index Value</th>
                  <th className={TH_CLASS}>Effective Date</th>
                  <th className={TH_CLASS}>Source URL</th>
                </tr>
              </thead>
              <tbody>
                {bulkRows.map((row, idx) => (
                  <tr
                    key={row.year}
                    className={`border-b border-border-default last:border-b-0 ${
                      row.year === CURRENT_YEAR ? "bg-primary-500/5" : ""
                    }`}
                  >
                    <td className="px-4 py-2">
                      <span
                        className={`text-sm font-medium ${
                          row.year === CURRENT_YEAR
                            ? "text-primary-600"
                            : "text-text-primary"
                        }`}
                      >
                        {row.year}
                      </span>
                    </td>
                    <td className="px-4 py-2">
                      <input
                        type="number"
                        step="0.01"
                        value={row.maxIncreasePercentage}
                        onChange={(e) =>
                          updateBulkRow(
                            idx,
                            "maxIncreasePercentage",
                            e.target.value,
                          )
                        }
                        placeholder="e.g. 3.1"
                        className={INPUT_CLASS + " w-28"}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <select
                        value={row.maxIncreaseType}
                        onChange={(e) =>
                          updateBulkRow(idx, "maxIncreaseType", e.target.value)
                        }
                        className={INPUT_CLASS + " w-36"}
                      >
                        <option value="PERCENTAGE">Percentage</option>
                        <option value="INDEX_LINKED">Index-linked</option>
                        <option value="FIXED_AMOUNT">Fixed amount</option>
                        <option value="NEGOTIABLE">Negotiable</option>
                        <option value="NONE">None</option>
                      </select>
                    </td>
                    <td className="px-4 py-2">
                      <input
                        type="number"
                        step="0.01"
                        value={row.indexValue}
                        onChange={(e) =>
                          updateBulkRow(idx, "indexValue", e.target.value)
                        }
                        placeholder="e.g. 104.2"
                        className={INPUT_CLASS + " w-28"}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <input
                        type="date"
                        value={row.effectiveDate}
                        onChange={(e) =>
                          updateBulkRow(idx, "effectiveDate", e.target.value)
                        }
                        className={INPUT_CLASS + " w-36"}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <input
                        type="url"
                        value={row.sourceUrl}
                        onChange={(e) =>
                          updateBulkRow(idx, "sourceUrl", e.target.value)
                        }
                        placeholder="https://..."
                        className={INPUT_CLASS + " w-48"}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="px-4 py-3 border-t border-border-default flex justify-end gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setBulkMode(false)}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleBulkSubmit}
              isLoading={bulkCreate.isPending}
            >
              Import Rules
            </Button>
          </div>
        </div>
      )}

      {/* Rules grouped by year */}
      {groupedRules.length === 0 ? (
        <div className="bg-surface-card rounded-lg border border-border-default px-4 py-12 text-center text-sm text-text-muted">
          No rules yet.
        </div>
      ) : (
        <div className="space-y-4">
          {groupedRules.map(([year, yearRules]) => (
            <div
              key={year}
              className={`bg-surface-card rounded-lg border overflow-hidden ${
                year === CURRENT_YEAR
                  ? "border-primary-500/40"
                  : "border-border-default"
              }`}
            >
              <div
                className={`px-4 py-2.5 border-b ${
                  year === CURRENT_YEAR
                    ? "bg-primary-500/5 border-primary-500/20"
                    : "bg-surface-page border-border-default"
                }`}
              >
                <span
                  className={`text-sm font-semibold ${
                    year === CURRENT_YEAR
                      ? "text-primary-600"
                      : "text-text-primary"
                  }`}
                >
                  {year}
                  {year === CURRENT_YEAR && (
                    <span className="ml-2 text-xs font-normal text-text-secondary">
                      (current year)
                    </span>
                  )}
                </span>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border-default">
                    <th className={TH_CLASS}>Category</th>
                    <th className={TH_CLASS}>Max Increase</th>
                    <th className={TH_CLASS}>Type</th>
                    <th className={TH_CLASS}>Frequency</th>
                    <th className={TH_CLASS}>Effective</th>
                    <th className={`${TH_CLASS} text-right`}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {yearRules.map((rule) => (
                    <tr
                      key={rule.identifier}
                      className="border-b border-border-default last:border-b-0"
                    >
                      <td className="px-4 py-3">
                        <span className="text-sm text-text-primary">
                          {formatLabel(rule.propertyCategory)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-sm text-text-secondary">
                          {rule.maxIncreasePercentage != null
                            ? `${rule.maxIncreasePercentage}%`
                            : "--"}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-sm text-text-secondary">
                          {formatLabel(rule.maxIncreaseType)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-sm text-text-secondary">
                          {formatLabel(rule.frequency)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-sm text-text-secondary">
                          {rule.effectiveDate ?? "--"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => openEdit(rule)}
                            className="p-1.5 rounded-md text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 transition-colors"
                            title="Edit"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => setDeleteTarget(rule)}
                            className="p-1.5 rounded-md text-text-secondary hover:text-error-text hover:bg-error-bg transition-colors"
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}

      {/* Rule Form Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black/40" onClick={closeForm} />
          <div className="relative bg-surface-card rounded-lg border border-border-default shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
              <h2 className="text-lg font-semibold text-text-primary">
                {editingRule ? "Edit Rule" : "Add Rule"}
              </h2>
              <button
                onClick={closeForm}
                className="p-1 rounded-md text-text-secondary hover:text-text-primary hover:bg-surface-inset transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Year
                  </label>
                  <input
                    type="number"
                    required
                    min={2000}
                    max={2100}
                    value={form.year}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, year: Number(e.target.value) }))
                    }
                    className={INPUT_CLASS}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Property Category
                  </label>
                  <select
                    value={form.propertyCategory}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        propertyCategory: e.target.value,
                      }))
                    }
                    className={INPUT_CLASS}
                  >
                    <option value="RESIDENTIAL">Residential</option>
                    <option value="COMMERCIAL">Commercial</option>
                    <option value="SOCIAL_HOUSING">Social Housing</option>
                    <option value="FREE_SECTOR">Free Sector</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Max Increase %
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={form.maxIncreasePercentage}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        maxIncreasePercentage: e.target.value,
                      }))
                    }
                    placeholder="e.g. 3.1"
                    className={INPUT_CLASS}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Max Increase Type
                  </label>
                  <select
                    value={form.maxIncreaseType}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        maxIncreaseType: e.target.value,
                      }))
                    }
                    className={INPUT_CLASS}
                  >
                    <option value="PERCENTAGE">Percentage</option>
                    <option value="INDEX_LINKED">Index-linked</option>
                    <option value="FIXED_AMOUNT">Fixed amount</option>
                    <option value="NEGOTIABLE">Negotiable</option>
                    <option value="NONE">None</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Index Name
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="text"
                    value={form.indexName}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, indexName: e.target.value }))
                    }
                    placeholder="e.g. CPI"
                    className={INPUT_CLASS}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Index Value
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={form.indexValue}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, indexValue: e.target.value }))
                    }
                    className={INPUT_CLASS}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Sector
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="text"
                    value={form.sector}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, sector: e.target.value }))
                    }
                    className={INPUT_CLASS}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Frequency
                  </label>
                  <select
                    value={form.frequency}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, frequency: e.target.value }))
                    }
                    className={INPUT_CLASS}
                  >
                    <option value="ANNUAL">Annual</option>
                    <option value="SEMI_ANNUAL">Semi-annual</option>
                    <option value="QUARTERLY">Quarterly</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="ONE_TIME">One-time</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Effective Date
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="date"
                    value={form.effectiveDate}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        effectiveDate: e.target.value,
                      }))
                    }
                    className={INPUT_CLASS}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Notice Period (days)
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="number"
                    min={0}
                    value={form.noticePeriodDays}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        noticePeriodDays: e.target.value,
                      }))
                    }
                    className={INPUT_CLASS}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Source URL
                  <span className="text-text-muted font-normal ml-1">
                    (optional)
                  </span>
                </label>
                <input
                  type="url"
                  value={form.sourceUrl}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, sourceUrl: e.target.value }))
                  }
                  placeholder="https://..."
                  className={INPUT_CLASS}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Additional Conditions
                  <span className="text-text-muted font-normal ml-1">
                    (optional)
                  </span>
                </label>
                <RichTextEditor
                  value={form.additionalConditions}
                  onChange={(val) =>
                    setForm((f) => ({
                      ...f,
                      additionalConditions: val,
                    }))
                  }
                  placeholder="Any additional conditions or notes..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Notes
                  <span className="text-text-muted font-normal ml-1">
                    (optional)
                  </span>
                </label>
                <textarea
                  value={form.notes}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, notes: e.target.value }))
                  }
                  rows={2}
                  className={INPUT_CLASS + " resize-none"}
                />
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
                  {editingRule ? "Save Changes" : "Add Rule"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete Rule"
          message={`Are you sure you want to delete this ${formatLabel(deleteTarget.propertyCategory)} rule for ${deleteTarget.year}?`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteRule.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

function formatLabel(value: string): string {
  return value
    .replace(/_/g, "")
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}
