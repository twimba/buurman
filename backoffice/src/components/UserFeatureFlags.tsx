import { useState, useMemo, useRef, useEffect, useCallback } from "react";
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
  Loader2,
  Pencil,
  Plus,
  Trash2,
  X,
  Info,
  Layers,
} from "lucide-react";
import { RefreshButton, ConfirmDialog } from "@buurman/ui";
import {
  useGlobalFeatureFlags,
  useUserFeatureFlags,
  useUpsertIdentityOverride,
  useDeleteIdentityOverride,
  useSegmentFeatureFlags,
  useUpsertSegmentOverride,
  useDeleteSegmentOverride,
} from "../hooks/useFeatureFlags";
import type {
  FlagMap,
  TeamFlagEvaluation,
  SegmentEvaluation,
} from "../api/featureFlags";

// --- Propagation banner ---

const PropagationBanner = ({
  visible,
  onDismiss,
}: {
  visible: boolean;
  onDismiss: () => void;
}) => {
  if (!visible) { return null; }
  return (
    <div className="flex items-center gap-3 px-4 py-2.5 rounded-lg bg-blue-50 dark:bg-blue-500/10 border border-blue-200 dark:border-blue-500/20 text-sm text-blue-700 dark:text-blue-400">
      <Info className="h-4 w-4 flex-shrink-0" />
      <span className="flex-1">
        Flag updated in Flagsmith. The app will pick up this change within ~60
        seconds.
      </span>
      <button
        onClick={onDismiss}
        className="flex-shrink-0 p-0.5 rounded hover:bg-blue-100 dark:hover:bg-blue-500/20 transition-colors"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
};

// --- Toggle switch ---

const ToggleSwitch = ({
  enabled,
  loading,
  onChange,
}: {
  enabled: boolean;
  loading?: boolean;
  onChange: (enabled: boolean) => void;
}) => (
  <button
    onClick={() => !loading && onChange(!enabled)}
    disabled={loading}
    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]/30 disabled:opacity-50 ${
      enabled
        ? "bg-emerald-500 dark:bg-emerald-600"
        : "bg-[#c9cfd9] dark:bg-[#3a3f54]"
    }`}
  >
    <span
      className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform duration-200 ${
        enabled ? "translate-x-6" : "translate-x-1"
      }`}
    />
    {loading && (
      <Loader2 className="absolute -right-6 h-3.5 w-3.5 animate-spin text-[#9ca0b8]" />
    )}
  </button>
);

// --- Inline value editor ---

