import { useState, useMemo, Fragment } from "react";
import {
  Plus,
  Pencil,
  Trash2,
  Search,
  X,
  Info,
  AlertTriangle,
  AlertCircle,
} from "lucide-react";
import { RefreshButton, ConfirmDialog, Button } from "@buurman/ui";
import { format, isPast, isFuture, parseISO } from "date-fns";
import {
  useBroadcasts,
  useCreateBroadcast,
  useUpdateBroadcast,
  useDeleteBroadcast,
} from "../hooks/useBroadcasts";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { RichTextEditor } from "../components/RichTextEditor";
import { RichTextDisplay } from "../components/RichTextDisplay";
import { TargetTeamSelector, TargetUserSelector } from "../components/TargetSelector";
import type {
  BroadcastMessage,
  BroadcastSeverity,
  BroadcastScope,
  CreateBroadcastMessageRequest,
} from "../types";

const TH_CLASS =
  "text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-[#6b7194] dark:text-[#8b90a8]";

const SEVERITY_CONFIG: Record<
  BroadcastSeverity,
  { label: string; icon: React.ReactNode; classes: string }
> = {
  INFO: {
    label: "Info",
    icon: <Info className="h-3.5 w-3.5" />,
    classes:
      "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
  },
  WARNING: {
    label: "Warning",
    icon: <AlertTriangle className="h-3.5 w-3.5" />,
    classes:
      "bg-amber-50 text-amber-700 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700",
  },
  CRITICAL: {
    label: "Critical",
    icon: <AlertCircle className="h-3.5 w-3.5" />,
    classes:
      "bg-red-50 text-red-700 ring-1 ring-red-200 dark:bg-red-900/30 dark:text-red-300 dark:ring-red-700",
  },
};

type BroadcastStatus = "active" | "scheduled" | "expired";

const getStatus = (msg: BroadcastMessage): BroadcastStatus => {
  const start = parseISO(msg.startAt);
  if (isFuture(start)) {
    return "scheduled";
  }
  if (msg.endAt && isPast(parseISO(msg.endAt))) {
    return "expired";
  }
  return "active";
};

const STATUS_STYLES: Record<BroadcastStatus, string> = {
  active:
    "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-300 dark:ring-emerald-700",
  scheduled:
    "bg-blue-50 text-blue-700 ring-1 ring-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:ring-blue-700",
  expired:
    "bg-gray-50 text-gray-500 ring-1 ring-gray-200 dark:bg-gray-800/30 dark:text-gray-400 dark:ring-gray-600",
};

interface FormData {
  title: string;
  body: string;
  severity: BroadcastSeverity;
  scope: BroadcastScope;
  startAt: string;
  endAt: string;
  showOnLogin: boolean;
  showOnRegister: boolean;
  showInApp: boolean;
  targetTeamIdentifiers: string[];
  targetUserIdentifiers: string[];
}

const emptyForm: FormData = {
  title: "",
  body: "",
  severity: "INFO",
  scope: "GLOBAL",
  startAt: "",
  endAt: "",
  showOnLogin: false,
  showOnRegister: false,
  showInApp: true,
  targetTeamIdentifiers: [],
  targetUserIdentifiers: [],
};

const toLocalDatetime = (iso: string): string => {
  try {
    return format(parseISO(iso), "yyyy-MM-dd'T'HH:mm");
  } catch {
    return "";
  }
};

