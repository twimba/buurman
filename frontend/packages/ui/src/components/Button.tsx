import { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '../utils/cn';

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'danger'
  | 'ghost'
  | 'success';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  children: ReactNode;
  ref?: React.Ref<HTMLButtonElement>;
}

const variantStyles: Record<ButtonVariant, string> = {
  primary: `
    bg-gradient-to-b from-primary-500 to-primary-600
    text-white
    border border-primary-700
    shadow-sm shadow-primary-500/20
    hover:from-primary-600 hover:to-primary-700 hover:border-primary-700
    hover:shadow-md hover:shadow-primary-500/30
    active:from-primary-700 active:to-primary-800
    focus-visible:ring-2 focus-visible:ring-primary-500/50 focus-visible:ring-offset-2
    focus-visible:ring-offset-surface-card
    disabled:from-primary-200 disabled:to-primary-200 disabled:border-primary-200 disabled:shadow-none
  `,
  secondary: `
    bg-surface-card
    text-text-secondary
    border border-border-default
    shadow-sm
    hover:bg-neutral-50 hover:border-border-strong hover:text-text-primary
    hover:shadow
    active:bg-neutral-100
    focus-visible:ring-2 focus-visible:ring-primary-500/30 focus-visible:ring-offset-2
    focus-visible:ring-offset-surface-card
    disabled:bg-neutral-50 disabled:text-text-disabled disabled:border-border-default disabled:shadow-none
  `,
  danger: `
    bg-surface-card
    text-error-text
    border border-error-border
    shadow-sm
    hover:bg-error-bg hover:border-error-border hover:text-error-text
    hover:shadow
    active:bg-error-bg
    focus-visible:ring-2 focus-visible:ring-error/50 focus-visible:ring-offset-2
    focus-visible:ring-offset-surface-card
    disabled:bg-neutral-50 disabled:text-text-disabled disabled:border-border-default disabled:shadow-none
  `,
  ghost: `
    bg-transparent
    text-text-secondary
    border border-transparent
    hover:bg-neutral-50 hover:text-text-primary
    active:bg-neutral-100
    focus-visible:ring-2 focus-visible:ring-primary-500/30 focus-visible:ring-offset-2
    focus-visible:ring-offset-surface-card
    disabled:text-text-disabled disabled:bg-transparent
  `,
  success: `
    bg-success
    text-white
    border border-success
    shadow-sm
    hover:bg-success/90
    hover:shadow-md
    active:bg-success/80
    focus-visible:ring-2 focus-visible:ring-success/50 focus-visible:ring-offset-2
    focus-visible:ring-offset-surface-card
    disabled:bg-success/50 disabled:border-success/50 disabled:shadow-none
  `,
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-xs gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2.5',
};

const iconSizes: Record<ButtonSize, string> = {
  sm: 'h-3.5 w-3.5',
  md: 'h-4 w-4',
  lg: 'h-5 w-5',
};

export const Button = ({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  children,
  disabled,
  className,
  ref,
  ...props
}: ButtonProps) => {
  const isDisabled = disabled || isLoading;

  return (
    <button
      ref={ref}
      disabled={isDisabled}
      className={cn(
        'inline-flex items-center justify-center',
        'font-medium',
        'rounded-md',
        'transition-all duration-150 ease-out',
        'outline-none',
        'select-none',
        'disabled:cursor-not-allowed',
        'disabled:pointer-events-none',
        variantStyles[variant],
        sizeStyles[size],
        className
      )}
      {...props}
    >
      {isLoading ? (
        <Loader2 className={cn(iconSizes[size], 'animate-spin')} />
      ) : leftIcon ? (
        <span
          className={cn(
            iconSizes[size],
            'flex-shrink-0 [&>svg]:h-full [&>svg]:w-full'
          )}
        >
          {leftIcon}
        </span>
      ) : null}
      <span className="truncate">{children}</span>
      {!isLoading && rightIcon && (
        <span
          className={cn(
            iconSizes[size],
            'flex-shrink-0 [&>svg]:h-full [&>svg]:w-full'
          )}
        >
          {rightIcon}
        </span>
      )}
    </button>
  );
};
