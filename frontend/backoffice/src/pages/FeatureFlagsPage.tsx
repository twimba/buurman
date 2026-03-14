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
        <h1 className="text-2xl font-bold text-text-primary">Feature Flags</h1>
        <p className="mt-1 text-sm text-text-secondary">
          Manage feature flag status across the platform
        </p>
      </div>

      {/* Admin not configured warning */}
      {!adminConfigured && (
        <div className="flex items-start gap-3 p-4 bg-warning-bg border border-warning-border rounded-lg">
          <AlertTriangle className="h-5 w-5 text-warning-text flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-warning-text">
              Feature flag management unavailable
            </p>
            <p className="text-sm text-warning-text mt-1">
              No Flagsmith admin credentials configured. Set{" "}
              <code className="text-xs bg-warning-bg px-1 py-0.5 rounded">
                FLAGSMITH_API_TOKEN
              </code>{" "}
              (Cloud) or{" "}
              <code className="text-xs bg-warning-bg px-1 py-0.5 rounded">
                FLAGSMITH_ADMIN_EMAIL
              </code>{" "}
              +{" "}
              <code className="text-xs bg-warning-bg px-1 py-0.5 rounded">
                FLAGSMITH_ADMIN_PASSWORD
              </code>{" "}
              (self-hosted).
            </p>
          </div>
        </div>
      )}

      {/* Error banner */}
      {errorMessage && (
        <div className="flex items-start gap-3 p-4 bg-error-bg border border-error-border rounded-lg">
          <XCircle className="h-5 w-5 text-error-text flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm text-error-text">{errorMessage}</p>
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-error-text hover:text-error-text"
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
            <Flag className="h-4 w-4 text-primary-500" />
            <h2 className="text-sm font-semibold text-text-primary">
              Global Defaults
            </h2>
            <span className="text-xs text-text-muted">
              {globalFlagCount} flag{globalFlagCount !== 1 ? "s" : ""}
            </span>
          </div>
          <RefreshButton
            onClick={() => refetchGlobal()}
            isRefreshing={globalFetching}
          />
        </div>
        <div
          className={`bg-surface-card rounded-lg border border-border-default overflow-hidden${!adminConfigured ? " opacity-60 pointer-events-none" : ""}`}
        >
          {globalLoading ? (
            <div className="text-center py-12 text-text-muted text-sm">
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
          <User className="h-4 w-4 text-primary-500" />
          <h2 className="text-sm font-semibold text-text-primary">
            User-Specific Evaluation
          </h2>
        </div>

        {/* User search */}
        <div className="relative mb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted " />
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
              className="w-full pl-10 pr-10 py-2.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
            {selectedUser && (
              <button
                onClick={handleClear}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
              >
                <XCircle className="h-4 w-4" />
              </button>
            )}
          </div>

          {/* Dropdown */}
          {showDropdown && !selectedUser && (
            <div className="absolute z-20 mt-1 w-full bg-surface-card border border-border-default rounded-lg shadow-lg overflow-hidden max-h-80 overflow-y-auto">
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
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-left hover:bg-surface-inset transition-colors"
                  >
                    <div className="h-7 w-7 rounded-full bg-primary-500/10 flex items-center justify-center flex-shrink-0">
                      <span className="text-xs font-semibold text-primary-500">
                        {(user.firstName?.[0] ?? user.email[0]).toUpperCase()}
                      </span>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-text-primary truncate">
                        {user.email}
                      </p>
                      <p className="text-xs text-text-secondary truncate">
                        {user.firstName} {user.lastName}
                      </p>
                    </div>
                    <span className="text-[10px] font-mono text-text-muted flex-shrink-0">
                      {user.identifier.slice(0, 8)}...
                    </span>
                  </button>
                ))
              ) : (
                <div className="px-4 py-3 text-sm text-text-muted">
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
          <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
            <div className="text-center py-12 text-text-muted">
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
