import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Search, X } from "lucide-react";
import { RefreshButton, Button } from "@buurman/ui";
import { format } from "date-fns";
import {
  useRentRegulationCountries,
  useCreateCountry,
} from "../hooks/useRentRegulationHooks";
import { LoadingSpinner } from "../components/LoadingSpinner";
import type { RentRegulationCountryResponse } from "../api/rentRegulations";

const TH_CLASS =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]";

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

export const RentRegulationsPage = () => {
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
        <p className="text-red-600 dark:text-red-400">
          Failed to load rent regulations.
        </p>
      </div>
    );
  }

  return (
    <div>
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
          <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Rent Regulations
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
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
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8]" />
        <input
          type="search"
          placeholder="Search by country name or code..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#9ca0b8] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
        />
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
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
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
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
          <div className="relative bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Add Country
              </h2>
              <button
                onClick={closeForm}
                className="p-1 rounded-md text-[#6b7194] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
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
                    className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
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
                    className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                  />
                </div>
              </div>
              <div>
                <label className="flex items-center gap-2 text-sm text-[#3d4463] dark:text-[#c4c8db] cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.hasRegionalRegulations}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        hasRegionalRegulations: e.target.checked,
                      }))
                    }
                    className="rounded border-[#cdd3e6] text-[#5c7cfa] focus:ring-[#5c7cfa]/20"
                  />
                  Has regional regulations
                </label>
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Summary
                  <span className="text-[#9ca0b8] font-normal ml-1">
                    (optional)
                  </span>
                </label>
                <textarea
                  value={form.summary}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, summary: e.target.value }))
                  }
                  rows={3}
                  placeholder="Brief overview of rent regulations in this country..."
                  className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors resize-none"
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
    </div>
  );
};

function CountryRow({
  country,
  onClick,
}: {
  country: RentRegulationCountryResponse;
  onClick: () => void;
}) {
  return (
    <tr
      className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors cursor-pointer"
      onClick={onClick}
    >
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="text-base">
            {countryCodeToFlag(country.countryCode)}
          </span>
          <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
            {country.countryName}
          </span>
        </div>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#3d4463] dark:text-[#c4c8db] font-mono">
          {country.countryCode}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          {country.hasRegionalRegulations ? "Yes" : "No"}
        </span>
      </td>
      <td className="px-4 py-3">
        <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          {country.lastReviewedAt
            ? format(new Date(country.lastReviewedAt), "dd MMM yyyy")
            : "Never"}
        </span>
      </td>
      <td className="px-4 py-3">
        {country.stale ? (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700">
            May be outdated
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700">
            Verified
          </span>
        )}
      </td>
    </tr>
  );
}
