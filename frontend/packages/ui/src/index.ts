// Existing components
export { Button } from './components/Button';
export type { ButtonVariant, ButtonSize } from './components/Button';
export { Pagination } from './components/Pagination';
export { PageHeader } from './components/PageHeader';
export { ConfirmDialog } from './components/ConfirmDialog';
export { RefreshButton } from './components/RefreshButton';
export { SidebarTooltip } from './components/SidebarTooltip';

// New components — Batch 1 (Foundation)
export { Card } from './components/Card';
export { Input } from './components/Input';
export { Select } from './components/Select';
export { Textarea } from './components/Textarea';
export { Skeleton } from './components/Skeleton';
export { FormField } from './components/FormField';

// New components — Batch 2 (Interactive)
export { ModalWrapper } from './components/ModalWrapper';
export { StatusBadge } from './components/StatusBadge';
export type { BadgeColorVariant } from './components/StatusBadge';
export { EmptyState } from './components/EmptyState';
export { ToastProvider, useToast } from './components/Toast';
export type { ToastType } from './components/Toast';

// New components — Batch 3 (Data)
export { DataTable } from './components/DataTable';
export type { ColumnDef, SortState } from './components/DataTable';
export { MetricCard } from './components/MetricCard';
export { FilterBar } from './components/FilterBar';
export type { FilterDef } from './components/FilterBar';

// Promoted shared components
export { LoadingSpinner } from './components/LoadingSpinner';
export { EnvironmentBanner } from './components/EnvironmentBanner';
export type { Environment } from './components/EnvironmentBanner';

// Hooks
export { useFilterState } from './hooks/useFilterState';
export { usePagination } from './hooks/usePagination';
export type { PageParams } from './hooks/usePagination';
export { useTabState } from './hooks/useTabState';

// Rich text
export { RichTextEditor } from './components/RichTextEditor';
export { RichTextDisplay, stripHtml } from './components/RichTextDisplay';

// Utilities
export { cn } from './utils/cn';

// Auth
export { createAuthProvider } from './auth/createAuthProvider';
export type {
  AuthContextType,
  AuthProviderOptions,
} from './auth/createAuthProvider';

// Analytics
export { createAnalytics } from './utils/analytics';
