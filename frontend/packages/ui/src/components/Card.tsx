import { cn } from '../utils/cn';

interface CardProps {
  children: React.ReactNode;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  className?: string;
}

const paddingMap = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8',
} as const;

export function Card({ children, padding = 'md', className }: CardProps) {
  return (
    <div
      className={cn(
        'bg-surface-card rounded-lg border border-border-default shadow-sm',
        // Establishes a `card` container query context. Descendants can
        // adapt to the card's own width with `@container/card (min-width: ...)`
        // instead of the viewport breakpoint — the same Card renders
        // differently in a 1-col phone layout, a 2-col tablet grid, or
        // an iPad-rail split-view, without viewport-bound class forks.
        '@container/card',
        paddingMap[padding],
        className
      )}
    >
      {children}
    </div>
  );
}

Card.displayName = 'Card';
