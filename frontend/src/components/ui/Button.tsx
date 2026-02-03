import { forwardRef, ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'success';
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
    bg-white
    text-gray-700
    border border-gray-200
    shadow-sm
    hover:bg-gray-50 hover:border-gray-300 hover:text-gray-900
    hover:shadow
    active:bg-gray-100
    focus-visible:ring-2 focus-visible:ring-gray-400/50 focus-visible:ring-offset-2
    disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200 disabled:shadow-none
  `,
  danger: `
    bg-white
    text-red-600
    border border-red-200
    shadow-sm
    hover:bg-red-50 hover:border-red-300 hover:text-red-700
    hover:shadow
    active:bg-red-100
    focus-visible:ring-2 focus-visible:ring-red-500/50 focus-visible:ring-offset-2
    disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200 disabled:shadow-none
  `,
  ghost: `
    bg-transparent
    text-gray-600
    border border-transparent
    hover:bg-gray-100 hover:text-gray-900
    active:bg-gray-200
    focus-visible:ring-2 focus-visible:ring-gray-400/50 focus-visible:ring-offset-2
    disabled:text-gray-400 disabled:bg-transparent
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
        `.trim().replace(/\s+/g, ' ')}
        {...props}
      >
        {isLoading ? (
          <Loader2 className={`${iconSizes[size]} animate-spin`} />
        ) : leftIcon ? (
          <span className={`${iconSizes[size]} flex-shrink-0 [&>svg]:h-full [&>svg]:w-full`}>
            {leftIcon}
          </span>
        ) : null}
        <span className="truncate">{children}</span>
        {!isLoading && rightIcon && (
          <span className={`${iconSizes[size]} flex-shrink-0 [&>svg]:h-full [&>svg]:w-full`}>
            {rightIcon}
          </span>
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';
