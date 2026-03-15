import { useState, useRef } from "react";
import { Shield } from "lucide-react";
import { ModalWrapper, Button } from "@buurman/ui";

interface PasswordConfirmationDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (password: string) => void;
  isLoading: boolean;
  error?: string | null;
  title?: string;
  subtitle?: string;
}

export const PasswordConfirmationDialog = ({
  open,
  onClose,
  onConfirm,
  isLoading,
  error,
  title = "Confirm your identity",
  subtitle = "Enter your password to continue with this privileged action.",
}: PasswordConfirmationDialogProps) => {
  const [password, setPassword] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  const handleClose = () => {
    setPassword("");
    onClose();
  };

  const handleSubmit = () => {
    if (password.trim()) {
      onConfirm(password);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !isLoading && password.trim()) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <ModalWrapper
      open={open}
      onClose={handleClose}
      title={title}
      subtitle={subtitle}
      size="sm"
      initialFocusRef={inputRef as React.RefObject<HTMLElement>}
      footer={
        <>
          <Button
            variant="secondary"
            onClick={handleClose}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            leftIcon={<Shield />}
            onClick={handleSubmit}
            disabled={isLoading || !password.trim()}
            isLoading={isLoading}
          >
            Confirm
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label
            htmlFor="password-confirm"
            className="block text-sm font-medium text-text-primary mb-1.5"
          >
            Password
          </label>
          <input
            ref={inputRef}
            id="password-confirm"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Enter your password"
            className="w-full rounded-lg border border-border-default bg-surface-card px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            autoComplete="current-password"
            disabled={isLoading}
          />
        </div>
        {error && <p className="text-sm text-error-text">{error}</p>}
      </div>
    </ModalWrapper>
  );
};
