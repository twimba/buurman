import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layers, Plus, Trash2, ChevronRight } from 'lucide-react';
import { ConfirmDialog } from '@buurman/ui';
import { useSegmentsList, useDeleteSegment } from '../hooks/useSegments';

const ATTRIBUTE_LABELS: Record<string, string> = {
  is_demo: 'Is Demo',
  is_owner: 'Is Owner',
  is_team: 'Is Team',
  is_user: 'Is User',
  role: 'Role',
  email: 'Email',
  email_verified: 'Email Verified',
  property_count: 'Property Count',
  member_count: 'Member Count',
  team_age_days: 'Team Age (days)',
  contract_count: 'Contract Count',
  contact_count: 'Contact Count',
  photo_count: 'Photo Count',
  document_count: 'Document Count',
  expense_count: 'Expense Count',
  payment_count: 'Payment Count',
  calendar_feed_count: 'Calendar Feed Count',
};

const OPERATOR_LABELS: Record<string, string> = {
  eq: '=',
  neq: '!=',
  in: 'in',
  not_in: 'not in',
  gt: '>',
  gte: '>=',
  lt: '<',
  lte: '<=',
  contains: 'contains',
  not_contains: 'not contains',
  starts_with: 'starts with',
  ends_with: 'ends with',
  regex: 'matches regex',
};

export const SegmentsPage = () => {
  const navigate = useNavigate();
  const { data: segments, isLoading } = useSegmentsList();
  const deleteSegment = useDeleteSegment();
  const [deleteKey, setDeleteKey] = useState<string | null>(null);

  const handleDelete = () => {
    if (deleteKey) {
      deleteSegment.mutate(deleteKey, {
        onSuccess: () => setDeleteKey(null),
      });
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Segments</h1>
          <p className="mt-1 text-sm text-text-secondary">
            Manage segments for feature flag targeting
          </p>
        </div>
        <button
          onClick={() => navigate('/segments/new')}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary-500 text-white text-sm font-medium hover:bg-primary-600 transition-colors"
        >
          <Plus className="h-4 w-4" />
          New Segment
        </button>
      </div>

      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        {isLoading ? (
          <div className="text-center py-12 text-text-muted text-sm">
            Loading segments...
          </div>
        ) : segments && segments.length > 0 ? (
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-default bg-surface-inset">
                <th className="text-left px-4 py-3 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Name
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Key
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Conditions
                </th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Priority
                </th>
                <th className="text-right px-4 py-3 text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {segments.map((segment) => (
                <tr
                  key={segment.key}
                  className="hover:bg-surface-inset/50 cursor-pointer transition-colors"
                  onClick={() => navigate(`/segments/${segment.key}`)}
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-text-primary">
                        {segment.name}
                      </span>
                    </div>
                    {segment.description && (
                      <p className="text-xs text-text-muted mt-0.5">
                        {segment.description}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <code className="text-xs font-mono text-text-secondary bg-surface-inset px-1.5 py-0.5 rounded">
                      {segment.key}
                    </code>
                  </td>
                  <td className="px-4 py-3">
                    {segment.conditions.length > 0 ? (
                      <div className="space-y-0.5">
                        {segment.conditions.map((c, i) => (
                          <div key={i} className="text-xs text-text-secondary">
                            <span className="font-medium">
                              {ATTRIBUTE_LABELS[c.attribute] ?? c.attribute}
                            </span>{' '}
                            <span className="text-text-muted">
                              {OPERATOR_LABELS[c.operator] ?? c.operator}
                            </span>{' '}
                            <span className="font-mono">{c.value}</span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <span className="text-xs text-text-muted italic">
                        No conditions
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary">
                    {segment.priority}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteKey(segment.key);
                        }}
                        className="p-1.5 rounded-md text-text-muted hover:text-error-text hover:bg-error-bg transition-colors"
                        title="Delete segment"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                      <ChevronRight className="h-4 w-4 text-text-muted" />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="text-center py-12 text-text-muted">
            <Layers className="h-10 w-10 mx-auto mb-3 opacity-40" />
            <p className="text-sm">No segments configured</p>
          </div>
        )}
      </div>

      {deleteKey !== null && (
        <ConfirmDialog
          title="Delete Segment"
          message={`Are you sure you want to delete segment "${deleteKey}"? Feature flag overrides targeting this segment will no longer match.`}
          confirmLabel="Delete"
          variant="danger"
          onConfirm={handleDelete}
          onCancel={() => setDeleteKey(null)}
          isLoading={deleteSegment.isPending}
        />
      )}
    </div>
  );
};