export const BroadcastsPage = () => {
  const { data, isLoading, isFetching, error, refetch } = useBroadcasts();
  const createBroadcast = useCreateBroadcast();
  const updateBroadcast = useUpdateBroadcast();
  const deleteBroadcast = useDeleteBroadcast();

  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingBroadcast, setEditingBroadcast] =
    useState<BroadcastMessage | null>(null);
  const [form, setForm] = useState<FormData>(emptyForm);
  const [deleteTarget, setDeleteTarget] = useState<BroadcastMessage | null>(
    null,
  );
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const broadcasts = useMemo(() => {
    const all = data ?? [];
    if (!search.trim()) {
      return all;
    }
    const q = search.toLowerCase();
    return all.filter(
      (b) =>
        b.title.toLowerCase().includes(q) ||
        b.body.toLowerCase().includes(q) ||
        b.severity.toLowerCase().includes(q),
    );
  }, [data, search]);

  const openCreate = () => {
    setEditingBroadcast(null);
    setForm({
      ...emptyForm,
      startAt: format(new Date(), "yyyy-MM-dd'T'HH:mm"),
    });
    setShowForm(true);
  };

  const openEdit = (msg: BroadcastMessage) => {
    setEditingBroadcast(msg);
    setForm({
      title: msg.title,
      body: msg.body,
      severity: msg.severity,
      scope: msg.scope,
      startAt: toLocalDatetime(msg.startAt),
      endAt: msg.endAt ? toLocalDatetime(msg.endAt) : "",
      showOnLogin: msg.showOnLogin,
      showOnRegister: msg.showOnRegister,
      showInApp: msg.showInApp,
      targetTeamIdentifiers: msg.targetTeamIdentifiers ?? [],
      targetUserIdentifiers: msg.targetUserIdentifiers ?? [],
    });
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingBroadcast(null);
    setForm(emptyForm);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const payload: CreateBroadcastMessageRequest = {
      title: form.title,
      body: form.body,
      severity: form.severity,
      scope: form.scope,
      startAt: new Date(form.startAt).toISOString(),
      endAt: form.endAt ? new Date(form.endAt).toISOString() : undefined,
      showOnLogin: form.showOnLogin,
      showOnRegister: form.showOnRegister,
      showInApp: form.showInApp,
      targetTeamIdentifiers:
        form.scope === "TEAMS" ? form.targetTeamIdentifiers : undefined,
      targetUserIdentifiers:
        form.scope === "USERS" ? form.targetUserIdentifiers : undefined,
    };

    if (editingBroadcast) {
      updateBroadcast.mutate(
        { identifier: editingBroadcast.identifier, data: payload },
        { onSuccess: closeForm },
      );
    } else {
      createBroadcast.mutate(payload, { onSuccess: closeForm });
    }
  };

  const handleDelete = () => {
    if (!deleteTarget) {
      return;
    }
    deleteBroadcast.mutate(deleteTarget.identifier, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  const isSaving = createBroadcast.isPending || updateBroadcast.isPending;

  if (isLoading) {
    return <LoadingSpinner message="Loading broadcasts..." />;
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 dark:text-red-400">
          Failed to load broadcasts.
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
            Broadcasts
          </h1>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Manage system-wide broadcast messages shown to users on login,
            registration, and in-app.
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
            New Broadcast
          </Button>
        </div>
      </div>

      {/* Search */}
      <div className="mb-4 relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9ca0b8]" />
        <input
          type="search"
          placeholder="Search by title, body, or severity..."
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
                <th className={TH_CLASS}>Title</th>
                <th className={TH_CLASS}>Severity</th>
                <th className={TH_CLASS}>Scope</th>
                <th className={TH_CLASS}>Status</th>
                <th className={TH_CLASS}>Visibility</th>
                <th className={TH_CLASS}>Start</th>
                <th className={TH_CLASS}>End</th>
                <th className={`${TH_CLASS} text-right`}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {broadcasts.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-sm text-[#9ca0b8] dark:text-[#5c6180]"
                  >
                    No broadcasts found.
                  </td>
                </tr>
              ) : (
                broadcasts.map((msg) => {
                  const status = getStatus(msg);
                  const sev = SEVERITY_CONFIG[msg.severity];
                  const isExpanded = expandedId === msg.identifier;
                  return (
                    <Fragment key={msg.identifier}>
                      <tr
                        className="border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] transition-colors cursor-pointer"
                        onClick={() =>
                          setExpandedId(isExpanded ? null : msg.identifier)
                        }
                      >
                        <td className="px-4 py-3">
                          <span className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                            {msg.title}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${sev.classes}`}
                          >
                            {sev.icon}
                            {sev.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
                            {msg.scope === "GLOBAL"
                              ? "Global"
                              : msg.scope === "TEAMS"
                                ? `${msg.targetTeamIdentifiers?.length ?? 0} Team${(msg.targetTeamIdentifiers?.length ?? 0) !== 1 ? "s" : ""}`
                                : `${msg.targetUserIdentifiers?.length ?? 0} User${(msg.targetUserIdentifiers?.length ?? 0) !== 1 ? "s" : ""}`}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium capitalize ${STATUS_STYLES[status]}`}
                          >
                            {status}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1">
                            {msg.showOnLogin && (
                              <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 ring-1 ring-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:ring-violet-700">
                                Login
                              </span>
                            )}
                            {msg.showOnRegister && (
                              <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 ring-1 ring-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:ring-violet-700">
                                Register
                              </span>
                            )}
                            {msg.showInApp && (
                              <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 ring-1 ring-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:ring-violet-700">
                                In-App
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                            {format(parseISO(msg.startAt), "dd MMM yyyy HH:mm")}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                            {msg.endAt
                              ? format(parseISO(msg.endAt), "dd MMM yyyy HH:mm")
                              : "Never"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div
                            className="flex items-center justify-end gap-1"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <button
                              onClick={() => openEdit(msg)}
                              className="p-1.5 rounded-md text-[#6b7194] dark:text-[#8b90a8] hover:text-[#4263eb] dark:hover:text-[#91a7ff] hover:bg-[#5c7cfa]/10 transition-colors"
                              title="Edit"
                            >
                              <Pencil className="h-4 w-4" />
                            </button>
                            <button
                              onClick={() => setDeleteTarget(msg)}
                              className="p-1.5 rounded-md text-[#6b7194] dark:text-[#8b90a8] hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                              title="Delete"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                      {isExpanded && (
                        <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
                          <td
                            colSpan={8}
                            className="px-6 py-4 bg-[#f8f9fc] dark:bg-[#0c0d14]"
                          >
                            <RichTextDisplay
                              content={msg.body}
                              className="text-sm text-[#3d4463] dark:text-[#c4c8db] prose prose-sm dark:prose-invert max-w-none"
                            />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create/Edit Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black/40" onClick={closeForm} />
          <div className="relative bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <h2 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                {editingBroadcast ? "Edit Broadcast" : "New Broadcast"}
              </h2>
              <button
                onClick={closeForm}
                className="p-1 rounded-md text-[#6b7194] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {/* Title */}
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Title
                </label>
                <input
                  type="text"
                  required
                  maxLength={200}
                  value={form.title}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, title: e.target.value }))
                  }
                  className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                  placeholder="Scheduled Maintenance"
                />
              </div>

              {/* Body */}
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Body
                </label>
                <RichTextEditor
                  value={form.body}
                  onChange={(val) => setForm((f) => ({ ...f, body: val }))}
                  placeholder="We will be performing maintenance on..."
                />
              </div>

              {/* Severity */}
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Severity
                </label>
                <select
                  value={form.severity}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      severity: e.target.value as BroadcastSeverity,
                    }))
                  }
                  className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                >
                  <option value="INFO">Info</option>
                  <option value="WARNING">Warning</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>

              {/* Scope */}
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Scope
                </label>
                <select
                  value={form.scope}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      scope: e.target.value as BroadcastScope,
                      targetTeamIdentifiers:
                        e.target.value !== "TEAMS"
                          ? []
                          : f.targetTeamIdentifiers,
                      targetUserIdentifiers:
                        e.target.value !== "USERS"
                          ? []
                          : f.targetUserIdentifiers,
                      showOnLogin:
                        e.target.value !== "GLOBAL" ? false : f.showOnLogin,
                      showOnRegister:
                        e.target.value !== "GLOBAL" ? false : f.showOnRegister,
                    }))
                  }
                  className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                >
                  <option value="GLOBAL">Global (all users)</option>
                  <option value="TEAMS">Specific Teams</option>
                  <option value="USERS">Specific Users</option>
                </select>
              </div>

              {/* Target Selectors */}
              {form.scope === "TEAMS" && (
                <TargetTeamSelector
                  selected={form.targetTeamIdentifiers}
                  onChange={(ids) =>
                    setForm((f) => ({ ...f, targetTeamIdentifiers: ids }))
                  }
                />
              )}
              {form.scope === "USERS" && (
                <TargetUserSelector
                  selected={form.targetUserIdentifiers}
                  onChange={(ids) =>
                    setForm((f) => ({ ...f, targetUserIdentifiers: ids }))
                  }
                />
              )}

              {/* Start / End */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                    Start At
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={form.startAt}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, startAt: e.target.value }))
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                    End At
                    <span className="text-[#9ca0b8] font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="datetime-local"
                    value={form.endAt}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, endAt: e.target.value }))
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors"
                  />
                </div>
              </div>

              {/* Visibility toggles */}
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-2">
                  Visibility
                </label>
                <div className="flex flex-wrap gap-4">
                  <label
                    className={`flex items-center gap-2 text-sm ${form.scope !== "GLOBAL" ? "text-[#9ca0b8] dark:text-[#5c6180] cursor-not-allowed" : "text-[#3d4463] dark:text-[#c4c8db] cursor-pointer"}`}
                    title={form.scope !== "GLOBAL" ? "Only available for Global scope" : undefined}
                  >
                    <input
                      type="checkbox"
                      checked={form.showOnLogin}
                      disabled={form.scope !== "GLOBAL"}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          showOnLogin: e.target.checked,
                        }))
                      }
                      className="rounded border-[#cdd3e6] text-[#5c7cfa] focus:ring-[#5c7cfa]/20 disabled:opacity-40"
                    />
                    Login Page
                  </label>
                  <label
                    className={`flex items-center gap-2 text-sm ${form.scope !== "GLOBAL" ? "text-[#9ca0b8] dark:text-[#5c6180] cursor-not-allowed" : "text-[#3d4463] dark:text-[#c4c8db] cursor-pointer"}`}
                    title={form.scope !== "GLOBAL" ? "Only available for Global scope" : undefined}
                  >
                    <input
                      type="checkbox"
                      checked={form.showOnRegister}
                      disabled={form.scope !== "GLOBAL"}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          showOnRegister: e.target.checked,
                        }))
                      }
                      className="rounded border-[#cdd3e6] text-[#5c7cfa] focus:ring-[#5c7cfa]/20 disabled:opacity-40"
                    />
                    Register Page
                  </label>
                  <label className="flex items-center gap-2 text-sm text-[#3d4463] dark:text-[#c4c8db] cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.showInApp}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          showInApp: e.target.checked,
                        }))
                      }
                      className="rounded border-[#cdd3e6] text-[#5c7cfa] focus:ring-[#5c7cfa]/20"
                    />
                    In-App
                  </label>
                </div>
              </div>

              {/* Actions */}
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
                  {editingBroadcast ? "Save Changes" : "Create Broadcast"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete Broadcast"
          message={`Are you sure you want to delete "${deleteTarget.title}"? This action cannot be undone.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          isLoading={deleteBroadcast.isPending}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
};
