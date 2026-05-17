import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Save,
  Plus,
  Trash2,
  Loader2,
  Building2,
  Users,
  ChevronDown,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';
import {
  useSegmentDetail,
  useUpdateSegment,
  useSegmentMatches,
  useSegmentMatchingTeams,
  useSegmentMatchingUsers,
} from '../hooks/useSegments';
import { SegmentDetailFeatureFlags } from '../components/UserFeatureFlags';
import type { SegmentCondition } from '../api/segments';

const ATTRIBUTES = [
  { value: 'is_demo', label: 'Is Demo', type: 'boolean' },
  { value: 'is_owner', label: 'Is Owner', type: 'boolean' },
  { value: 'is_team', label: 'Is Team', type: 'boolean' },
  { value: 'is_user', label: 'Is User', type: 'boolean' },
  { value: 'role', label: 'Role', type: 'role' },
  { value: 'email', label: 'Email', type: 'string' },
  { value: 'email_verified', label: 'Email Verified', type: 'boolean' },
  { value: 'property_count', label: 'Property Count', type: 'number' },
  { value: 'member_count', label: 'Member Count', type: 'number' },
  { value: 'team_age_days', label: 'Team Age (days)', type: 'number' },
  { value: 'contract_count', label: 'Contract Count', type: 'number' },
  { value: 'contact_count', label: 'Contact Count', type: 'number' },
  { value: 'photo_count', label: 'Photo Count', type: 'number' },
  { value: 'document_count', label: 'Document Count', type: 'number' },
  { value: 'expense_count', label: 'Expense Count', type: 'number' },
  { value: 'payment_count', label: 'Payment Count', type: 'number' },
  {
    value: 'calendar_feed_count',
    label: 'Calendar Feed Count',
    type: 'number',
  },
  { value: 'team_name', label: 'Team Name', type: 'string' },
  { value: 'team_admin_email', label: 'Team Admin Email', type: 'string' },
  { value: 'team_owner_email', label: 'Team Owner Email', type: 'string' },
  { value: 'team_currency', label: 'Team Currency', type: 'string' },
  {
    value: 'team_default_country',
    label: 'Team Default Country (ISO-2)',
    type: 'string',
  },
  { value: 'team_timezone', label: 'Team Timezone', type: 'string' },
];

const OPERATORS = [
  { value: 'eq', label: 'equals' },
  { value: 'neq', label: 'not equals' },
  { value: 'in', label: 'in' },
  { value: 'not_in', label: 'not in' },
  { value: 'gt', label: '>' },
  { value: 'gte', label: '>=' },
  { value: 'lt', label: '<' },
  { value: 'lte', label: '<=' },
  { value: 'contains', label: 'contains' },
  { value: 'not_contains', label: 'does not contain' },
  { value: 'starts_with', label: 'starts with' },
  { value: 'ends_with', label: 'ends with' },
  { value: 'regex', label: 'matches regex' },
];

const ROLE_VALUES = ['TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER'];

