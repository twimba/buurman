import { useState, useMemo } from "react";
import {
  Flag,
  Filter,
  CheckCircle2,
  XCircle,
  Minus,
  Shield,
  Crown,
  Building2,
  User,
} from "lucide-react";
import { RefreshButton } from "@buurman/ui";
import {
  useGlobalFeatureFlags,
  useUserFeatureFlags,
} from "../hooks/useFeatureFlags";
import type { FlagMap, TeamFlagEvaluation } from "../api/featureFlags";

const FlagBadge = ({ enabled }: { enabled: boolean }) => (
  <span
    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide ${
      enabled
        ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400"
        : "bg-red-50 text-red-600 dark:bg-red-500/10 dark:text-red-400"
    }`}
  >
    {enabled ? (
      <CheckCircle2 className="h-3.5 w-3.5" />
    ) : (
      <XCircle className="h-3.5 w-3.5" />
    )}
    {enabled ? "Enabled" : "Disabled"}
  </span>
);

const OverrideBadge = () => (
  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-amber-50 text-amber-600 dark:bg-amber-500/10 dark:text-amber-400 border border-amber-200 dark:border-amber-500/20">
    Override
  </span>
);

const RoleBadge = ({ role }: { role: string }) => {
  const label = role.replace("TEAM_", "");
  const colors: Record<string, string> = {
    ADMIN:
      "bg-violet-50 text-violet-700 dark:bg-violet-500/10 dark:text-violet-400 border-violet-200 dark:border-violet-500/20",
    EDITOR:
      "bg-sky-50 text-sky-700 dark:bg-sky-500/10 dark:text-sky-400 border-sky-200 dark:border-sky-500/20",
    VIEWER:
      "bg-slate-50 text-slate-600 dark:bg-slate-500/10 dark:text-slate-400 border-slate-200 dark:border-slate-500/20",
  };
  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border ${colors[label] ?? colors.VIEWER}`}
    >
      <Shield className="h-2.5 w-2.5" />
      {label}
    </span>
  );
};

const FlagRow = ({
  name,
  flag,
  globalFlag,
  showOverrideOnly,
}: {
  name: string;
  flag: { enabled: boolean; value: unknown };
  globalFlag?: { enabled: boolean; value: unknown };
  showOverrideOnly: boolean;
}) => {
  const isOverride =
    globalFlag !== undefined && flag.enabled !== globalFlag.enabled;
  if (showOverrideOnly && !isOverride) return null;

  return (
    <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f1f3f9]/50 dark:hover:bg-[#1a1d2e]/50 transition-colors">
      <td className="px-5 py-3.5">
        <div className="flex items-center gap-2.5">
          <code className="text-sm font-mono font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
            {name}
          </code>
          {isOverride && <OverrideBadge />}
        </div>
      </td>
      <td className="px-5 py-3.5">
        <FlagBadge enabled={flag.enabled} />
      </td>
      {globalFlag !== undefined && (
        <td className="px-5 py-3.5">
          <FlagBadge enabled={globalFlag.enabled} />
        </td>
      )}
      <td className="px-5 py-3.5 text-sm text-[#6b7194] dark:text-[#8b90a8] font-mono">
        {flag.value !== null && flag.value !== undefined ? (
          String(flag.value)
        ) : (
          <Minus className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
        )}
      </td>
    </tr>
  );
};

