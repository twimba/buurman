import { useState } from "react";
import { X, Send, Loader2, Mail, MessageSquare } from "lucide-react";
import { useSendRegistrationInvitation } from "../hooks/useRegistrationInvitations";
import type { RegistrationInvitation } from "../api/registrationInvitations";

interface Props {
  invitation: RegistrationInvitation;
  onClose: () => void;
}

export function SendRegistrationInvitationModal({
  invitation,
  onClose,
}: Props) {
  const [recipient, setRecipient] = useState("");
  const [channel, setChannel] = useState<"EMAIL" | "SMS">("EMAIL");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const sendMutation = useSendRegistrationInvitation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!recipient.trim()) {
      setError(
        channel === "EMAIL"
          ? "Email address is required"
          : "Phone number is required",
      );
      return;
    }

    try {
      await sendMutation.mutateAsync({
        identifier: invitation.identifier,
        data: { recipient: recipient.trim(), channel },
      });
      setSuccess(true);
      setTimeout(onClose, 1500);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { detail?: string } } };
      setError(error.response?.data?.detail || "Failed to send invitation");
    }
  };

  const registerUrl = `${window.location.protocol}//app.${window.location.hostname.replace(/^backoffice\./, "")}/register?code=${invitation.code}`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="relative z-10 w-full max-w-md rounded-lg bg-surface-card shadow-xl">
        <div className="flex items-center justify-between border-b border-border-default px-6 py-4">
          <div className="flex items-center gap-2">
            <Send className="h-5 w-5 text-primary-500" />
            <h2 className="text-lg font-semibold text-text-primary">
              Send Invitation
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-md hover:bg-surface-inset transition-colors"
          >
            <X className="h-5 w-5 text-text-muted" />
          </button>
        </div>

        {success ? (
          <div className="p-6 text-center">
            <div className="text-success-text text-lg font-semibold mb-1">
              Sent!
            </div>
            <p className="text-sm text-text-secondary">
              Invitation sent to {recipient}
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            {/* Code preview */}
            <div className="rounded-lg bg-surface-inset border border-border-default p-3">
              <div className="text-xs text-text-secondary mb-1">
                Invitation Code
              </div>
              <div className="font-mono font-semibold text-primary-600">
                {invitation.code}
              </div>
            </div>

            {/* Channel */}
            <div>
              <label className="block text-sm font-medium text-text-primary mb-2">
                Send via
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setChannel("EMAIL")}
                  className={`flex-1 flex items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-medium transition-colors ${
                    channel === "EMAIL"
                      ? "border-primary-500 bg-primary-50 text-primary-700"
                      : "border-border-default text-text-secondary hover:bg-surface-inset"
                  }`}
                >
                  <Mail className="h-4 w-4" />
                  Email
                </button>
                <button
                  type="button"
                  onClick={() => setChannel("SMS")}
                  className={`flex-1 flex items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-medium transition-colors ${
                    channel === "SMS"
                      ? "border-primary-500 bg-primary-50 text-primary-700"
                      : "border-border-default text-text-secondary hover:bg-surface-inset"
                  }`}
                >
                  <MessageSquare className="h-4 w-4" />
                  SMS
                </button>
              </div>
            </div>

            {/* Recipient */}
            <div>
              <label className="block text-sm font-medium text-text-primary mb-1.5">
                {channel === "EMAIL" ? "Email Address" : "Phone Number"}
              </label>
              <input
                type={channel === "EMAIL" ? "email" : "tel"}
                value={recipient}
                onChange={(e) => setRecipient(e.target.value)}
                className="w-full rounded-md border border-border-default bg-surface-card px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                placeholder={
                  channel === "EMAIL" ? "john@example.com" : "+1 (555) 123-4567"
                }
              />
            </div>

            {/* Preview */}
            <div className="rounded-lg bg-surface-inset border border-border-default p-3">
              <div className="text-xs text-text-secondary mb-1.5">
                Message preview
              </div>
              <div className="text-xs text-text-secondary leading-relaxed">
                {channel === "EMAIL" ? (
                  <>
                    The recipient will receive an email with the code{" "}
                    <span className="font-mono font-semibold">
                      {invitation.code}
                    </span>{" "}
                    and a direct link to register.
                  </>
                ) : (
                  <>
                    &ldquo;You&apos;ve been invited to join Buurman! Use code:{" "}
                    <span className="font-mono font-semibold">
                      {invitation.code}
                    </span>{" "}
                    or register at: {registerUrl}&rdquo;
                  </>
                )}
              </div>
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
                disabled={sendMutation.isPending}
                className="inline-flex items-center gap-2 rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
              >
                {sendMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
                Send
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