export const SegmentDetailPage = () => {
  const { key } = useParams<{ key: string }>();
  const navigate = useNavigate();
  const { data: segment, isLoading } = useSegmentDetail(key);
  const { data: matches } = useSegmentMatches(key);
  const updateSegment = useUpdateSegment();

  const [lastSynced, setLastSynced] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState(0);
  const [conditions, setConditions] = useState<SegmentCondition[]>([]);
  const [dirty, setDirty] = useState(false);
  const [expandedList, setExpandedList] = useState<'teams' | 'users' | null>(
    null
  );
  const { data: matchingTeams, isLoading: teamsLoading } =
    useSegmentMatchingTeams(key, expandedList === 'teams');
  const { data: matchingUsers, isLoading: usersLoading } =
    useSegmentMatchingUsers(key, expandedList === 'users');

  // Sync form state when segment data loads (state-based tracking)
  if (segment && lastSynced !== segment.key) {
    setLastSynced(segment.key);
    setName(segment.name);
    setDescription(segment.description ?? '');
    setPriority(segment.priority);
    setConditions(segment.conditions.map((c) => ({ ...c })));
    setDirty(false);
  }

  const handleSave = () => {
    if (!key) {
      return;
    }
    updateSegment.mutate(
      {
        key,
        data: {
          name,
          description: description || undefined,
          priority,
          conditions,
        },
      },
      {
        onSuccess: () => setDirty(false),
      }
    );
  };

  const addCondition = () => {
    setConditions([
      ...conditions,
      { attribute: 'is_demo', operator: 'eq', value: 'true' },
    ]);
    setDirty(true);
  };

  const removeCondition = (index: number) => {
    setConditions(conditions.filter((_, i) => i !== index));
    setDirty(true);
  };

  const updateCondition = (
    index: number,
    field: keyof SegmentCondition,
    value: string
  ) => {
    const updated = [...conditions];
    updated[index] = { ...updated[index], [field]: value };

    // Reset value when changing attribute
    if (field === 'attribute') {
      const newType = ATTRIBUTES.find((a) => a.value === value)?.type;
      if (newType === 'boolean') {
        updated[index].value = 'true';
        updated[index].operator = 'eq';
      } else if (newType === 'role') {
        updated[index].value = 'TEAM_ADMIN';
        updated[index].operator = 'eq';
      } else if (newType === 'number') {
        updated[index].value = '0';
        updated[index].operator = 'gte';
      } else {
        // string
        updated[index].value = '';
        updated[index].operator = 'eq';
      }
    }

    setConditions(updated);
    setDirty(true);
  };

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto text-center py-12 text-text-muted text-sm">
        Loading segment...
      </div>
    );
  }

  if (!segment) {
    return (
      <div className="max-w-3xl mx-auto text-center py-12 text-text-muted text-sm">
        Segment not found
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/segments')}
            className="p-1.5 rounded-md text-text-muted hover:text-text-primary hover:bg-surface-inset transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-text-primary">
              {segment.name}
            </h1>
            <code className="text-xs font-mono text-text-muted">
              {segment.key}
            </code>
          </div>
        </div>
        <button
          onClick={handleSave}
          disabled={!dirty || updateSegment.isPending}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary-500 text-white text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {updateSegment.isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Save className="h-4 w-4" />
          )}
          Save
        </button>
      </div>

      {/* Metadata */}
      <section className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
        <h2 className="text-sm font-semibold text-text-primary">Details</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">
              Name <span className="text-error-text">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setDirty(true);
              }}
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">
              Priority
            </label>
            <input
              type="number"
              value={priority}
              onChange={(e) => {
                setPriority(parseInt(e.target.value) || 0);
                setDirty(true);
              }}
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
          </div>
        </div>
        <div>
          <label className="block text-xs font-medium text-text-secondary mb-1">
            Description
          </label>
          <input
            type="text"
            value={description}
            onChange={(e) => {
              setDescription(e.target.value);
              setDirty(true);
            }}
            className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
          />
        </div>
      </section>

      {/* Conditions */}
      <section className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-text-primary">
            Conditions
          </h2>
          <button
            onClick={addCondition}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-primary-600 hover:bg-primary-50 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" />
            Add Condition
          </button>
        </div>
        <p className="text-xs text-text-muted">
          All conditions must match (AND logic). For OR logic, create separate
          segments.
        </p>
        {conditions.length > 0 ? (
          <div className="space-y-3">
            {conditions.map((condition, index) => (
              <div key={index} className="space-y-2">
                <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-inset border border-border-default">
                  <select
                    value={condition.attribute}
                    onChange={(e) =>
                      updateCondition(index, 'attribute', e.target.value)
                    }
                    className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                  >
                    {ATTRIBUTES.map((a) => (
                      <option key={a.value} value={a.value}>
                        {a.label}
                      </option>
                    ))}
                  </select>
                  <select
                    value={condition.operator}
                    onChange={(e) =>
                      updateCondition(index, 'operator', e.target.value)
                    }
                    className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                  >
                    {OPERATORS.filter((op) => {
                      const attrType = ATTRIBUTES.find(
                        (a) => a.value === condition.attribute
                      )?.type;
                      if (attrType === 'boolean') {
                        return op.value === 'eq' || op.value === 'neq';
                      }
                      if (attrType === 'string') {
                        return [
                          'eq',
                          'neq',
                          'in',
                          'not_in',
                          'contains',
                          'not_contains',
                          'starts_with',
                          'ends_with',
                          'regex',
                        ].includes(op.value);
                      }
                      if (attrType === 'role') {
                        return ['eq', 'neq', 'in', 'not_in'].includes(op.value);
                      }
                      // number type: all except string operators
                      return ![
                        'contains',
                        'not_contains',
                        'starts_with',
                        'ends_with',
                      ].includes(op.value);
                    }).map((op) => (
                      <option key={op.value} value={op.value}>
                        {op.label}
                      </option>
                    ))}
                  </select>
                  {(() => {
                    const attrType = ATTRIBUTES.find(
                      (a) => a.value === condition.attribute
                    )?.type;
                    if (attrType === 'boolean') {
                      return (
                        <select
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, 'value', e.target.value)
                          }
                          className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        >
                          <option value="true">true</option>
                          <option value="false">false</option>
                        </select>
                      );
                    }
                    if (attrType === 'role') {
                      return condition.operator === 'in' ||
                        condition.operator === 'not_in' ? (
                        <input
                          type="text"
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, 'value', e.target.value)
                          }
                          placeholder="TEAM_ADMIN,TEAM_EDITOR"
                          className="flex-1 px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        />
                      ) : (
                        <select
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, 'value', e.target.value)
                          }
                          className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        >
                          {ROLE_VALUES.map((r) => (
                            <option key={r} value={r}>
                              {r}
                            </option>
                          ))}
                        </select>
                      );
                    }
                    if (attrType === 'number') {
                      return (
                        <input
                          type="number"
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, 'value', e.target.value)
                          }
                          className="flex-1 px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        />
                      );
                    }
                    // string type
                    return (
                      <input
                        type="text"
                        value={condition.value}
                        onChange={(e) =>
                          updateCondition(index, 'value', e.target.value)
                        }
                        placeholder={
                          condition.attribute === 'email'
                            ? 'user@buurman.io'
                            : ''
                        }
                        className="flex-1 px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                      />
                    );
                  })()}
                  <button
                    onClick={() => removeCondition(index)}
                    className="p-1.5 rounded-md text-text-muted hover:text-error-text hover:bg-error-bg transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                {condition.operator === 'regex' && (
                  <div className="ml-3 bg-surface-inset border border-border-subtle rounded-lg p-3 text-xs text-text-muted space-y-1.5">
                    <p className="font-medium text-text-secondary">
                      Regex Help
                    </p>
                    <p>
                      Matches the <span className="font-medium">entire</span>{' '}
                      value. Use{' '}
                      <code className="bg-surface-card px-1 rounded">
                        .*pattern.*
                      </code>{' '}
                      for partial match.
                    </p>
                    <div className="flex flex-wrap gap-x-4 gap-y-1">
                      <span>
                        <code className="bg-surface-card px-1 rounded">
                          .*@gmail\.com
                        </code>{' '}
                        ends with @gmail.com
                      </span>
                      <span>
                        <code className="bg-surface-card px-1 rounded">
                          (admin|editor)
                        </code>{' '}
                        admin or editor
                      </span>
                      <span>
                        <code className="bg-surface-card px-1 rounded">
                          ^[A-Z].*
                        </code>{' '}
                        starts with uppercase
                      </span>
                    </div>
                    <a
                      href="https://regex101.com/"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-primary-600 hover:underline"
                    >
                      Test on regex101.com <ExternalLink className="h-3 w-3" />
                    </a>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-6 text-text-muted text-sm">
            No conditions defined. A segment without conditions will never
            match.
          </div>
        )}
      </section>

      {/* Matched entities */}
      <section className="bg-surface-card rounded-lg border border-border-default p-6 space-y-3">
        <h2 className="text-sm font-semibold text-text-primary">
          Matched Entities
        </h2>
        <div className="flex items-center gap-6">
          <button
            onClick={() =>
              setExpandedList((prev) => (prev === 'teams' ? null : 'teams'))
            }
            className="flex items-center gap-2 text-sm text-text-secondary hover:text-text-primary transition-colors"
          >
            {expandedList === 'teams' ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
            <Building2 className="h-4 w-4 text-text-muted" />
            <span>
              <span className="font-semibold text-text-primary">
                {matches?.matchingTeamCount ?? 0}
              </span>{' '}
              teams
            </span>
          </button>
          <button
            onClick={() =>
              setExpandedList((prev) => (prev === 'users' ? null : 'users'))
            }
            className="flex items-center gap-2 text-sm text-text-secondary hover:text-text-primary transition-colors"
          >
            {expandedList === 'users' ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
            <Users className="h-4 w-4 text-text-muted" />
            <span>
              <span className="font-semibold text-text-primary">
                {matches?.matchingUserCount ?? 0}
              </span>{' '}
              users
            </span>
          </button>
        </div>

        {/* Teams list */}
        {expandedList === 'teams' && (
          <div className="mt-3">
            {teamsLoading ? (
              <div className="flex items-center gap-2 text-xs text-text-muted py-4 justify-center">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Loading...
              </div>
            ) : matchingTeams && matchingTeams.length > 0 ? (
              <>
                {matchingTeams.length === 500 && (
                  <p className="text-xs text-text-muted mb-2">
                    (limited to 500)
                  </p>
                )}
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-border-default text-left text-text-muted">
                      <th className="pb-2 font-medium">Team Name</th>
                      <th className="pb-2 font-medium">Identifier</th>
                      <th className="pb-2 font-medium">Demo</th>
                      <th className="pb-2 font-medium">Created</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border-default">
                    {matchingTeams.map((team) => (
                      <tr key={team.identifier}>
                        <td className="py-2">
                          <Link
                            to={`/teams/${team.identifier}`}
                            className="text-primary-600 hover:underline"
                          >
                            {team.teamName}
                          </Link>
                        </td>
                        <td className="py-2">
                          <code className="font-mono text-text-muted">
                            {team.identifier}
                          </code>
                        </td>
                        <td className="py-2">
                          <span
                            className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-medium ${
                              team.demo
                                ? 'bg-green-100 text-green-700'
                                : 'bg-gray-100 text-gray-500'
                            }`}
                          >
                            {team.demo ? 'demo' : 'live'}
                          </span>
                        </td>
                        <td className="py-2 text-text-muted">
                          {new Date(team.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </>
            ) : (
              <p className="text-xs text-text-muted text-center py-4">
                No matching teams
              </p>
            )}
          </div>
        )}

        {/* Users list */}
        {expandedList === 'users' && (
          <div className="mt-3">
            {usersLoading ? (
              <div className="flex items-center gap-2 text-xs text-text-muted py-4 justify-center">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Loading...
              </div>
            ) : matchingUsers && matchingUsers.length > 0 ? (
              <>
                {matchingUsers.length === 500 && (
                  <p className="text-xs text-text-muted mb-2">
                    (limited to 500)
                  </p>
                )}
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-border-default text-left text-text-muted">
                      <th className="pb-2 font-medium">Email</th>
                      <th className="pb-2 font-medium">Name</th>
                      <th className="pb-2 font-medium">Role</th>
                      <th className="pb-2 font-medium">Team</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border-default">
                    {matchingUsers.map((user) => (
                      <tr key={user.identifier}>
                        <td className="py-2">
                          <Link
                            to={`/buurmies/${user.identifier}`}
                            className="text-primary-600 hover:underline"
                          >
                            {user.email}
                          </Link>
                        </td>
                        <td className="py-2 text-text-secondary">
                          {[user.firstName, user.lastName]
                            .filter(Boolean)
                            .join(' ') || '-'}
                        </td>
                        <td className="py-2">
                          <span
                            className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-medium ${
                              user.role === 'TEAM_ADMIN'
                                ? 'bg-green-100 text-green-700'
                                : user.role === 'TEAM_EDITOR'
                                  ? 'bg-blue-100 text-blue-700'
                                  : 'bg-gray-100 text-gray-500'
                            }`}
                          >
                            {user.role}
                          </span>
                        </td>
                        <td className="py-2">
                          <Link
                            to={`/teams/${user.teamIdentifier}`}
                            className="text-primary-600 hover:underline"
                          >
                            {user.teamName}
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </>
            ) : (
              <p className="text-xs text-text-muted text-center py-4">
                No matching users
              </p>
            )}
          </div>
        )}
      </section>

      {/* Feature Flag Overrides */}
      {key && <SegmentDetailFeatureFlags segmentKey={key} />}
    </div>
  );
};
