import { cn } from "../utils/cn";

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actions?: React.ReactNode;
  variant?: "page" | "section" | "inline";
  className?: string;
}

const variantStyles = {
  page: "py-16",
  section: "py-12",
  inline: "py-6",
} as const;

const iconSizes = {
  page: "h-12 w-12",
  section: "h-10 w-10",
  inline: "h-8 w-8",
} as const;

export function EmptyState({
  icon,
  title,
  description,
  actions,
  variant = "section",
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center text-center",
        variantStyles[variant],
        className,
      )}
      role="status"
    >
      {icon && (
        <div
          className={cn("mb-4 text-text-muted", iconSizes[variant])}
          aria-hidden="true"
        >
          {icon}
        </div>
      )}
      <h3
        className={cn(
          "font-semibold text-text-primary",
          variant === "inline" ? "text-sm" : "text-base",
        )}
      >
        {title}
      </h3>
      {description && (
        <p
          className={cn(
            "mt-1 text-text-secondary",
            variant === "inline" ? "text-sm" : "text-sm max-w-sm",
          )}
        >
          {description}
        </p>
      )}
      {actions && <div className="mt-4">{actions}</div>}
    </div>
  );
}

EmptyState.displayName = "EmptyState";
