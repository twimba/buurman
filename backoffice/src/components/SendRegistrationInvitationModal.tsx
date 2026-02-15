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
      <div className="relative z-10 w-full max-w-md rounded-2xl bg-white dark:bg-zinc-800 shadow-xl">
        <div className="flex items-center justify-between border-b border-zinc-200 dark:border-zinc-700 px-6 py-4">
          <div className="flex items-center gap-2">
            <Send className="h-5 w-5 text-indigo-500" />
            <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
              Send Invitation
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-700 transition-colors"
          >
            <X className="h-5 w-5 text-zinc-400" />
          </button>
        </div>

        {success ? (
          <div className="p-6 text-center">
            <div className="text-emerald-500 text-lg font-semibold mb-1">
              Sent!
            </div>
            <p className="text-sm text-zinc-500">
              Invitation sent to {recipient}
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            {/* Code preview */}
            <div className="rounded-lg bg-zinc-50 dark:bg-zinc-700/50 border border-zinc-200 dark:border-zinc-600 p-3">
              <div className="text-xs text-zinc-500 mb-1">Invitation Code</div>
              <div className="font-mono font-semibold text-indigo-600 dark:text-indigo-400">
                {invitation.code}
              </div>
            </div>

            {/* Channel */}
            <div>
              <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                Send via
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setChannel("EMAIL")}
                  className={`flex-1 flex items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-medium transition-colors ${
                    channel === "EMAIL"
                      ? "border-indigo-500 bg-indigo-50 text-indigo-700 dark:bg-indigo-900/20 dark:text-indigo-400 dark:border-indigo-500"
                      : "border-zinc-200 dark:border-zinc-600 text-zinc-600 dark:text-zinc-400 hover:bg-zinc-50 dark:hover:bg-zinc-700"
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
                      ? "border-indigo-500 bg-indigo-50 text-indigo-700 dark:bg-indigo-900/20 dark:text-indigo-400 dark:border-indigo-500"
                      : "border-zinc-200 dark:border-zinc-600 text-zinc-600 dark:text-zinc-400 hover:bg-zinc-50 dark:hover:bg-zinc-700"
                  }`}
                >
                  <MessageSquare className="h-4 w-4" />
                  SMS
                </button>
              </div>
            </div>

            {/* Recipient */}
            <div>
              <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                {channel === "EMAIL" ? "Email Address" : "Phone Number"}
              </label>
              <input
                type={channel === "EMAIL" ? "email" : "tel"}
                value={recipient}
                onChange={(e) => setRecipient(e.target.value)}
                className="w-full rounded-lg border border-zinc-200 dark:border-zinc-600 bg-white dark:bg-zinc-700 px-3 py-2 text-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                placeholder={
                  channel === "EMAIL" ? "john@example.com" : "+1 (555) 123-4567"
                }
              />
            </div>

            {/* Preview */}
            <div className="rounded-lg bg-zinc-50 dark:bg-zinc-700/30 border border-zinc-200 dark:border-zinc-600 p-3">
              <div className="text-xs text-zinc-500 mb-1.5">
                Message preview
              </div>
              <div className="text-xs text-zinc-600 dark:text-zinc-400 leading-relaxed">
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
              <div className="rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 p-3">
                <p className="text-sm text-red-600 dark:text-red-400">
                  {error}
                </p>
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-zinc-200 dark:border-zinc-600 px-4 py-2 text-sm font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-700 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={sendMutation.isPending}
                className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors"
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
