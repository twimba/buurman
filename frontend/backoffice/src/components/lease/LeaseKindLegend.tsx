import { Info } from 'lucide-react';
import { StatusBadge } from '@buurman/ui';
import type { LeaseKind } from '../../generated/models';
import {
  LEASE_KIND_META,
  LEASE_KIND_ORDER,
  fallbackText,
} from '../../lib/leaseKindMeta';
import { Popover } from './Popover';

/** Info icon opening the legend of all lease kinds; `current` is highlighted. */
export const LeaseKindLegend = ({ current }: { current?: LeaseKind }) => (
  <Popover
    label="Lease kinds"
    trigger={<Info className="h-4 w-4" aria-hidden="true" />}
  >
    <h3 className="mb-3 text-sm font-semibold text-text-primary">
      Lease kinds
    </h3>
    <ul className="space-y-3">
      {LEASE_KIND_ORDER.map((kind) => {
        const meta = LEASE_KIND_META[kind];
        return (
          <li
            key={kind}
            className={`rounded-md p-2 text-xs text-text-secondary ${
              kind === current ? 'ring-2 ring-primary-500/40' : ''
            }`}
            aria-current={kind === current ? 'true' : undefined}
          >
            <StatusBadge label={meta.label} color={meta.color} size="xs" />
            <p className="mt-1 text-text-primary">{meta.oneLine}</p>
            <p className="mt-1">
              <span className="font-medium">Applies when:</span>{' '}
              {meta.appliesWhen}
            </p>
            <p className="mt-1">
              <span className="font-medium">Falls back to:</span>{' '}
              {fallbackText(kind)}
            </p>
          </li>
        );
      })}
    </ul>
    <p className="mt-3 text-xs text-text-muted">
      Every kind finally falls back to Legacy. The kind outranks the language:
      the fallback chain is walked before falling back to another language.
    </p>
  </Popover>
);
