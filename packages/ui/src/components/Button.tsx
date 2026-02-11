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
    bg-gradient-to-b from-[#5c7cfa] to-[#4c6ef5]
    text-white
    border border-[#4263eb]
    shadow-sm shadow-[#5c7cfa]/20
    hover:from-[#4c6ef5] hover:to-[#4263eb] hover:border-[#3b5bdb]
    hover:shadow-md hover:shadow-[#5c7cfa]/30
    active:from-[#4263eb] active:to-[#3b5bdb]
    focus-visible:ring-2 focus-visible:ring-[#5c7cfa]/50 focus-visible:ring-offset-2
    dark:focus-visible:ring-offset-[#14161f]
    disabled:from-[#bac8ff] disabled:to-[#bac8ff] disabled:border-[#bac8ff] disabled:shadow-none
  `,
  secondary: `
    bg-white dark:bg-[#1a1d28]
    text-[#3d4463] dark:text-[#c4c8db]
    border border-[#e2e6f0] dark:border-[#2a2e3f]
    shadow-sm
    hover:bg-[#f1f3f9] dark:hover:bg-[#262a3a] hover:border-[#c9cfd9] dark:hover:border-[#3a3f54] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]
    hover:shadow
    active:bg-[#e8ecf4] dark:active:bg-[#2a2e3f]
    focus-visible:ring-2 focus-visible:ring-[#5c7cfa]/30 focus-visible:ring-offset-2
    dark:focus-visible:ring-offset-[#14161f]
    disabled:bg-[#f1f3f9] dark:disabled:bg-[#1a1d28] disabled:text-[#9ca0b8] disabled:border-[#e2e6f0] dark:disabled:border-[#2a2e3f] disabled:shadow-none
  `,
  danger: `
    bg-white dark:bg-[#1a1d28]
    text-red-600 dark:text-red-400
    border border-red-200 dark:border-red-500/25
    shadow-sm
    hover:bg-red-50 dark:hover:bg-red-500/10 hover:border-red-300 dark:hover:border-red-500/40 hover:text-red-700 dark:hover:text-red-300
    hover:shadow
    active:bg-red-100 dark:active:bg-red-500/15
    focus-visible:ring-2 focus-visible:ring-red-500/50 focus-visible:ring-offset-2
    dark:focus-visible:ring-offset-[#14161f]
    disabled:bg-[#f1f3f9] dark:disabled:bg-[#1a1d28] disabled:text-[#9ca0b8] disabled:border-[#e2e6f0] dark:disabled:border-[#2a2e3f] disabled:shadow-none
  `,
  ghost: `
    bg-transparent
    text-[#6b7194] dark:text-[#8b90a8]
    border border-transparent
    hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]
    active:bg-[#e8ecf4] dark:active:bg-[#262a3a]
    focus-visible:ring-2 focus-visible:ring-[#5c7cfa]/30 focus-visible:ring-offset-2
    dark:focus-visible:ring-offset-[#14161f]
    disabled:text-[#9ca0b8] dark:disabled:text-[#5c6180] disabled:bg-transparent
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
    dark:focus-visible:ring-offset-[#14161f]
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