const InlineValueEditor = ({
  value,
  onSave,
  loading,
}: {
  value: unknown;
  onSave: (value: string | null) => void;
  loading?: boolean;
}) => {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  const startEdit = () => {
    setDraft(value !== null && value !== undefined ? String(value) : "");
    setEditing(true);
  };

  const hasValue =
    value !== null && value !== undefined && String(value) !== "";

  const save = () => {
    setEditing(false);
    const newValue = draft.trim();
    const oldValue = hasValue ? String(value) : "";
    if (newValue !== oldValue) {
      // Send "" to clear, non-empty to set
      onSave(newValue || "");
    }
  };

  const cancel = () => setEditing(false);

  if (editing) {
    return (
      <div className="flex items-center gap-1.5">
        <input
          ref={inputRef}
          type="text"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") { save(); }
            if (e.key === "Escape") { cancel(); }
          }}
          onBlur={save}
          disabled={loading}
          className="w-32 px-2 py-1 text-sm font-mono rounded border border-[#5c7cfa] bg-white dark:bg-[#1a1d2e] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-1 focus:ring-[#5c7cfa]/30"
          placeholder="empty"
        />
      </div>
    );
  }

  return (
    <div className="group/val flex items-center gap-1">
      <button
        onClick={startEdit}
        className="flex items-center gap-1.5 text-sm font-mono text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] transition-colors"
        title="Click to edit value"
      >
        {hasValue ? (
          String(value)
        ) : (
          <Minus className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
        )}
        <Pencil className="h-3 w-3 opacity-0 group-hover/val:opacity-50 transition-opacity" />
      </button>
      {hasValue && !loading && (
        <button
          onClick={() => onSave("")}
          className="p-0.5 rounded text-[#9ca0b8] hover:text-red-500 dark:hover:text-red-400 opacity-0 group-hover/val:opacity-100 transition-all"
          title="Clear value"
        >
          <XCircle className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
};

// --- Badges ---

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

const ValueCell = ({ value }: { value: unknown }) => (
  <td className="px-5 py-3.5 text-sm text-[#6b7194] dark:text-[#8b90a8] font-mono">
    {value !== null && value !== undefined ? (
      String(value)
    ) : (
      <Minus className="h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
    )}
  </td>
);

// --- Helpers ---

const isOverridden = (
  flag: { enabled: boolean; value: unknown },
  globalFlag?: { enabled: boolean; value: unknown },
) => {
  if (!globalFlag) { return false; }
  return (
    flag.enabled !== globalFlag.enabled ||
    String(flag.value ?? "") !== String(globalFlag.value ?? "")
  );
};

// --- Global Flag Row (interactive) ---

const GlobalFlagRow = ({
  name,
  flag,
  onToggle,
  onValueChange,
  mutating,
}: {
  name: string;
  flag: { enabled: boolean; value: unknown };
  onToggle: (flagName: string, enabled: boolean) => void;
  onValueChange: (flagName: string, value: string | null) => void;
  mutating?: boolean;
}) => {
  return (
    <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f1f3f9]/50 dark:hover:bg-[#1a1d2e]/50 transition-colors">
      <td className="px-5 py-3.5">
        <code className="text-sm font-mono font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
          {name}
        </code>
      </td>
      <td className="px-5 py-3.5">
        <ToggleSwitch
          enabled={flag.enabled}
          loading={mutating}
          onChange={(enabled) => onToggle(name, enabled)}
        />
      </td>
      <td className="px-5 py-3.5">
        <InlineValueEditor
          value={flag.value}
          loading={mutating}
          onSave={(value) => onValueChange(name, value)}
        />
      </td>
    </tr>
  );
};

// --- User Flag Row (with override actions) ---

const UserFlagRow = ({
  name,
  flag,
  globalFlag,
  showOverrideOnly,
  userIdentifier,
  teamIdentifier,
  onUpsertOverride,
  onDeleteOverride,
  mutating,
}: {
  name: string;
  flag: { enabled: boolean; value: unknown };
  globalFlag?: { enabled: boolean; value: unknown };
  showOverrideOnly: boolean;
  userIdentifier: string;
  teamIdentifier: string;
  onUpsertOverride: (
    flagName: string,
    userIdentifier: string,
    teamIdentifier: string,
    enabled: boolean,
    value: string | null,
  ) => void;
  onDeleteOverride: (
    flagName: string,
    userIdentifier: string,
    teamIdentifier: string,
  ) => void;
  mutating?: boolean;
}) => {
  const override = isOverridden(flag, globalFlag);
  const [confirmDelete, setConfirmDelete] = useState(false);

  if (showOverrideOnly && !override) { return null; }

  return (
    <>
      <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f1f3f9]/50 dark:hover:bg-[#1a1d2e]/50 transition-colors">
        <td className="px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <code className="text-sm font-mono font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {name}
            </code>
            {override && <OverrideBadge />}
          </div>
        </td>
        <td className="px-5 py-3.5">
          <ToggleSwitch
            enabled={flag.enabled}
            loading={mutating}
            onChange={(enabled) =>
              onUpsertOverride(
                name,
                userIdentifier,
                teamIdentifier,
                enabled,
                flag.value !== null && flag.value !== undefined
                  ? String(flag.value)
                  : null,
              )
            }
          />
        </td>
        {globalFlag !== undefined && (
          <td className="px-5 py-3.5">
            <FlagBadge enabled={globalFlag.enabled} />
          </td>
        )}
        <td className="px-5 py-3.5">
          <InlineValueEditor
            value={flag.value}
            loading={mutating}
            onSave={(value) =>
              onUpsertOverride(
                name,
                userIdentifier,
                teamIdentifier,
                flag.enabled,
                value,
              )
            }
          />
        </td>
        {globalFlag !== undefined && <ValueCell value={globalFlag.value} />}
        <td className="px-5 py-3.5">
          {override ? (
            <button
              onClick={() => setConfirmDelete(true)}
              disabled={mutating}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors disabled:opacity-50"
              title="Remove override"
            >
              <Trash2 className="h-3 w-3" />
              Remove
            </button>
          ) : (
            <button
              onClick={() =>
                onUpsertOverride(
                  name,
                  userIdentifier,
                  teamIdentifier,
                  !flag.enabled,
                  flag.value !== null && flag.value !== undefined
                    ? String(flag.value)
                    : null,
                )
              }
              disabled={mutating}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium text-[#5c7cfa] hover:bg-[#5c7cfa]/10 transition-colors disabled:opacity-50"
              title="Create override"
            >
              <Plus className="h-3 w-3" />
              Override
            </button>
          )}
        </td>
      </tr>
      {confirmDelete && (
        <tr>
          <td colSpan={6} className="p-0">
            <ConfirmDialog
              title="Remove Override"
              message={`Remove the override for "${name}"? The flag will fall back to the global default.`}
              confirmLabel="Remove"
              variant="danger"
              isLoading={mutating}
              onConfirm={() => {
                onDeleteOverride(name, userIdentifier, teamIdentifier);
                setConfirmDelete(false);
              }}
              onCancel={() => setConfirmDelete(false)}
            />
          </td>
        </tr>
      )}
    </>
  );
};

// --- Tables ---

const GlobalFlagTable = ({
  flags,
  onToggle,
  onValueChange,
  mutatingFlag,
}: {
  flags: FlagMap;
  onToggle: (flagName: string, enabled: boolean) => void;
  onValueChange: (flagName: string, value: string | null) => void;
  mutatingFlag?: string | null;
}) => {
  const sortedNames = useMemo(() => Object.keys(flags).sort(), [flags]);

  if (sortedNames.length === 0) {
    return (
      <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180]">
        <Flag className="h-10 w-10 mx-auto mb-3 opacity-40" />
        <p className="text-sm">No feature flags configured</p>
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
              Enabled
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Value
            </th>
          </tr>
        </thead>
        <tbody>
          {sortedNames.map((name) => (
            <GlobalFlagRow
              key={name}
              name={name}
              flag={flags[name]}
              onToggle={onToggle}
              onValueChange={onValueChange}
              mutating={mutatingFlag === name}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
};

const UserFlagTable = ({
  flags,
  globalFlags,
  showOverrideOnly,
  userIdentifier,
  teamIdentifier,
  onUpsertOverride,
  onDeleteOverride,
  mutatingFlag,
}: {
  flags: FlagMap;
  globalFlags?: FlagMap;
  showOverrideOnly: boolean;
  userIdentifier: string;
  teamIdentifier: string;
  onUpsertOverride: (
    flagName: string,
    userIdentifier: string,
    teamIdentifier: string,
    enabled: boolean,
    value: string | null,
  ) => void;
  onDeleteOverride: (
    flagName: string,
    userIdentifier: string,
    teamIdentifier: string,
  ) => void;
  mutatingFlag?: string | null;
}) => {
  const sortedNames = useMemo(() => Object.keys(flags).sort(), [flags]);

  const visibleCount = showOverrideOnly
    ? sortedNames.filter((name) =>
        isOverridden(flags[name], globalFlags?.[name]),
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
              User Status
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Global Status
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              User Value
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Global Value
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {sortedNames.map((name) => (
            <UserFlagRow
              key={name}
              name={name}
              flag={flags[name]}
              globalFlag={globalFlags?.[name]}
              showOverrideOnly={showOverrideOnly}
              userIdentifier={userIdentifier}
              teamIdentifier={teamIdentifier}
              onUpsertOverride={onUpsertOverride}
              onDeleteOverride={onDeleteOverride}
              mutating={mutatingFlag === name}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
};

// --- Team section ---

const TeamFlagSection = ({
  evaluation,
  globalFlags,
  showOverrideOnly,
  userIdentifier,
  onUpsertOverride,
  onDeleteOverride,
  mutatingFlag,
}: {
  evaluation: TeamFlagEvaluation;
  globalFlags?: FlagMap;
  showOverrideOnly: boolean;
  userIdentifier: string;
  onUpsertOverride: (
    flagName: string,
    userIdentifier: string,
    teamIdentifier: string,
    enabled: boolean,
    value: string | null,
  ) => void;
  onDeleteOverride: (
    flagName: string,
    userIdentifier: string,
    teamIdentifier: string,
  ) => void;
  mutatingFlag?: string | null;
}) => {
  const overrideCount = globalFlags
    ? Object.keys(evaluation.flags).filter((k) =>
        isOverridden(evaluation.flags[k], globalFlags[k]),
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
      <UserFlagTable
        flags={evaluation.flags}
        globalFlags={globalFlags}
        showOverrideOnly={showOverrideOnly}
        userIdentifier={userIdentifier}
        teamIdentifier={evaluation.teamIdentifier}
        onUpsertOverride={onUpsertOverride}
        onDeleteOverride={onDeleteOverride}
        mutatingFlag={mutatingFlag}
      />
    </div>
  );
};

// --- Main export: UserFeatureFlags ---

export const UserFeatureFlags = ({
  userIdentifier,
}: {
  userIdentifier: string;
}) => {
  const [showOverrideOnly, setShowOverrideOnly] = useState(false);
  const [showBanner, setShowBanner] = useState(false);
  const [mutatingFlag, setMutatingFlag] = useState<string | null>(null);

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

  const upsertOverride = useUpsertIdentityOverride();
  const deleteOverride = useDeleteIdentityOverride();

  const handleUpsertOverride = useCallback(
    (
      flagName: string,
      uid: string,
      tid: string,
      enabled: boolean,
      value: string | null,
    ) => {
      setMutatingFlag(flagName);
      upsertOverride.mutate(
        {
          userIdentifier: uid,
          teamIdentifier: tid,
          flagName,
          data: { enabled, value },
        },
        {
          onSettled: () => setMutatingFlag(null),
          onSuccess: () => setShowBanner(true),
        },
      );
    },
    [upsertOverride],
  );

  const handleDeleteOverride = useCallback(
    (flagName: string, uid: string, tid: string) => {
      setMutatingFlag(flagName);
      deleteOverride.mutate(
        { userIdentifier: uid, teamIdentifier: tid, flagName },
        {
          onSettled: () => setMutatingFlag(null),
          onSuccess: () => setShowBanner(true),
        },
      );
    },
    [deleteOverride],
  );

  const totalOverrides =
    teamEvaluations && globalFlags
      ? teamEvaluations.reduce(
          (total, ev) =>
            total +
            Object.keys(ev.flags).filter((k) =>
              isOverridden(ev.flags[k], globalFlags[k]),
            ).length,
          0,
        )
      : 0;

  return (
    <div className="space-y-4">
      <PropagationBanner
        visible={showBanner}
        onDismiss={() => setShowBanner(false)}
      />

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
              userIdentifier={userIdentifier}
              onUpsertOverride={handleUpsertOverride}
              onDeleteOverride={handleDeleteOverride}
              mutatingFlag={mutatingFlag}
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

// --- Segment override row ---

const SegmentOverrideRow = ({
  name,
  flag,
  globalFlag,
  showOverrideOnly,
  segmentId,
  onUpsertOverride,
  onDeleteOverride,
  mutating,
}: {
  name: string;
  flag: { enabled: boolean; value: unknown };
  globalFlag?: { enabled: boolean; value: unknown };
  showOverrideOnly: boolean;
  segmentId: number;
  onUpsertOverride: (
    flagName: string,
    segmentId: number,
    enabled: boolean,
    value: string | null,
  ) => void;
  onDeleteOverride: (flagName: string, segmentId: number) => void;
  mutating?: boolean;
}) => {
  const override = isOverridden(flag, globalFlag);
  const [confirmDelete, setConfirmDelete] = useState(false);

  if (showOverrideOnly && !override) { return null; }

  return (
    <>
      <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f1f3f9]/50 dark:hover:bg-[#1a1d2e]/50 transition-colors">
        <td className="px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <code className="text-sm font-mono font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
              {name}
            </code>
            {override && <OverrideBadge />}
          </div>
        </td>
        <td className="px-5 py-3.5">
          <ToggleSwitch
            enabled={flag.enabled}
            loading={mutating}
            onChange={(enabled) =>
              onUpsertOverride(
                name,
                segmentId,
                enabled,
                flag.value !== null && flag.value !== undefined
                  ? String(flag.value)
                  : null,
              )
            }
          />
        </td>
        {globalFlag !== undefined && (
          <td className="px-5 py-3.5">
            <FlagBadge enabled={globalFlag.enabled} />
          </td>
        )}
        <td className="px-5 py-3.5">
          <InlineValueEditor
            value={flag.value}
            loading={mutating}
            onSave={(value) =>
              onUpsertOverride(name, segmentId, flag.enabled, value)
            }
          />
        </td>
        {globalFlag !== undefined && <ValueCell value={globalFlag.value} />}
        <td className="px-5 py-3.5">
          <button
            onClick={() => setConfirmDelete(true)}
            disabled={mutating}
            className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors disabled:opacity-50"
            title="Remove segment override"
          >
            <Trash2 className="h-3 w-3" />
            Remove
          </button>
        </td>
      </tr>
      {confirmDelete && (
        <tr>
          <td colSpan={6} className="p-0">
            <ConfirmDialog
              title="Remove Segment Override"
              message={`Remove the override for "${name}"? The flag will fall back to the global default for this segment.`}
              confirmLabel="Remove"
              variant="danger"
              isLoading={mutating}
              onConfirm={() => {
                onDeleteOverride(name, segmentId);
                setConfirmDelete(false);
              }}
              onCancel={() => setConfirmDelete(false)}
            />
          </td>
        </tr>
      )}
    </>
  );
};

// --- Segment flag table ---

const SegmentFlagTable = ({
  overrides,
  globalFlags,
  showOverrideOnly,
  segmentId,
  onUpsertOverride,
  onDeleteOverride,
  mutatingFlag,
}: {
  overrides: Record<string, { enabled: boolean; value: unknown }>;
  globalFlags?: FlagMap;
  showOverrideOnly: boolean;
  segmentId: number;
  onUpsertOverride: (
    flagName: string,
    segmentId: number,
    enabled: boolean,
    value: string | null,
  ) => void;
  onDeleteOverride: (flagName: string, segmentId: number) => void;
  mutatingFlag?: string | null;
}) => {
  const sortedNames = useMemo(() => Object.keys(overrides).sort(), [overrides]);

  if (sortedNames.length === 0) {
    return (
      <div className="text-center py-8 text-[#9ca0b8] dark:text-[#5c6180]">
        <Flag className="h-8 w-8 mx-auto mb-2 opacity-40" />
        <p className="text-sm">No overrides for this segment</p>
      </div>
    );
  }

  const visibleCount = showOverrideOnly
    ? sortedNames.filter((name) =>
        isOverridden(overrides[name], globalFlags?.[name]),
      ).length
    : sortedNames.length;

  if (showOverrideOnly && visibleCount === 0) {
    return (
      <div className="text-center py-8 text-[#9ca0b8] dark:text-[#5c6180]">
        <CheckCircle2 className="h-8 w-8 mx-auto mb-2 opacity-40" />
        <p className="text-sm">All overrides match global defaults</p>
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
              Segment Status
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Global Status
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Segment Value
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Global Value
            </th>
            <th className="text-left px-5 py-3 text-[10px] font-bold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {sortedNames.map((name) => (
            <SegmentOverrideRow
              key={name}
              name={name}
              flag={overrides[name]}
              globalFlag={globalFlags?.[name]}
              showOverrideOnly={showOverrideOnly}
              segmentId={segmentId}
              onUpsertOverride={onUpsertOverride}
              onDeleteOverride={onDeleteOverride}
              mutating={mutatingFlag === name}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
};

// --- Segment section ---

const SegmentFlagSection = ({
  segment,
  globalFlags,
  showOverrideOnly,
  onUpsertOverride,
  onDeleteOverride,
  mutatingFlag,
}: {
  segment: SegmentEvaluation;
  globalFlags?: FlagMap;
  showOverrideOnly: boolean;
  onUpsertOverride: (
    flagName: string,
    segmentId: number,
    enabled: boolean,
    value: string | null,
  ) => void;
  onDeleteOverride: (flagName: string, segmentId: number) => void;
  mutatingFlag?: string | null;
}) => {
  const overrideCount = Object.keys(segment.overrides).length;

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
      <div className="flex items-center justify-between px-5 py-3 border-b border-[#e2e6f0] dark:border-[#2a2e3f] bg-[#f8f9fc] dark:bg-[#0f1120]">
        <div className="flex items-center gap-2.5 flex-wrap">
          <Layers className="h-4 w-4 text-[#5c7cfa]" />
          <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            {segment.segmentName}
          </span>
          {overrideCount > 0 && (
            <span className="text-xs font-medium text-amber-600 dark:text-amber-400">
              {overrideCount} override{overrideCount !== 1 ? "s" : ""}
            </span>
          )}
        </div>
        {segment.description && (
          <span className="text-xs text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0 ml-3 max-w-xs truncate">
            {segment.description}
          </span>
        )}
      </div>
      <SegmentFlagTable
        overrides={segment.overrides}
        globalFlags={globalFlags}
        showOverrideOnly={showOverrideOnly}
        segmentId={segment.segmentId}
        onUpsertOverride={onUpsertOverride}
        onDeleteOverride={onDeleteOverride}
        mutatingFlag={mutatingFlag}
      />
    </div>
  );
};

// --- Main export: SegmentFeatureFlags ---

export const SegmentFeatureFlags = () => {
  const [showOverrideOnly, setShowOverrideOnly] = useState(false);
  const [showBanner, setShowBanner] = useState(false);
  const [mutatingFlag, setMutatingFlag] = useState<string | null>(null);

  const {
    data: globalFlags,
    isFetching: globalFetching,
    refetch: refetchGlobal,
  } = useGlobalFeatureFlags();

  const {
    data: segments,
    isLoading: segmentsLoading,
    isFetching: segmentsFetching,
    refetch: refetchSegments,
  } = useSegmentFeatureFlags();

  const upsertOverride = useUpsertSegmentOverride();
  const deleteOverride = useDeleteSegmentOverride();

  const handleUpsertOverride = useCallback(
    (
      flagName: string,
      segmentId: number,
      enabled: boolean,
      value: string | null,
    ) => {
      setMutatingFlag(flagName);
      upsertOverride.mutate(
        { segmentId, flagName, data: { enabled, value } },
        {
          onSettled: () => setMutatingFlag(null),
          onSuccess: () => setShowBanner(true),
        },
      );
    },
    [upsertOverride],
  );

  const handleDeleteOverride = useCallback(
    (flagName: string, segmentId: number) => {
      setMutatingFlag(flagName);
      deleteOverride.mutate(
        { segmentId, flagName },
        {
          onSettled: () => setMutatingFlag(null),
          onSuccess: () => setShowBanner(true),
        },
      );
    },
    [deleteOverride],
  );

  const totalOverrides = segments
    ? segments.reduce(
        (total, seg) => total + Object.keys(seg.overrides).length,
        0,
      )
    : 0;

  return (
    <div className="space-y-4">
      <PropagationBanner
        visible={showBanner}
        onDismiss={() => setShowBanner(false)}
      />

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <Layers className="h-4 w-4 text-[#5c7cfa]" />
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            Segment Overrides
          </h2>
          <span className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
            {segments?.length ?? 0} segment
            {(segments?.length ?? 0) !== 1 ? "s" : ""}
          </span>
          {totalOverrides > 0 && (
            <span className="text-xs font-medium text-amber-600 dark:text-amber-400">
              {totalOverrides} override{totalOverrides !== 1 ? "s" : ""} total
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
            Diff only
          </button>
          <RefreshButton
            onClick={() => {
              refetchGlobal();
              refetchSegments();
            }}
            isRefreshing={globalFetching || segmentsFetching}
          />
        </div>
      </div>

      {segmentsLoading ? (
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
          <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180] text-sm">
            Loading segments...
          </div>
        </div>
      ) : segments && segments.length > 0 ? (
        <div className="space-y-4">
          {segments.map((segment) => (
            <SegmentFlagSection
              key={segment.segmentId}
              segment={segment}
              globalFlags={globalFlags}
              showOverrideOnly={showOverrideOnly}
              onUpsertOverride={handleUpsertOverride}
              onDeleteOverride={handleDeleteOverride}
              mutatingFlag={mutatingFlag}
            />
          ))}
        </div>
      ) : (
        <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
          <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180]">
            <Layers className="h-10 w-10 mx-auto mb-3 opacity-40" />
            <p className="text-sm">No segments configured in Flagsmith</p>
          </div>
        </div>
      )}
    </div>
  );
};

/** Exported for use by FeatureFlagsPage global section */
export { GlobalFlagTable, PropagationBanner };
export type { FlagMap };
