import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
  Plus,
  Search,
  X,
  ChevronDown,
  ChevronRight,
  Trash2,
  Users,
  MessageSquareText,
  Clock,
  Globe,
} from "lucide-react";
import { RefreshButton, Button, ConfirmDialog } from "@buurman/ui";
import { RichTextEditor } from "../components/RichTextEditor";
import { format, formatDistanceToNow } from "date-fns";
import {
  useRentRegulationCountries,
  useCreateCountry,
  useCountryRegulationRequests,
  useDismissCountryRequest,
} from "../hooks/useRentRegulationHooks";
import { LoadingSpinner } from "../components/LoadingSpinner";
import type {
  RentRegulationCountryResponse,
  CountryRegulationRequestSummary,
  CountryRegulationRequester,
} from "../api/rentRegulations";

function groupByTeam(
  requesters: CountryRegulationRequester[],
): [string, CountryRegulationRequester[]][] {
  const map = new Map<string, CountryRegulationRequester[]>();
  for (const r of requesters) {
    const existing = map.get(r.teamIdentifier);
    if (existing) {
      existing.push(r);
    } else {
      map.set(r.teamIdentifier, [r]);
    }
  }
  return Array.from(map.entries());
}

const TH_CLASS =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary";

const countryCodeToFlag = (code: string): string =>
  code
    .toUpperCase()
    .split("")
    .map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65))
    .join("");

interface CountryForm {
  countryCode: string;
  countryName: string;
  hasRegionalRegulations: boolean;
  summary: string;
}

const emptyForm: CountryForm = {
  countryCode: "",
  countryName: "",
  hasRegionalRegulations: false,
  summary: "",
};

type Tab = "countries" | "requests";

