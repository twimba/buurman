import { useState, useMemo, Fragment } from 'react';
import {
  Plus,
  Pencil,
  Trash2,
  Search,
  X,
  Info,
  AlertTriangle,
  AlertCircle,
} from 'lucide-react';
import {
  RefreshButton,
  ConfirmDialog,
  Button,
  RichTextEditor,
  RichTextDisplay,
} from '@buurman/ui';
import { format, isPast, isFuture, parseISO } from 'date-fns';
import {
  useBroadcasts,
  useCreateBroadcast,
  useUpdateBroadcast,
  useDeleteBroadcast,
} from '../hooks/useBroadcasts';
import { LoadingSpinner } from '../components/LoadingSpinner';
import {
  TargetTeamSelector,
  TargetUserSelector,
} from '../components/TargetSelector';
import type {
  BroadcastMessage,
  BroadcastSeverity,
  BroadcastScope,
  CreateBroadcastMessageRequest,
} from '../types';

const TH_CLASS =
  'text-left px-4 py-3 text-xs font-semibold uppercase tracking-wider text-text-secondary';

const SEVERITY_CONFIG: Record<
  BroadcastSeverity,
  { label: string; icon: React.ReactNode; classes: string }
> = {
  INFO: {
    label: 'Info',
    icon: <Info className="h-3.5 w-3.5" />,
    classes: 'bg-info-bg text-info-text ring-1 ring-info-border',
  },
  WARNING: {
    label: 'Warning',
    icon: <AlertTriangle className="h-3.5 w-3.5" />,
    classes: 'bg-warning-bg text-warning-text ring-1 ring-warning-border',
  },
  CRITICAL: {
    label: 'Critical',
    icon: <AlertCircle className="h-3.5 w-3.5" />,
    classes: 'bg-error-bg text-error-text ring-1 ring-error-border',
  },
};

type BroadcastStatus = 'active' | 'scheduled' | 'expired';

const getStatus = (msg: BroadcastMessage): BroadcastStatus => {
  const start = parseISO(msg.startAt);
  if (isFuture(start)) {
    return 'scheduled';
  }
  if (msg.endAt && isPast(parseISO(msg.endAt))) {
    return 'expired';
  }
  return 'active';
};

const STATUS_STYLES: Record<BroadcastStatus, string> = {
  active: 'bg-success-bg text-success-text ring-1 ring-success-border',
  scheduled: 'bg-info-bg text-info-text ring-1 ring-info-border',
  expired: 'bg-gray-50 text-gray-500 ring-1 ring-gray-200',
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
  title: '',
  body: '',
  severity: 'INFO',
  scope: 'GLOBAL',
  startAt: '',
  endAt: '',
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
    return '';
  }
};

