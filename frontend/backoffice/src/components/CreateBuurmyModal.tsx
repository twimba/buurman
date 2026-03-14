import { useState, useEffect, useRef, forwardRef } from "react";
import { X, Loader2 } from "lucide-react";
import { AxiosError } from "axios";
import { useCreateBuurmy } from "../hooks/useBuurmies";

interface CreateBuurmyModalProps {
  onClose: () => void;
}

const getErrorMessage = (error: unknown): string => {
  if (error instanceof AxiosError && error.response?.data) {
    const data = error.response.data;
    return (
      data.detail || data.message || data.title || "Failed to create buurmy"
    );
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "Failed to create buurmy";
};

export const CreateBuurmyModal = ({ onClose }: CreateBuurmyModalProps) => {
  const createBuurmy = useCreateBuurmy();
  const firstInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState({
    email: "",
    username: "",
    firstName: "",
    lastName: "",
    password: "",
    temporaryPassword: true,
  });

  useEffect(() => {
    firstInputRef.current?.focus();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createBuurmy.mutate(
      {
        email: form.email,
        username: form.username || undefined,
        firstName: form.firstName,
        lastName: form.lastName,
        password: form.password,
        temporaryPassword: form.temporaryPassword,
      },
      { onSuccess: () => onClose() },
    );
  };

  const update = (field: string, value: string | boolean) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const isValid =
    form.email && form.firstName && form.lastName && form.password.length >= 8;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4 py-8">
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" />

        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-buurmy-title"
          className="relative bg-surface-card rounded-lg shadow-xl w-full max-w-lg"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-border-default">
            <h2
              id="create-buurmy-title"
              className="text-lg font-semibold text-text-primary"
            >
              Create Buurmy
            </h2>
            <button
              onClick={onClose}
              className="p-1 text-text-muted hover:text-text-secondary rounded transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit}>
            <div className="px-6 py-4 space-y-4">
              {/* Error */}
              {createBuurmy.isError && (
                <div className="p-3 rounded-lg bg-error-bg border border-error-border">
                  <p className="text-sm text-error-text">
                    {getErrorMessage(createBuurmy.error)}
                  </p>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <InputField
                  ref={firstInputRef}
                  label="First Name"
                  value={form.firstName}
                  onChange={(v) => update("firstName", v)}
                  required
                />
                <InputField
                  label="Last Name"
                  value={form.lastName}
                  onChange={(v) => update("lastName", v)}
                  required
                />
              </div>

              <InputField
                label="Email"
                type="email"
                value={form.email}
                onChange={(v) => update("email", v)}
                required
              />

              <InputField
                label="Username"
                value={form.username}
                onChange={(v) => update("username", v)}
                placeholder="Defaults to email if empty"
              />

              <InputField
                label="Password"
                type="password"
                value={form.password}
                onChange={(v) => update("password", v)}
                required
                minLength={8}
                hint="Minimum 8 characters"
              />

              {/* Temporary password toggle */}
              <label className="flex items-center gap-3 cursor-pointer">
                <div className="relative">
                  <input
                    type="checkbox"
                    checked={form.temporaryPassword}
                    onChange={(e) =>
                      update("temporaryPassword", e.target.checked)
                    }
                    className="sr-only peer"
                  />
                  <div className="w-9 h-5 bg-neutral-200 rounded-full peer-checked:bg-primary-500 transition-colors" />
                  <div className="absolute top-0.5 left-0.5 w-4 h-4 bg-surface-card rounded-full shadow peer-checked:translate-x-4 transition-transform" />
                </div>
                <div>
                  <span className="text-sm font-medium text-text-primary">
                    Temporary password
                  </span>
                  <p className="text-xs text-text-secondary">
                    User must change password on first login
                  </p>
                </div>
              </label>
            </div>

            {/* Footer */}
            <div className="px-6 py-4 border-t border-border-default flex justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                disabled={createBuurmy.isPending}
                className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!isValid || createBuurmy.isPending}
                className="px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-md transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {createBuurmy.isPending && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                Create Buurmy
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// --- Reusable input field ---

interface InputFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
  placeholder?: string;
  minLength?: number;
  hint?: string;
}

const InputField = forwardRef<HTMLInputElement, InputFieldProps>(
  function InputField(
    {
      label,
      value,
      onChange,
      type = "text",
      required = false,
      placeholder,
      minLength,
      hint,
    },
    ref,
  ) {
    return (
      <div>
        <label className="block text-sm font-medium text-text-secondary mb-1.5">
          {label}
          {required && <span className="text-error-text ml-0.5">*</span>}
        </label>
        <input
          ref={ref}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          required={required}
          minLength={minLength}
          placeholder={placeholder}
          className="w-full px-3 py-2 text-sm rounded-md border border-border-default bg-surface-card text-text-primary placeholder-text-muted focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-colors"
        />
        {hint && <p className="mt-1 text-xs text-text-muted">{hint}</p>}
      </div>
    );
  },
);
