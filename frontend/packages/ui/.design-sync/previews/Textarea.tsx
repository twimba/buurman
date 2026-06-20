import { Textarea } from '@buurman/ui';

export function States() {
  return (
    <div className="flex w-96 flex-col gap-3">
      <Textarea
        rows={3}
        placeholder="Add a note about this tenant…"
        onChange={() => {}}
      />
      <Textarea
        rows={3}
        defaultValue="Tenant reported a leaking faucet in the kitchen. Plumber scheduled for next Tuesday."
        onChange={() => {}}
      />
    </div>
  );
}

export function DisabledAndError() {
  return (
    <div className="flex w-96 flex-col gap-3">
      <Textarea
        rows={3}
        disabled
        defaultValue="Lease terminated on 31-12-2025. No further notes."
      />
      <div className="flex flex-col gap-1.5">
        <Textarea
          rows={3}
          error
          defaultValue=""
          placeholder="Reason is required"
          onChange={() => {}}
        />
        <span className="text-sm text-error-text">
          Please provide a reason for the deposit deduction
        </span>
      </div>
    </div>
  );
}
