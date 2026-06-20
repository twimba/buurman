import { StatusBadge } from '@buurman/ui';
import { Check, Clock } from 'lucide-react';

export function Colors() {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <StatusBadge label="Active" color="green" />
      <StatusBadge label="Pending" color="amber" />
      <StatusBadge label="Overdue" color="red" />
      <StatusBadge label="Draft" color="gray" />
      <StatusBadge label="Signed" color="blue" />
      <StatusBadge label="Archived" color="violet" />
    </div>
  );
}

export function WithDot() {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <StatusBadge label="Occupied" color="green" dot />
      <StatusBadge label="Vacant" color="gray" dot />
      <StatusBadge label="Notice given" color="orange" dot />
    </div>
  );
}

export function Shapes() {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <StatusBadge label="Rounded" color="teal" shape="rounded" />
      <StatusBadge label="Pill" color="indigo" shape="pill" />
      <StatusBadge label="Paid" color="emerald" icon={<Check className="h-3 w-3" />} />
      <StatusBadge label="Waiting" color="amber" icon={<Clock className="h-3 w-3" />} />
    </div>
  );
}

export function Sizes() {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <StatusBadge label="Extra small" color="blue" size="xs" />
      <StatusBadge label="Small" color="blue" size="sm" />
      <StatusBadge label="Medium" color="blue" size="md" />
    </div>
  );
}
