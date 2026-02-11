import { RefreshCw } from 'lucide-react';

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
    className="p-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] hover:text-[#3d4463] dark:hover:text-[#c4c8db] transition-colors disabled:opacity-50"
  >
    <RefreshCw className={`h-5 w-5 ${isRefreshing ? 'animate-spin' : ''}`} />
  </button>
);
