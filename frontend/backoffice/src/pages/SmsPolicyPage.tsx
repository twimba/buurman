import { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Save, Loader2, ChevronRight, Info } from 'lucide-react';
import { RefreshButton } from '@buurman/ui';
import {
  usePhonePolicy,
  usePhonePolicyMetadata,
  useUpdatePhonePolicy,
} from '../hooks/useSettings';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { formatDateTime } from '../utils/dateFormatting';
import type { CountryGroupResponse } from '../types';

const TYPE_LABELS: Record<string, string> = {
  MOBILE: 'Mobile',
  FIXED_LINE: 'Fixed',
  FIXED_LINE_OR_MOBILE: 'Fixed/Mobile',
  VOIP: 'VoIP',
  TOLL_FREE: 'Toll-Free',
  PREMIUM_RATE: 'Premium',
  SHARED_COST: 'Shared',
  PERSONAL_NUMBER: 'Personal',
  PAGER: 'Pager',
  UAN: 'UAN',
};

const TYPE_TOOLTIPS: Record<string, string> = {
  MOBILE: 'Standard mobile/cellular numbers. Most common for SMS verification.',
  FIXED_LINE:
    'Landline numbers. Some can receive SMS depending on the carrier.',
  FIXED_LINE_OR_MOBILE:
    'Numbers that could be either mobile or fixed line. Common in countries where number ranges overlap.',
  VOIP: 'Internet-based phone numbers (e.g. Google Voice, Skype). Higher fraud risk but used legitimately by some users.',
  TOLL_FREE:
    'Free-to-call numbers (e.g. 0800). Often used by businesses. Sending SMS to these can be expensive.',
  PREMIUM_RATE:
    'Premium-rate numbers that charge the caller extra. High abuse risk \u2014 sending SMS here is costly.',
  SHARED_COST:
    'Numbers where the cost is split between caller and recipient. Uncommon for personal use.',
  PERSONAL_NUMBER:
    'Numbers that follow the user across locations. Rare and carrier-dependent.',
  PAGER: 'Pager devices. Very limited SMS support, largely obsolete.',
  UAN: 'Universal Access Numbers that route to different destinations. Typically used by businesses, not individuals.',
};

const countryCodeToFlag = (code: string): string =>
  code
    .toUpperCase()
    .split('')
    .map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65))
    .join('');

// Tri-state: "all" | "some" | "none"
type CheckState = 'all' | 'some' | 'none';

function getCheckState(
  countryCodes: string[],
  type: string,
  matrix: Record<string, string[]>
): CheckState {
  let has = 0;
  for (const code of countryCodes) {
    if (matrix[code]?.includes(type)) {
      has++;
    }
  }
  if (has === 0) {
    return 'none';
  }
  if (has === countryCodes.length) {
    return 'all';
  }
  return 'some';
}

function getRowCheckState(
  countryCodes: string[],
  types: string[],
  matrix: Record<string, string[]>
): CheckState {
  const totalCells = countryCodes.length * types.length;
  let checked = 0;
  for (const code of countryCodes) {
    for (const type of types) {
      if (matrix[code]?.includes(type)) {
        checked++;
      }
    }
  }
  if (checked === 0) {
    return 'none';
  }
  if (checked === totalCells) {
    return 'all';
  }
  return 'some';
}

function TriStateCheckbox({
  state,
  onChange,
  className = '',
}: {
  state: CheckState;
  onChange: () => void;
  className?: string;
}) {
  const ref = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.indeterminate = state === 'some';
    }
  }, [state]);

  return (
    <input
      ref={ref}
      type="checkbox"
      checked={state === 'all'}
      onChange={onChange}
      className={`rounded text-primary-500 focus:ring-primary-500 border-border-strong cursor-pointer ${className}`}
    />
  );
}

