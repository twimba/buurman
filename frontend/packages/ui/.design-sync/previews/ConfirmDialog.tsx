import type { CSSProperties } from 'react';
import { ConfirmDialog } from '@buurman/ui';

// ConfirmDialog renders with `position: fixed inset-0`. A wrapper with
// `transform` establishes a containing block so the fixed overlay resolves
// against the wrapper (and is captured in-frame) instead of escaping to the
// page viewport.
const stage: CSSProperties = {
  transform: 'translateZ(0)',
  position: 'relative',
  width: '100%',
  height: 520,
};

export function Danger() {
  return (
    <div style={stage}>
      <ConfirmDialog
        variant="danger"
        title="Delete payment"
        message="The €1.250 rent payment for Maple Street 14 will be permanently removed. This action cannot be undone."
        confirmLabel="Delete payment"
        cancelLabel="Keep"
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    </div>
  );
}

export function Default() {
  return (
    <div style={stage}>
      <ConfirmDialog
        title="Apply rent adjustment"
        message="The monthly rent for Kerkstraat 8 will increase from €1.100 to €1.155 starting 1 July 2026."
        confirmLabel="Apply"
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    </div>
  );
}
