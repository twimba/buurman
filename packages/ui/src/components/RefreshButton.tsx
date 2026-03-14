import { RefreshCw } from "lucide-react";
import { cn } from "../utils/cn";

interface RefreshButtonProps {
  onClick: () => void;
  isRefreshing?: boolean;
}

export const RefreshButton = ({
  onClick,
  isRefreshing,
}: RefreshButtonProps) => (
  <button
    onClick={onClick}
    disabled={isRefreshing}
    title="Refresh data"
    className="p-2 border border-border-strong rounded-lg text-text-secondary hover:bg-neutral-50 hover:text-text-primary transition-colors disabled:opacity-50"
  >
    <RefreshCw className={cn("h-5 w-5", isRefreshing && "animate-spin")} />
  </button>
);
