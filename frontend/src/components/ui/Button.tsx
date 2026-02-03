import { forwardRef, ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

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
}

const variantStyles: Record<ButtonVariant, string> = {
  primary: `
    bg-gradient-to-b from-blue-500 to-blue-600
    text-white
    border border-blue-600
    shadow-sm shadow-blue-500/25
    hover:from-blue-600 hover:to-blue-700 hover:border-blue-700
    hover:shadow-md hover:shadow-blue-500/30
    active:from-blue-700 active:to-blue-800
    focus-visible:ring-2 focus-visible:ring-blue-500/50 focus-visible:ring-offset-2
    disabled:from-blue-300 disabled:to-blue-400 disabled:border-blue-300 disabled:shadow-none
  `,
  secondary: `
    bg-white dark:bg-gray-800
    text-gray-700 dark:text-gray-300
    border border-gray-200 dark:border-gray-600
    shadow-sm
    hover:bg-gray-50 dark:hover:bg-gray-700 hover:border-gray-300 dark:hover:border-gray-500 hover:text-gray-900 dark:hover:text-gray-100
    hover:shadow
    active:bg-gray-100 dark:active:bg-gray-600
    focus-visible:ring-2 focus-visible:ring-gray-400/50 focus-visible:ring-offset-2
    disabled:bg-gray-50 dark:disabled:bg-gray-800 disabled:text-gray-400 disabled:border-gray-200 dark:disabled:border-gray-600 disabled:shadow-none
  `,
  danger: `
    bg-white dark:bg-gray-800
    text-red-600 dark:text-red-400
    border border-red-200 dark:border-red-900
    shadow-sm
    hover:bg-red-50 dark:hover:bg-red-900/30 hover:border-red-300 dark:hover:border-red-700 hover:text-red-700 dark:hover:text-red-300
    hover:shadow
    active:bg-red-100 dark:active:bg-red-900
    focus-visible:ring-2 focus-visible:ring-red-500/50 focus-visible:ring-offset-2
    disabled:bg-gray-50 dark:disabled:bg-gray-800 disabled:text-gray-400 disabled:border-gray-200 dark:disabled:border-gray-600 disabled:shadow-none
  `,
  ghost: `
    bg-transparent
    text-gray-600 dark:text-gray-400
    border border-transparent
    hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-gray-100
    active:bg-gray-200 dark:active:bg-gray-600
    focus-visible:ring-2 focus-visible:ring-gray-400/50 focus-visible:ring-offset-2
    disabled:text-gray-400 dark:disabled:text-gray-500 disabled:bg-transparent
  `,
  success: `
    bg-gradient-to-b from-emerald-500 to-emerald-600
    text-white
    border border-emerald-600
    shadow-sm shadow-emerald-500/25
    hover:from-emerald-600 hover:to-emerald-700 hover:border-emerald-700
    hover:shadow-md hover:shadow-emerald-500/30
    active:from-emerald-700 active:to-emerald-800
    focus-visible:ring-2 focus-visible:ring-emerald-500/50 focus-visible:ring-offset-2
    disabled:from-emerald-300 disabled:to-emerald-400 disabled:border-emerald-300 disabled:shadow-none
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

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      children,
      disabled,
      className = '',
      ...props
    },
    ref
  ) => {
    const isDisabled = disabled || isLoading;

    return (
      <button
        ref={ref}
        disabled={isDisabled}
        className={`
          inline-flex items-center justify-center
          font-medium
          rounded-lg
          transition-all duration-150 ease-out
          outline-none
          select-none
          disabled:cursor-not-allowed
          disabled:pointer-events-none
          ${variantStyles[variant]}
          ${sizeStyles[size]}
          ${className}
        `
          .trim()
          .replace(/\s+/g, ' ')}
        {...props}
      >
        {isLoading ? (
          <Loader2 className={`${iconSizes[size]} animate-spin`} />
        ) : leftIcon ? (
          <span
            className={`${iconSizes[size]} flex-shrink-0 [&>svg]:h-full [&>svg]:w-full`}
          >
            {leftIcon}
          </span>
        ) : null}
        <span className="truncate">{children}</span>
        {!isLoading && rightIcon && (
          <span
            className={`${iconSizes[size]} flex-shrink-0 [&>svg]:h-full [&>svg]:w-full`}
          >
            {rightIcon}
          </span>
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';
