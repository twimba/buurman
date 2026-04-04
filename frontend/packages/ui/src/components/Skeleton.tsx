import { cn } from '../utils/cn';

interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-shimmer rounded-md bg-gradient-to-r from-neutral-100 via-neutral-50 to-neutral-100 bg-[length:200%_100%]',
        className
      )}
    />
  );
}

Skeleton.displayName = 'Skeleton';