export const RentRegulationsPage = () => {
  const [tab, setTab] = useState<Tab>("countries");

  return (
    <div>
      {/* Tab bar */}
      <div className="flex gap-1 mb-6 border-b border-border-default">
        <TabButton
          active={tab === "countries"}
          onClick={() => setTab("countries")}
        >
          Countries
        </TabButton>
        <TabButton
          active={tab === "requests"}
          onClick={() => setTab("requests")}
        >
          Country Requests
        </TabButton>
      </div>

      {tab === "countries" ? <CountriesTab /> : <CountryRequestsTab />}
    </div>
  );
};

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px ${
        active
          ? "border-primary-500 text-primary-500"
          : "border-transparent text-text-secondary hover:text-text-secondary"
      }`}
    >
      {children}
    </button>
  );
}

// ── Countries Tab ─────────────────────────────────────────────────────

function CountriesTab() {
  const { data, isLoading, isFetching, error, refetch } =
    useRentRegulationCountries();
  const createCountry = useCreateCountry();
  const navigate = useNavigate();

  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CountryForm>(emptyForm);

  const countries = useMemo(() => {
    const all = data ?? [];
    if (!search.trim()) {
      return all;
    }
    const q = search.toLowerCase();
    return all.filter(
      (c) =>
        c.countryName.toLowerCase().includes(q) ||
        c.countryCode.toLowerCase().includes(q),
    );
  }, [data, search]);

  const openCreate = () => {
    setForm(emptyForm);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setForm(emptyForm);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createCountry.mutate(
      {
        countryCode: form.countryCode.toUpperCase(),
        countryName: form.countryName,
        hasRegionalRegulations: form.hasRegionalRegulations,
        summary: form.summary || undefined,
      },
      { onSuccess: closeForm },
    );
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading rent regulations..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load rent regulations.</p>
      </div>
    );
  }

  return (
    <>
      {/* Header */}
      <div
        className="mb-6"
        style={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-text-primary">
            Rent Regulations
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage rent regulation rules by country. Keep data reviewed and
            up-to-date.
          </p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
          <Button
            variant="primary"
            size="sm"
            leftIcon={<Plus />}
            onClick={openCreate}
          >
            Add Country
          </Button>
        </div>
      </div>

      {/* Search */}
      <div className="mb-4 relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
        <input
          type="search"
          placeholder="Search by country name or code..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary placeholder-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
        />
      </div>

      {/* Table */}
      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default">
                <th className={TH_CLASS}>Country</th>
                <th className={TH_CLASS}>Code</th>
                <th className={TH_CLASS}>Regional?</th>
                <th className={TH_CLASS}>Last Reviewed</th>
                <th className={TH_CLASS}>Status</th>
              </tr>
            </thead>
            <tbody>
              {countries.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-12 text-center text-sm text-text-muted"
                  >
                    No countries found.
                  </td>
                </tr>
              ) : (
                countries.map((country) => (
                  <CountryRow
                    key={country.identifier}
                    country={country}
                    onClick={() =>
                      navigate(`/rent-regulations/${country.countryCode}`)
                    }
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black/40" onClick={closeForm} />
          <div className="relative bg-surface-card rounded-lg border border-border-default shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
              <h2 className="text-lg font-semibold text-text-primary">
                Add Country
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
                    Country Code
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={2}
                    value={form.countryCode}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        countryCode: e.target.value.toUpperCase(),
                      }))
                    }
                    placeholder="NL"
                    className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Country Name
                  </label>
                  <input
                    type="text"
                    required
                    value={form.countryName}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, countryName: e.target.value }))
                    }
                    placeholder="Netherlands"
                    className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                  />
                </div>
              </div>
              <div>
                <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.hasRegionalRegulations}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        hasRegionalRegulations: e.target.checked,
                      }))
                    }
                    className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
                  />
                  Has regional regulations
                </label>
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Summary
                  <span className="text-text-muted font-normal ml-1">
                    (optional)
                  </span>
                </label>
                <RichTextEditor
                  value={form.summary}
                  onChange={(val) => setForm((f) => ({ ...f, summary: val }))}
                  placeholder="Brief overview of rent regulations in this country..."
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
                  isLoading={createCountry.isPending}
                >
                  Add Country
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}

function CountryRow({
  country,
  onClick,
}: {
  country: RentRegulationCountryResponse;
  onClick: () => void;
}) {
  return (
    <tr
      className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors cursor-pointer"
      onClick={onClick}
    >
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="text-base">
            {countryCodeToFlag(country.countryCode)}
          </span>
          <span className="text-sm font-medium text-text-primary">
            {country.countryName}
          </span>
        </div>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-text-secondary font-mono">
          {country.countryCode}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-text-secondary">
          {country.hasRegionalRegulations ? "Yes" : "No"}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-text-secondary">
          {country.lastReviewedAt
            ? format(new Date(country.lastReviewedAt), "dd MMM yyyy")
            : "Never"}
        </span>
      </td>
      <td className="px-4 py-3">
        {country.stale ? (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-warning-bg text-warning-text ring-1 ring-warning-border">
            May be outdated
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-success-bg text-success-text ring-1 ring-success-border">
            Verified
          </span>
        )}
      </td>
    </tr>
  );
}

// ── Country Requests Tab ──────────────────────────────────────────────

function CountryRequestsTab() {
  const { data, isLoading, isFetching, error, refetch } =
    useCountryRegulationRequests();
  const dismissRequest = useDismissCountryRequest();

  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [dismissTarget, setDismissTarget] = useState<string | null>(null);

  const toggle = (countryName: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(countryName)) {
        next.delete(countryName);
      } else {
        next.add(countryName);
      }
      return next;
    });
  };

  const handleDismiss = () => {
    if (!dismissTarget) {
      return;
    }
    dismissRequest.mutate(dismissTarget, {
      onSuccess: () => setDismissTarget(null),
    });
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading country requests..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load country requests.</p>
      </div>
    );
  }

  const requests = data ?? [];

  return (
    <>
      {/* Header */}
      <div
        className="mb-6"
        style={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-text-primary">
            Country Requests
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Countries that users have requested regulation data for, sorted by
            demand.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {requests.length === 0 ? (
        <div className="bg-surface-card rounded-lg border border-border-default px-6 py-12 text-center">
          <Globe className="h-10 w-10 mx-auto text-text-muted mb-3" />
          <p className="text-sm text-text-secondary">
            No country requests yet. Users can request countries from the Rent
            Regulations page.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {requests.map((req) => (
            <RequestCard
              key={req.countryName}
              request={req}
              isExpanded={expanded.has(req.countryName)}
              onToggle={() => toggle(req.countryName)}
              onDismiss={() => setDismissTarget(req.countryName)}
            />
          ))}
        </div>
      )}

      {dismissTarget && (
        <ConfirmDialog
          onCancel={() => setDismissTarget(null)}
          onConfirm={handleDismiss}
          isLoading={dismissRequest.isPending}
          title="Dismiss Country Request"
          message={`Dismiss all requests for "${dismissTarget}"? This will remove the request and all associated requester data. This action cannot be undone.`}
          confirmLabel="Dismiss"
          variant="danger"
        />
      )}
    </>
  );
}

function RequestCard({
  request,
  isExpanded,
  onToggle,
  onDismiss,
}: {
  request: CountryRegulationRequestSummary;
  isExpanded: boolean;
  onToggle: () => void;
  onDismiss: () => void;
}) {
  return (
    <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
      {/* Summary row */}
      <div
        className="flex items-center gap-4 px-5 py-4 cursor-pointer hover:bg-surface-page transition-colors"
        onClick={onToggle}
      >
        <div className="text-text-muted">
          {isExpanded ? (
            <ChevronDown className="h-4 w-4" />
          ) : (
            <ChevronRight className="h-4 w-4" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3">
            <span className="text-sm font-semibold text-text-primary">
              {request.countryName}
            </span>
            <span className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium bg-primary-500/10 text-primary-500 ring-1 ring-primary-500/20">
              <Users className="h-3 w-3" />
              {request.requestCount}{" "}
              {request.requestCount === 1 ? "request" : "requests"}
            </span>
          </div>
          <div className="flex items-center gap-4 mt-1">
            <span className="flex items-center gap-1 text-xs text-text-muted">
              <Clock className="h-3 w-3" />
              First: {format(new Date(request.firstRequestedAt), "dd MMM yyyy")}
            </span>
            <span className="flex items-center gap-1 text-xs text-text-muted">
              <Clock className="h-3 w-3" />
              Last:{" "}
              {formatDistanceToNow(new Date(request.lastRequestedAt), {
                addSuffix: true,
              })}
            </span>
          </div>
        </div>

        <Button
          variant="ghost"
          size="sm"
          leftIcon={<Trash2 className="h-3.5 w-3.5" />}
          onClick={(e) => {
            e.stopPropagation();
            onDismiss();
          }}
          className="text-error-text hover:text-error-text hover:bg-error-bg"
        >
          Dismiss
        </Button>
      </div>

      {/* Expanded requester details grouped by team */}
      {isExpanded && (
        <div className="border-t border-border-default">
          {groupByTeam(request.requesters).map(([teamKey, members]) => (
            <div key={teamKey}>
              <div className="px-5 py-2.5 bg-surface-page flex items-center gap-2 border-b border-border-default">
                <Users className="h-3.5 w-3.5 text-text-secondary " />
                <span className="text-xs font-semibold text-text-secondary">
                  {members[0].teamName}
                </span>
                <span className="text-xs text-text-muted font-mono">
                  {members[0].teamIdentifier}
                </span>
              </div>
              <div className="divide-y divide-border-default">
                {members.map((requester, idx) => (
                  <div
                    key={idx}
                    className="px-5 py-3 flex items-start gap-4 text-sm pl-10"
                  >
                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-medium text-text-primary">
                          {requester.userName}
                        </span>
                        <span className="text-text-muted font-mono text-xs">
                          {requester.userIdentifier}
                        </span>
                      </div>
                      {requester.notes && (
                        <div className="flex items-start gap-1.5 text-text-secondary">
                          <MessageSquareText className="h-3.5 w-3.5 mt-0.5 shrink-0" />
                          <span className="text-xs">{requester.notes}</span>
                        </div>
                      )}
                    </div>
                    <span className="text-xs text-text-muted whitespace-nowrap">
                      {format(
                        new Date(requester.requestedAt),
                        "dd MMM yyyy HH:mm",
                      )}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