export const BroadcastsPage = () => {
  const { data, isLoading, isFetching, error, refetch } = useBroadcasts();
  const createBroadcast = useCreateBroadcast();
  const updateBroadcast = useUpdateBroadcast();
  const deleteBroadcast = useDeleteBroadcast();

  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingBroadcast, setEditingBroadcast] =
    useState<BroadcastMessage | null>(null);
  const [form, setForm] = useState<FormData>(emptyForm);
  const [deleteTarget, setDeleteTarget] = useState<BroadcastMessage | null>(
    null
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
        b.severity.toLowerCase().includes(q)
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
      endAt: msg.endAt ? toLocalDatetime(msg.endAt) : '',
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
        form.scope === 'TEAMS' ? form.targetTeamIdentifiers : undefined,
      targetUserIdentifiers:
        form.scope === 'USERS' ? form.targetUserIdentifiers : undefined,
    };

    if (editingBroadcast) {
      updateBroadcast.mutate(
        { identifier: editingBroadcast.identifier, data: payload },
        { onSuccess: closeForm }
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
        <p className="text-error-text">Failed to load broadcasts.</p>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div
        className="mb-6"
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Broadcasts</h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage system-wide broadcast messages shown to users on login,
            registration, and in-app.
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
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
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted" />
        <input
          type="search"
          placeholder="Search by title, body, or severity..."
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
                    className="px-4 py-12 text-center text-sm text-text-muted"
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
                        className="border-b border-border-default last:border-b-0 hover:bg-surface-page transition-colors cursor-pointer"
                        onClick={() =>
                          setExpandedId(isExpanded ? null : msg.identifier)
                        }
                      >
                        <td className="px-4 py-3">
                          <span className="text-sm font-medium text-text-primary">
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
                          <span className="text-sm text-text-secondary">
                            {msg.scope === 'GLOBAL'
                              ? 'Global'
                              : msg.scope === 'TEAMS'
                                ? `${msg.targetTeamIdentifiers?.length ?? 0} Team${(msg.targetTeamIdentifiers?.length ?? 0) !== 1 ? 's' : ''}`
                                : `${msg.targetUserIdentifiers?.length ?? 0} User${(msg.targetUserIdentifiers?.length ?? 0) !== 1 ? 's' : ''}`}
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
                              <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 ring-1 ring-violet-200">
                                Login
                              </span>
                            )}
                            {msg.showOnRegister && (
                              <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 ring-1 ring-violet-200">
                                Register
                              </span>
                            )}
                            {msg.showInApp && (
                              <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-violet-50 text-violet-700 ring-1 ring-violet-200">
                                In-App
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-text-secondary">
                            {format(parseISO(msg.startAt), 'dd MMM yyyy HH:mm')}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-text-secondary">
                            {msg.endAt
                              ? format(parseISO(msg.endAt), 'dd MMM yyyy HH:mm')
                              : 'Never'}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div
                            className="flex items-center justify-end gap-1"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <button
                              onClick={() => openEdit(msg)}
                              className="p-1.5 rounded-md text-text-secondary hover:text-primary-600 hover:bg-primary-500/10 transition-colors"
                              title="Edit"
                            >
                              <Pencil className="h-4 w-4" />
                            </button>
                            <button
                              onClick={() => setDeleteTarget(msg)}
                              className="p-1.5 rounded-md text-text-secondary hover:text-error-text hover:bg-error-bg transition-colors"
                              title="Delete"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                      {isExpanded && (
                        <tr className="border-b border-border-default">
                          <td colSpan={8} className="px-6 py-4 bg-surface-page">
                            <RichTextDisplay
                              content={msg.body}
                              className="text-sm text-text-secondary prose prose-sm max-w-none"
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
          <div className="relative bg-surface-card rounded-lg border border-border-default shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
              <h2 className="text-lg font-semibold text-text-primary">
                {editingBroadcast ? 'Edit Broadcast' : 'New Broadcast'}
              </h2>
              <button
                onClick={closeForm}
                className="p-1 rounded-md text-text-secondary hover:text-text-primary hover:bg-surface-inset transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {/* Title */}
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
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
                  className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                  placeholder="Scheduled Maintenance"
                />
              </div>

              {/* Body */}
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
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
                <label className="block text-sm font-medium text-text-secondary mb-1">
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
                  className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                >
                  <option value="INFO">Info</option>
                  <option value="WARNING">Warning</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>

              {/* Scope */}
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-1">
                  Scope
                </label>
                <select
                  value={form.scope}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      scope: e.target.value as BroadcastScope,
                      targetTeamIdentifiers:
                        e.target.value !== 'TEAMS'
                          ? []
                          : f.targetTeamIdentifiers,
                      targetUserIdentifiers:
                        e.target.value !== 'USERS'
                          ? []
                          : f.targetUserIdentifiers,
                      showOnLogin:
                        e.target.value !== 'GLOBAL' ? false : f.showOnLogin,
                      showOnRegister:
                        e.target.value !== 'GLOBAL' ? false : f.showOnRegister,
                    }))
                  }
                  className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                >
                  <option value="GLOBAL">Global (all users)</option>
                  <option value="TEAMS">Specific Teams</option>
                  <option value="USERS">Specific Users</option>
                </select>
              </div>

              {/* Target Selectors */}
              {form.scope === 'TEAMS' && (
                <TargetTeamSelector
                  selected={form.targetTeamIdentifiers}
                  onChange={(ids) =>
                    setForm((f) => ({ ...f, targetTeamIdentifiers: ids }))
                  }
                />
              )}
              {form.scope === 'USERS' && (
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
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    Start At
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={form.startAt}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, startAt: e.target.value }))
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-1">
                    End At
                    <span className="text-text-muted font-normal ml-1">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="datetime-local"
                    value={form.endAt}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, endAt: e.target.value }))
                    }
                    className="w-full px-3 py-2 text-sm rounded-lg border border-border-default bg-surface-card text-text-primary focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
                  />
                </div>
              </div>

              {/* Visibility toggles */}
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">
                  Visibility
                </label>
                <div className="flex flex-wrap gap-4">
                  <label
                    className={`flex items-center gap-2 text-sm ${form.scope !== 'GLOBAL' ? 'text-text-muted cursor-not-allowed' : 'text-text-secondary cursor-pointer'}`}
                    title={
                      form.scope !== 'GLOBAL'
                        ? 'Only available for Global scope'
                        : undefined
                    }
                  >
                    <input
                      type="checkbox"
                      checked={form.showOnLogin}
                      disabled={form.scope !== 'GLOBAL'}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          showOnLogin: e.target.checked,
                        }))
                      }
                      className="rounded border-border-default text-primary-500 focus:ring-primary-500/20 disabled:opacity-40"
                    />
                    Login Page
                  </label>
                  <label
                    className={`flex items-center gap-2 text-sm ${form.scope !== 'GLOBAL' ? 'text-text-muted cursor-not-allowed' : 'text-text-secondary cursor-pointer'}`}
                    title={
                      form.scope !== 'GLOBAL'
                        ? 'Only available for Global scope'
                        : undefined
                    }
                  >
                    <input
                      type="checkbox"
                      checked={form.showOnRegister}
                      disabled={form.scope !== 'GLOBAL'}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          showOnRegister: e.target.checked,
                        }))
                      }
                      className="rounded border-border-default text-primary-500 focus:ring-primary-500/20 disabled:opacity-40"
                    />
                    Register Page
                  </label>
                  <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.showInApp}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          showInApp: e.target.checked,
                        }))
                      }
                      className="rounded border-border-default text-primary-500 focus:ring-primary-500/20"
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
                  {editingBroadcast ? 'Save Changes' : 'Create Broadcast'}
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
