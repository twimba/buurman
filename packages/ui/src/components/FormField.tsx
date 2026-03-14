import { cn } from "../utils/cn";

interface FormFieldProps {
  label: string;
  htmlFor?: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: React.ReactNode;
  labelPosition?: "top" | "left";
  readOnly?: boolean;
  className?: string;
  colSpan?: 1 | 2;
}

export function FormField({
  label,
  htmlFor,
  required,
  error,
  hint,
  children,
  labelPosition = "top",
  readOnly,
  className,
  colSpan,
}: FormFieldProps) {
  const errorId = htmlFor ? `${htmlFor}-error` : undefined;
  const hintId = htmlFor ? `${htmlFor}-hint` : undefined;

  return (
    <div
      className={cn(
        labelPosition === "left" && "flex items-start gap-4",
        colSpan === 2 && "col-span-2",
        className,
      )}
    >
      <label
        htmlFor={htmlFor}
        className={cn(
          "block text-sm font-medium text-text-primary",
          labelPosition === "top" ? "mb-1.5" : "mt-2.5 w-40 shrink-0",
          readOnly && "text-text-muted",
        )}
      >
        {label}
        {required && (
          <span className="ml-0.5 text-error" aria-hidden="true">
            *
          </span>
        )}
      </label>

      <div className="min-w-0 flex-1">
        {children}

        {error && (
          <p id={errorId} className="mt-1 text-sm text-error-text" role="alert">
            {error}
          </p>
        )}

        {hint && !error && (
          <p id={hintId} className="mt-1 text-sm text-text-muted">
            {hint}
          </p>
        )}
      </div>
    </div>
  );
}

FormField.displayName = "FormField";