function InfoTooltip({ text }: { text: string }) {
  const [visible, setVisible] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const iconRef = useRef<HTMLSpanElement>(null);

  const show = () => {
    if (iconRef.current) {
      const rect = iconRef.current.getBoundingClientRect();
      setPos({
        top: rect.bottom + 6,
        left: rect.left + rect.width / 2,
      });
    }
    setVisible(true);
  };

  return (
    <>
      <span
        ref={iconRef}
        onMouseEnter={show}
        onMouseLeave={() => setVisible(false)}
        className="inline-flex cursor-help"
      >
        <Info className="h-3 w-3 text-text-muted " />
      </span>
      {visible &&
        createPortal(
          <div
            style={{ top: pos.top, left: pos.left }}
            className="fixed z-[9999] -translate-x-1/2 w-56 px-3 py-2 text-xs text-left font-normal leading-relaxed text-white bg-neutral-900 rounded-lg shadow-lg pointer-events-none"
          >
            {text}
          </div>,
          document.body
        )}
    </>
  );
}

export const SmsPolicyPage = () => {
  const { data, isLoading, isFetching, error, refetch } = usePhonePolicy();
  const { data: metadata, isLoading: metaLoading } = usePhonePolicyMetadata();
  const updatePolicy = useUpdatePhonePolicy();

  const [matrix, setMatrix] = useState<Record<string, string[]>>({});
  const [maxCodesPerHour, setMaxCodesPerHour] = useState(3);
  const [verificationCodeExpiryMinutes, setVerificationCodeExpiryMinutes] =
    useState(10);
  const [synced, setSynced] = useState(false);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  if (data && !synced) {
    setMatrix(data.policyMatrix ?? {});
    setMaxCodesPerHour(data.maxCodesPerHour);
    setVerificationCodeExpiryMinutes(data.verificationCodeExpiryMinutes);
    setSynced(true);
  }

  const numberTypes = metadata?.numberTypes ?? [];
  const groups = metadata?.countryGroups ?? [];
  const allCountryCodes = groups.flatMap((g) => g.countries.map((c) => c.code));

  const toggleExpanded = (groupId: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(groupId)) {
        next.delete(groupId);
      } else {
        next.add(groupId);
      }
      return next;
    });
  };

  // Toggle a single cell
  const toggleCell = useCallback((country: string, type: string) => {
    setMatrix((prev) => {
      const types = prev[country] ? [...prev[country]] : [];
      const idx = types.indexOf(type);
      if (idx >= 0) {
        types.splice(idx, 1);
      } else {
        types.push(type);
      }
      return { ...prev, [country]: types };
    });
  }, []);

  // Toggle a type for a set of countries
  const toggleTypeForCountries = useCallback(
    (countryCodes: string[], type: string) => {
      setMatrix((prev) => {
        const state = getCheckState(countryCodes, type, prev);
        const enable = state !== 'all';
        const next = { ...prev };
        for (const code of countryCodes) {
          const types = next[code] ? [...next[code]] : [];
          const idx = types.indexOf(type);
          if (enable && idx < 0) {
            types.push(type);
          }
          if (!enable && idx >= 0) {
            types.splice(idx, 1);
          }
          next[code] = types;
        }
        return next;
      });
    },
    []
  );

  // Toggle all types for a set of countries (row-level)
  const toggleAllForCountries = useCallback(
    (countryCodes: string[], types: string[]) => {
      setMatrix((prev) => {
        const state = getRowCheckState(countryCodes, types, prev);
        const enable = state !== 'all';
        const next = { ...prev };
        for (const code of countryCodes) {
          next[code] = enable ? [...types] : [];
        }
        return next;
      });
    },
    []
  );

  // Toggle a column (type) for ALL countries
  const toggleColumn = useCallback(
    (type: string) => {
      toggleTypeForCountries(allCountryCodes, type);
    },
    [allCountryCodes, toggleTypeForCountries]
  );

  const handleSave = () => {
    // Strip countries with empty type arrays
    const cleaned: Record<string, string[]> = {};
    for (const [code, types] of Object.entries(matrix)) {
      if (types && types.length > 0) {
        cleaned[code] = types;
      }
    }
    updatePolicy.mutate({
      policyMatrix: cleaned,
      maxCodesPerHour,
      verificationCodeExpiryMinutes,
    });
  };

  if (isLoading || metaLoading) {
    return <LoadingSpinner message="Loading settings..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-error-text">Failed to load settings.</p>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">SMS Policy</h1>
          <p className="text-sm text-text-secondary mt-1">
            Control which phone numbers users can register by country and type.
          </p>
        </div>
        <RefreshButton onClick={() => refetch()} isRefreshing={isFetching} />
      </div>

      {/* Verification Settings Card */}
      <div className="bg-surface-card rounded-lg border border-border-default mb-6">
        <div className="p-6 border-b border-border-default">
          <h2 className="text-lg font-semibold text-text-primary">
            Verification Settings
          </h2>
          <p className="text-sm text-text-secondary mt-1">
            Configure SMS verification code behavior.
          </p>
        </div>
        <div className="p-6 grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">
              Max verification codes per hour
            </label>
            <input
              type="number"
              min={1}
              max={20}
              value={maxCodesPerHour}
              onChange={(e) =>
                setMaxCodesPerHour(
                  Math.max(1, Math.min(20, Number(e.target.value) || 1))
                )
              }
              className="w-full px-3 py-2 text-sm border border-border-strong rounded-lg bg-surface-card text-text-primary outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
            <p className="text-xs text-text-muted mt-1">
              How many SMS codes a user can request within one hour (1–20).
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">
              Verification code expiry (minutes)
            </label>
            <input
              type="number"
              min={1}
              max={60}
              value={verificationCodeExpiryMinutes}
              onChange={(e) =>
                setVerificationCodeExpiryMinutes(
                  Math.max(1, Math.min(60, Number(e.target.value) || 1))
                )
              }
              className="w-full px-3 py-2 text-sm border border-border-strong rounded-lg bg-surface-card text-text-primary outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
            <p className="text-xs text-text-muted mt-1">
              How long a verification code stays valid (1–60 minutes).
            </p>
          </div>
        </div>
      </div>

      {/* Phone Number Policy Card */}
      <div className="bg-surface-card rounded-lg border border-border-default">
        <div className="p-6 border-b border-border-default">
          <h2 className="text-lg font-semibold text-text-primary">
            Phone Number Policy
          </h2>
          <p className="text-sm text-text-secondary mt-1">
            Control which phone numbers users can register by country and type.
            Changes take effect immediately.
          </p>
        </div>

        {/* Matrix Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-default">
                <th className="text-left py-3 px-4 font-medium text-text-secondary min-w-[220px] sticky left-0 bg-surface-card z-10">
                  Region / Country
                </th>
                {numberTypes.map((type) => (
                  <th
                    key={type}
                    className="py-3 px-2 text-center font-medium text-text-secondary min-w-[70px]"
                  >
                    <div className="flex flex-col items-center gap-1.5">
                      <span className="text-[11px] leading-tight flex items-center gap-0.5">
                        {TYPE_LABELS[type] || type}
                        {TYPE_TOOLTIPS[type] && (
                          <InfoTooltip text={TYPE_TOOLTIPS[type]} />
                        )}
                      </span>
                      <TriStateCheckbox
                        state={getCheckState(allCountryCodes, type, matrix)}
                        onChange={() => toggleColumn(type)}
                      />
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {groups.map((group) => (
                <GroupRows
                  key={group.groupId}
                  group={group}
                  numberTypes={numberTypes}
                  matrix={matrix}
                  isExpanded={expanded.has(group.groupId)}
                  onToggleExpand={() => toggleExpanded(group.groupId)}
                  onToggleCell={toggleCell}
                  onToggleTypeForCountries={toggleTypeForCountries}
                  onToggleAllForCountries={toggleAllForCountries}
                />
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-border-default flex items-center justify-between">
          <div>
            {data?.updatedAt && (
              <p className="text-xs text-text-muted">
                Last updated {formatDateTime(data.updatedAt)}
                {data.updatedBy ? ` by ${data.updatedBy}` : ''}
              </p>
            )}
          </div>
          <button
            onClick={handleSave}
            disabled={updatePolicy.isPending}
            className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50 text-sm font-medium"
          >
            {updatePolicy.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            Save Policy
          </button>
        </div>
      </div>
    </div>
  );
};

function GroupRows({
  group,
  numberTypes,
  matrix,
  isExpanded,
  onToggleExpand,
  onToggleCell,
  onToggleTypeForCountries,
  onToggleAllForCountries,
}: {
  group: CountryGroupResponse;
  numberTypes: string[];
  matrix: Record<string, string[]>;
  isExpanded: boolean;
  onToggleExpand: () => void;
  onToggleCell: (country: string, type: string) => void;
  onToggleTypeForCountries: (codes: string[], type: string) => void;
  onToggleAllForCountries: (codes: string[], types: string[]) => void;
}) {
  const codes = group.countries.map((c) => c.code);
  const rowState = getRowCheckState(codes, numberTypes, matrix);

  // Count enabled countries (countries with at least one type)
  const enabledCount = codes.filter(
    (c) => matrix[c] && matrix[c].length > 0
  ).length;

  return (
    <>
      {/* Group header row */}
      <tr className="border-b border-border-default bg-surface-page hover:bg-surface-inset">
        <td className="py-2.5 px-4 sticky left-0 bg-surface-page z-10">
          <div className="flex items-center gap-2">
            <TriStateCheckbox
              state={rowState}
              onChange={() => onToggleAllForCountries(codes, numberTypes)}
            />
            <button
              onClick={onToggleExpand}
              className="flex items-center gap-1.5 text-sm font-semibold text-text-primary hover:text-primary-500 transition-colors"
            >
              <ChevronRight
                className={`h-3.5 w-3.5 transition-transform duration-150 ${isExpanded ? 'rotate-90' : ''}`}
              />
              {group.groupName}
            </button>
            <span className="text-[11px] text-text-muted">
              ({enabledCount}/{codes.length})
            </span>
          </div>
        </td>
        {numberTypes.map((type) => (
          <td key={type} className="py-2.5 px-2 text-center">
            <TriStateCheckbox
              state={getCheckState(codes, type, matrix)}
              onChange={() => onToggleTypeForCountries(codes, type)}
            />
          </td>
        ))}
      </tr>

      {/* Expanded country rows */}
      {isExpanded &&
        group.countries.map((country) => (
          <tr
            key={country.code}
            className="border-b border-border-default/50 hover:bg-surface-page"
          >
            <td className="py-2 px-4 pl-12 sticky left-0 bg-surface-card z-10">
              <div className="flex items-center gap-2">
                <TriStateCheckbox
                  state={getRowCheckState([country.code], numberTypes, matrix)}
                  onChange={() =>
                    onToggleAllForCountries([country.code], numberTypes)
                  }
                />
                <span
                  className="text-base w-6 text-center cursor-default"
                  title={country.code}
                >
                  {countryCodeToFlag(country.code)}
                </span>
                <span className="text-sm text-text-secondary">
                  {country.name}
                </span>
              </div>
            </td>
            {numberTypes.map((type) => {
              const checked = matrix[country.code]?.includes(type) ?? false;
              return (
                <td key={type} className="py-2 px-2 text-center">
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => onToggleCell(country.code, type)}
                    className="rounded text-primary-500 focus:ring-primary-500 border-border-strong cursor-pointer"
                  />
                </td>
              );
            })}
          </tr>
        ))}
    </>
  );
}
