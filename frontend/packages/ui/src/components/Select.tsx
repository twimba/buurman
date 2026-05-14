import { cn } from '../utils/cn';

interface SelectProps extends Omit<
  React.SelectHTMLAttributes<HTMLSelectElement>,
  'size'
> {
  size?: 'sm' | 'md' | 'lg';
  error?: boolean;
  ref?: React.Ref<HTMLSelectElement>;
}

const sizeMap = {
  sm: 'h-8 px-2.5 text-sm',
  md: 'h-10 px-3 text-sm',
  lg: 'h-12 px-4 text-base',
} as const;

export function Select({
  size = 'md',
  error,
  className,
  children,
  ref,
  ...props
}: SelectProps) {
  return (
    <select
      ref={ref}
      className={cn(
        'w-full rounded-md border bg-surface-card text-text-primary',
        'transition-colors duration-150',
        'focus-ring',
        error
          ? 'border-error-border focus-visible:shadow-ring-error'
          : 'border-border-default',
        props.disabled && 'cursor-not-allowed bg-surface-inset text-text-disabled',
        sizeMap[size],
        className
      )}
      {...props}
    >
      {children}
    </select>
  );
}

Select.displayName = 'Select';
