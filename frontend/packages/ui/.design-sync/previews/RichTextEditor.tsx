import { RichTextEditor } from '@buurman/ui';

export function LeaseNote() {
  return (
    <div className="max-w-2xl">
      <RichTextEditor
        value={
          '<h2>Move-in inspection — Keizersgracht 124</h2>' +
          '<p>Tenant <strong>Sanne de Vries</strong> took possession on 1 March 2026. ' +
          'Keys handed over: 2 front-door, 1 mailbox, 1 storage.</p>' +
          '<ul>' +
          '<li>Kitchen appliances tested and working</li>' +
          '<li>Minor scuff on hallway wall noted</li>' +
          '<li>Deposit of <strong>€ 2.775,00</strong> received via SEPA</li>' +
          '</ul>'
        }
        onChange={() => {}}
      />
    </div>
  );
}

export function MaintenanceLog() {
  return (
    <div className="max-w-2xl">
      <RichTextEditor
        value={
          '<p>Boiler service scheduled for <strong>14 June 2026</strong> at ' +
          '<em>Oudegracht 210</em>. Vendor: VanderHeijden Installatie.</p>' +
          '<blockquote>Tenant reported intermittent hot water. ' +
          'Priority: high.</blockquote>'
        }
        onChange={() => {}}
      />
    </div>
  );
}

export function EmptyPlaceholder() {
  return (
    <div className="max-w-2xl">
      <RichTextEditor
        value=""
        onChange={() => {}}
        placeholder="Add a note about this contract…"
      />
    </div>
  );
}
