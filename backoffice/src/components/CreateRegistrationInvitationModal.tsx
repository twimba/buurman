import { useState } from "react";
import { X, RefreshCw, Loader2, Sparkles } from "lucide-react";
import { RichTextEditor } from "./RichTextEditor";
import {
  useCreateRegistrationInvitation,
  useSuggestCode,
} from "../hooks/useRegistrationInvitations";

interface Props {
  onClose: () => void;
}

export function CreateRegistrationInvitationModal({ onClose }: Props) {
  const [codeOverride, setCodeOverride] = useState<string | null>(null);
  const [maxUsages, setMaxUsages] = useState<string>("1");
  const [unlimited, setUnlimited] = useState(false);
  const [expiresAt, setExpiresAt] = useState("");
  const [neverExpires, setNeverExpires] = useState(true);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");

  const { data: suggestedCode, refetch: refreshCode } = useSuggestCode();
  const createMutation = useCreateRegistrationInvitation();

  const code = codeOverride ?? suggestedCode ?? "";
  const setCode = (v: string) => setCodeOverride(v);

  const handleRefreshCode = () => {
    setCodeOverride(null);
    refreshCode().then((result) => {
      if (result.data) {
        setCodeOverride(result.data);
      }
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!code.trim()) {
      setError("Code is required");
      return;
    }

    try {
      await createMutation.mutateAsync({
        code: code.trim().toLowerCase(),
        maxUsages: unlimited ? undefined : parseInt(maxUsages, 10) || 1,
        expiresAt: neverExpires ? undefined : expiresAt || undefined,
        note: note || undefined,
      });
      onClose();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { detail?: string } } };
      setError(error.response?.data?.detail || "Failed to create invitation");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="relative z-10 w-full max-w-lg rounded-lg bg-surface-card shadow-xl">
        <div className="flex items-center justify-between border-b border-border-default px-6 py-4">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary-500" />
            <h2 className="text-lg font-semibold text-text-primary">
              Create Invitation
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-md hover:bg-surface-inset transition-colors"
          >
            <X className="h-5 w-5 text-text-muted" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Code */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">
              Invitation Code
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="flex-1 rounded-md border border-border-default bg-surface-card px-3 py-2 text-sm font-mono focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                placeholder="e.g. snowy-cat"
              />
              <button
                type="button"
                onClick={handleRefreshCode}
                className="p-2 rounded-md border border-border-default hover:bg-surface-inset transition-colors"
                title="Generate new code"
              >
                <RefreshCw className="h-4 w-4 text-text-secondary" />
              </button>
            </div>
            <p className="text-xs text-text-secondary mt-1">
              Auto-generated, but you can customize it
            </p>
          </div>

          {/* Max Usages */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">
              Max Usages
            </label>
            <div className="flex items-center gap-3">
              <input
                type="number"
                value={maxUsages}
                onChange={(e) => setMaxUsages(e.target.value)}
                disabled={unlimited}
                min={1}
                className="w-24 rounded-md border border-border-default bg-surface-card px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
              />
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={unlimited}
                  onChange={(e) => setUnlimited(e.target.checked)}
                  className="rounded border-border-strong text-primary-600 focus:ring-primary-500"
                />
                <span className="text-sm text-text-secondary">Unlimited</span>
              </label>
            </div>
          </div>

          {/* Expiration */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">
              Expiration
            </label>
            <div className="space-y-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={neverExpires}
                  onChange={(e) => setNeverExpires(e.target.checked)}
                  className="rounded border-border-strong text-primary-600 focus:ring-primary-500"
                />
                <span className="text-sm text-text-secondary">
                  Never expires
                </span>
              </label>
              {!neverExpires && (
                <input
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  className="w-full rounded-md border border-border-default bg-surface-card px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                />
              )}
            </div>
          </div>

          {/* Internal Note */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">
              Internal Note{" "}
              <span className="text-text-muted font-normal">(optional)</span>
            </label>
            <RichTextEditor
              value={note}
              onChange={setNote}
              placeholder="Add an internal note about this invitation..."
            />
          </div>

          {error && (
            <div className="rounded-lg bg-error-bg border border-error-border p-3">
              <p className="text-sm text-error-text">{error}</p>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-border-default px-4 py-2 text-sm font-medium text-text-primary hover:bg-surface-inset transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="inline-flex items-center gap-2 rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {createMutation.isPending && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              Create Invitation
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
