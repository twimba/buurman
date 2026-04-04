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
        paddingMap[padding],
        className
      )}
    >
      {children}
    </div>
  );
}

Card.displayName = 'Card';
