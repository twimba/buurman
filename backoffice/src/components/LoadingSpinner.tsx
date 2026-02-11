import { Loader2 } from "lucide-react";

interface LoadingSpinnerProps {
  message?: string;
}

export const LoadingSpinner = ({
  message = "Loading...",
}: LoadingSpinnerProps) => {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-3">
      <Loader2 className="h-8 w-8 animate-spin text-[#5c7cfa]" />
      <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
        {message}
      </span>
    </div>
  );
};
