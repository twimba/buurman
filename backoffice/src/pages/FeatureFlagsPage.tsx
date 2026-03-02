import { useState, useEffect, useRef, useCallback } from "react";
import { Search, Flag, User, XCircle, AlertTriangle } from "lucide-react";
import { AxiosError } from "axios";
import { RefreshButton } from "@buurman/ui";
import {
  useAdminStatus,
  useGlobalFeatureFlags,
  useUpdateGlobalFlag,
} from "../hooks/useFeatureFlags";
import { useUsers } from "../hooks/useUsers";
import {
  UserFeatureFlags,
  GlobalFlagTable,
  PropagationBanner,
  SegmentFeatureFlags,
} from "../components/UserFeatureFlags";

function useDebouncedValue<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(id);
  }, [value, delay]);
  return debounced;
}

export const FeatureFlagsPage = () => {
  const [selectedUser, setSelectedUser] = useState<string | null>(null);
  const [selectedUserLabel, setSelectedUserLabel] = useState("");
  const [inputValue, setInputValue] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);
  const [showBanner, setShowBanner] = useState(false);
  const [mutatingFlag, setMutatingFlag] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const blurTimeoutRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  const debouncedSearch = useDebouncedValue(inputValue, 300);

  const { data: adminStatus } = useAdminStatus();
  const adminConfigured = adminStatus?.adminConfigured ?? true;

  const {
    data: globalFlags,
    isLoading: globalLoading,
    isFetching: globalFetching,
    refetch: refetchGlobal,
  } = useGlobalFeatureFlags();

  const updateGlobalFlag = useUpdateGlobalFlag();

  const { data: usersData } = useUsers({
    search: debouncedSearch || undefined,
    size: 10,
    sort: "email",
    direction: "ASC",
  });

  const globalFlagCount = globalFlags ? Object.keys(globalFlags).length : 0;

  const extractError = (error: unknown): string => {
    if (error instanceof AxiosError && error.response?.data) {
      const data = error.response.data;
      return (
        data.detail ||
        data.message ||
        data.title ||
        "Failed to update feature flag"
      );
    }
    if (error instanceof Error) {
      return error.message;
    }
    return "Failed to update feature flag";
  };

  const handleToggle = useCallback(
    (flagName: string, enabled: boolean) => {
      setErrorMessage(null);
      setMutatingFlag(flagName);
      updateGlobalFlag.mutate(
        { flagName, data: { enabled } },
        {
          onSettled: () => setMutatingFlag(null),
          onSuccess: () => setShowBanner(true),
          onError: (error) => setErrorMessage(extractError(error)),
        },
      );
    },
    [updateGlobalFlag],
  );

  const handleValueChange = useCallback(
    (flagName: string, value: string | null) => {
      setErrorMessage(null);
      setMutatingFlag(flagName);
      updateGlobalFlag.mutate(
        { flagName, data: { value: value ?? undefined } },
        {
          onSettled: () => setMutatingFlag(null),
          onSuccess: () => setShowBanner(true),
          onError: (error) => setErrorMessage(extractError(error)),
        },
      );
    },
    [updateGlobalFlag],
  );

  const handleFocus = () => {
    if (!selectedUser) {
      setShowDropdown(true);
    }
  };

  const handleBlur = () => {
    blurTimeoutRef.current = setTimeout(() => setShowDropdown(false), 200);
  };

  const handleSelect = (identifier: string, label: string) => {
    if (blurTimeoutRef.current) {
      clearTimeout(blurTimeoutRef.current);
    }
    setSelectedUser(identifier);
    setSelectedUserLabel(label);
    setInputValue("");
    setShowDropdown(false);
  };

  const handleClear = () => {
    setSelectedUser(null);
    setSelectedUserLabel("");
    setInputValue("");
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
          Feature Flags
        </h1>
        <p className="mt-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
          Manage feature flag status across the platform
        </p>
      </div>

      {/* Admin not configured warning */}
      {!adminConfigured && (
        <div className="flex items-start gap-3 p-4 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/50 rounded-xl">
          <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
              Feature flag management unavailable
            </p>
            <p className="text-sm text-amber-700 dark:text-amber-400 mt-1">
              No Flagsmith admin credentials configured. Set{" "}
              <code className="text-xs bg-amber-100 dark:bg-amber-900/40 px-1 py-0.5 rounded">
                FLAGSMITH_API_TOKEN
              </code>{" "}
              (Cloud) or{" "}
              <code className="text-xs bg-amber-100 dark:bg-amber-900/40 px-1 py-0.5 rounded">
                FLAGSMITH_ADMIN_EMAIL
              </code>{" "}
              +{" "}
              <code className="text-xs bg-amber-100 dark:bg-amber-900/40 px-1 py-0.5 rounded">
                FLAGSMITH_ADMIN_PASSWORD
              </code>{" "}
              (self-hosted).
            </p>
          </div>
        </div>
      )}

      {/* Error banner */}
      {errorMessage && (
        <div className="flex items-start gap-3 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/50 rounded-xl">
          <XCircle className="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm text-red-800 dark:text-red-300">
              {errorMessage}
            </p>
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-red-400 hover:text-red-600 dark:hover:text-red-300"
          >
            <XCircle className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Global Flags */}
      <section className="space-y-3">
        <PropagationBanner
          visible={showBanner}
          onDismiss={() => setShowBanner(false)}
        />

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Flag className="h-4 w-4 text-[#5c7cfa]" />
            <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              Global Defaults
            </h2>
            <span className="text-xs text-[#9ca0b8] dark:text-[#5c6180]">
              {globalFlagCount} flag{globalFlagCount !== 1 ? "s" : ""}
            </span>
          </div>
          <RefreshButton
            onClick={() => refetchGlobal()}
            isRefreshing={globalFetching}
          />
        </div>
        <div
          className={`bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden${!adminConfigured ? " opacity-60 pointer-events-none" : ""}`}
        >
          {globalLoading ? (
            <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180] text-sm">
              Loading flags...
            </div>
          ) : (
            <GlobalFlagTable
              flags={globalFlags ?? {}}
              onToggle={handleToggle}
              onValueChange={handleValueChange}
              mutatingFlag={mutatingFlag}
            />
          )}
        </div>
      </section>

      {/* User-specific Flags */}
      <section>
        <div className="flex items-center gap-2.5 mb-3">
          <User className="h-4 w-4 text-[#5c7cfa]" />
          <h2 className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
            User-Specific Evaluation
          </h2>
        </div>

        {/* User search */}
        <div className="relative mb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              placeholder="Search by email, name, or identifier..."
              value={selectedUser ? selectedUserLabel : inputValue}
              onChange={(e) => {
                setInputValue(e.target.value);
                setSelectedUser(null);
                setSelectedUserLabel("");
                setShowDropdown(true);
              }}
              onFocus={handleFocus}
              onBlur={handleBlur}
              className="w-full pl-10 pr-10 py-2.5 rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] text-sm text-[#1a1d2e] dark:text-[#eef0f6] placeholder:text-[#9ca0b8] dark:placeholder:text-[#5c6180] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa]/30 focus:border-[#5c7cfa] transition-all"
            />
            {selectedUser && (
              <button
                onClick={handleClear}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9ca0b8] hover:text-[#6b7194] dark:hover:text-[#8b90a8] transition-colors"
              >
                <XCircle className="h-4 w-4" />
              </button>
            )}
          </div>

          {/* Dropdown */}
          {showDropdown && !selectedUser && (
            <div className="absolute z-20 mt-1 w-full bg-white dark:bg-[#14161f] border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-xl shadow-lg overflow-hidden max-h-80 overflow-y-auto">
              {usersData?.content && usersData.content.length > 0 ? (
                usersData.content.map((user) => (
                  <button
                    key={user.identifier}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() =>
                      handleSelect(
                        user.identifier,
                        `${user.email} — ${user.firstName} ${user.lastName}`,
                      )
                    }
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-left hover:bg-[#f1f3f9] dark:hover:bg-[#1a1d2e] transition-colors"
                  >
                    <div className="h-7 w-7 rounded-full bg-[#5c7cfa]/10 flex items-center justify-center flex-shrink-0">
                      <span className="text-xs font-semibold text-[#5c7cfa]">
                        {(user.firstName?.[0] ?? user.email[0]).toUpperCase()}
                      </span>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                        {user.email}
                      </p>
                      <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] truncate">
                        {user.firstName} {user.lastName}
                      </p>
                    </div>
                    <span className="text-[10px] font-mono text-[#9ca0b8] dark:text-[#5c6180] flex-shrink-0">
                      {user.identifier.slice(0, 8)}...
                    </span>
                  </button>
                ))
              ) : (
                <div className="px-4 py-3 text-sm text-[#9ca0b8] dark:text-[#5c6180]">
                  No users found
                </div>
              )}
            </div>
          )}
        </div>

        {/* User flag evaluations */}
        {selectedUser ? (
          <UserFeatureFlags userIdentifier={selectedUser} />
        ) : (
          <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
            <div className="text-center py-12 text-[#9ca0b8] dark:text-[#5c6180]">
              <User className="h-10 w-10 mx-auto mb-3 opacity-40" />
              <p className="text-sm">
                Select a user to see their feature flag evaluation per team
              </p>
            </div>
          </div>
        )}
      </section>

      {/* Segment Overrides */}
      <section>
        <SegmentFeatureFlags />
      </section>
    </div>
  );
};
