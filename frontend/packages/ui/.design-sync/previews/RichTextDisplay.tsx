import { RichTextDisplay } from '@buurman/ui';

// NOTE: the `.rich-text-display` stylesheet lives in the consuming app, not in
// the UI package CSS pipeline. To keep these cards self-styled in capture, the
// HTML below carries inline Tailwind utility classes on each element.

const LEASE_NOTE = `
  <h2 class="text-lg font-semibold text-text-primary mb-2">Lease handover notes</h2>
  <p class="text-text-secondary mb-3">
    Keys handed over to <strong class="font-semibold text-text-primary">Jan de Vries</strong>
    on 1 March 2024. Deposit of <strong class="font-semibold text-text-primary">€1,650</strong>
    received and held in escrow.
  </p>
  <h3 class="text-base font-semibold text-text-primary mb-1">Outstanding items</h3>
  <ul style="list-style:disc;padding-left:1.5rem" class="text-text-secondary space-y-1 mb-3">
    <li>Replace kitchen tap (reported 12 Feb)</li>
    <li>Repaint hallway — scheduled week 14</li>
    <li>Service boiler before next winter</li>
  </ul>
  <p class="text-text-secondary">
    See the full
    <a href="#" class="text-text-link underline">inspection report</a>
    for photos.
  </p>
`;

const HOUSE_RULES = `
  <h2 class="text-lg font-semibold text-text-primary mb-2">House rules</h2>
  <ol style="list-style:decimal;padding-left:1.5rem" class="text-text-secondary space-y-1 mb-3">
    <li>No smoking inside the building.</li>
    <li>Quiet hours between <strong class="font-semibold text-text-primary">22:00</strong> and 07:00.</li>
    <li>Separate waste per Amsterdam recycling rules.</li>
  </ol>
  <blockquote class="border-l-4 border-border-strong pl-4 italic text-text-muted">
    Rent is due on the 1st of each month via SEPA direct debit.
  </blockquote>
`;

export function LeaseNote() {
  return (
    <div className="max-w-md bg-surface-card border border-border-default rounded-lg p-5">
      <RichTextDisplay content={LEASE_NOTE} />
    </div>
  );
}

export function HouseRules() {
  return (
    <div className="max-w-md bg-surface-card border border-border-default rounded-lg p-5">
      <RichTextDisplay html={HOUSE_RULES} />
    </div>
  );
}