const FlagTable = ({
  flags,
  globalFlags,
  showOverrideOnly,
  isUserContext,
}: {
  flags: FlagMap;
  globalFlags?: FlagMap;
  showOverrideOnly: boolean;
  isUserContext: boolean;
}) => {
  const sortedNames = useMemo(() => Object.keys(flags).sort(), [flags]);

  const visibleCount = showOverrideOnly
    ? sortedNames.filter(
        (name) =>
          globalFlags && flags[name]?.enabled !== globalFlags[name]?.enabled,
      ).length
    : sortedNames.length;

  if (sortedNames.length === 0) {
    return (
      <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180]">
        <Flag className="h-10 w-10 mx-auto mb-3 opacity-40" />
        <p className="text-sm">No feature flags configured</p>
      </div>
    );
  }

  if (showOverrideOnly && visibleCount === 0) {
    return (
      <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180]">
        <CheckCircle2 className="h-10 w-10 mx-auto mb-3 opacity-40" />
        <p className="text-sm">All flags match the global defaults</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Flag
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              {isUserContext ? "User Status" : "Status"}
            </th>
            {isUserContext && (
              <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
                Global Default
              </th>
            )}
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Value
            </th>
          </tr>
        </thead>
        <tbody>
          {sortedNames.map((name) => (
            <FlagRow
              key={name}
              name={name}
              flag={flags[name]}
              globalFlag={globalFlags?.[name]}
              showOverrideOnly={showOverrideOnly}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
};

const TeamFlagSection = ({
  evaluation,
  globalFlags,
  showOverrideOnly,
}: {
  evaluation: TeamFlagEvaluation;
  globalFlags?: FlagMap;
  showOverrideOnly: boolean;
}) => {
  const overrideCount = globalFlags
    ? Object.keys(evaluation.flags).filter(
        (k) => evaluation.flags[k]?.enabled !== globalFlags[k]?.enabled,
      ).length
    : 0;

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
      <div className="flex items-center justify-between px-5 py-3 border-b border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#0f1120]">
        <div className="flex items-center gap-2.5 flex-wrap">
          <Building2 className="h-4 w-4 text-[#5c7cfa]" />
          <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            {evaluation.teamName}
          </span>
          <RoleBadge role={evaluation.role} />
          {evaluation.isOwner && (
            <Crown className="h-3.5 w-3.5 text-amber-500" />
          )}
          {evaluation.isActive && (
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/20">
              Active
            </span>
          )}
          {overrideCount > 0 && (
            <span className="text-xs font-medium text-amber-600 dark:text-amber-400">
              {overrideCount} override{overrideCount !== 1 ? "s" : ""}
            </span>
          )}
        </div>
        <span className="text-[10px] font-mono text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0 ml-3">
          {evaluation.teamIdentifier}
        </span>
      </div>
      <FlagTable
        flags={evaluation.flags}
        globalFlags={globalFlags}
        showOverrideOnly={showOverrideOnly}
        isUserContext={true}
      />
    </div>
  );
};

/**
 * Reusable component that shows feature flag evaluations for a given user
 * across all their team memberships, compared against global defaults.
 */
export const UserFeatureFlags = ({
  userIdentifier,
}: {
  userIdentifier: string;
}) => {
  const [showOverrideOnly, setShowOverrideOnly] = useState(false);

  const {
    data: globalFlags,
    isFetching: globalFetching,
    refetch: refetchGlobal,
  } = useGlobalFeatureFlags();

  const {
    data: teamEvaluations,
    isLoading: userLoading,
    isFetching: userFetching,
    refetch: refetchUser,
  } = useUserFeatureFlags(userIdentifier);

  const totalOverrides =
    teamEvaluations && globalFlags
      ? teamEvaluations.reduce(
          (total, ev) =>
            total +
            Object.keys(ev.flags).filter(
              (k) => ev.flags[k]?.enabled !== globalFlags[k]?.enabled,
            ).length,
          0,
        )
      : 0;

  return (
    <div className="space-y-4">
      {/* Section header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <Flag className="h-4 w-4 text-[#5c7cfa]" />
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Feature Flags
          </h2>
          <span className="text-[10px] font-mono text-[#9ca0b8] dark:text-[#5c6180]">
            {userIdentifier}
          </span>
          {totalOverrides > 0 && (
            <span className="text-xs font-medium text-amber-600 dark:text-amber-400">
              {totalOverrides} override{totalOverrides !== 1 ? "s" : ""} across{" "}
              {teamEvaluations?.length} team
              {(teamEvaluations?.length ?? 0) !== 1 ? "s" : ""}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowOverrideOnly(!showOverrideOnly)}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              showOverrideOnly
                ? "bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400 border border-amber-200 dark:border-amber-500/20"
                : "text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] border border-[#e2e6f0] dark:border-[#2a2e3f]"
            }`}
          >
            <Filter className="h-3 w-3" />
            Overrides only
          </button>
          <RefreshButton
            onClick={() => {
              refetchGlobal();
              refetchUser();
            }}
            isRefreshing={globalFetching || userFetching}
          />
        </div>
      </div>

      {/* Per-team evaluations */}
      {userLoading ? (
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
          <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180] text-sm">
            Evaluating flags...
          </div>
        </div>
      ) : teamEvaluations && teamEvaluations.length > 0 ? (
        <div className="space-y-4">
          {teamEvaluations.map((evaluation) => (
            <TeamFlagSection
              key={evaluation.teamIdentifier}
              evaluation={evaluation}
              globalFlags={globalFlags}
              showOverrideOnly={showOverrideOnly}
            />
          ))}
        </div>
      ) : (
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
          <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180]">
            <User className="h-10 w-10 mx-auto mb-3 opacity-40" />
            <p className="text-sm">This user has no team memberships</p>
          </div>
        </div>
      )}
    </div>
  );
};

/** Exported for use by FeatureFlagsPage global section */
export { FlagTable };
export type { FlagMap };
