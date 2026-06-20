import { Skeleton } from '@buurman/ui';

export function TextLines() {
  return (
    <div className="flex w-80 flex-col gap-2">
      <Skeleton className="h-4 w-3/4" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-5/6" />
      <Skeleton className="h-4 w-2/3" />
    </div>
  );
}

export function CardPlaceholder() {
  return (
    <div className="flex w-80 items-center gap-3 rounded-lg border border-border-default bg-surface-card p-4">
      <Skeleton className="h-12 w-12 rounded-full" />
      <div className="flex flex-1 flex-col gap-2">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-3 w-1/2" />
      </div>
    </div>
  );
}

export function Shapes() {
  return (
    <div className="flex items-end gap-4">
      <Skeleton className="h-16 w-16 rounded-full" />
      <Skeleton className="h-16 w-24 rounded-md" />
      <Skeleton className="h-24 w-16 rounded-md" />
    </div>
  );
}
