import { Loader2 } from "lucide-react";
import { cn } from "../utils/cn";

interface LoadingSpinnerProps {
  /** Optional message displayed below the spinner */
  message?: string;
  /** Fill the full viewport height (default: false) */
  fullScreen?: boolean;
  className?: string;
}

export function LoadingSpinner({
  message,
  fullScreen,
  className,
}: LoadingSpinnerProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-3",
        fullScreen ? "min-h-screen" : "p-8",
        className,
      )}
    >
      <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      {message && (
        <span className="text-sm text-text-secondary">{message}</span>
      )}
    </div>
  );
}

LoadingSpinner.displayName = "LoadingSpinner";
