import { StatusBadge } from '@buurman/ui';
import type { LeaseKind } from '../../generated/models';
import { LEASE_KIND_META, fallbackText } from '../../lib/leaseKindMeta';
import { Tooltip } from './Tooltip';

export const LeaseKindBadge = ({ kind }: { kind: LeaseKind }) => {
  const meta = LEASE_KIND_META[kind];
  return (
    <Tooltip
      content={
        <>
          <span className="block font-mono text-text-muted">{kind}</span>
          <span className="mt-1 block text-text-primary">{meta.oneLine}</span>
          <span className="mt-1 block">
            Falls back to: {fallbackText(kind)}
          </span>
        </>
      }
    >
      <StatusBadge label={meta.label} color={meta.color} size="xs" />
    </Tooltip>
  );
};
