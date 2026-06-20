import { Input } from '@buurman/ui';

export function Sizes() {
  return (
    <div className="flex w-80 flex-col gap-3">
      <Input size="sm" defaultValue="Prinsengracht 263" onChange={() => {}} />
      <Input size="md" defaultValue="Keizersgracht 174" onChange={() => {}} />
      <Input size="lg" defaultValue="Herengracht 502" onChange={() => {}} />
    </div>
  );
}

export function States() {
  return (
    <div className="flex w-80 flex-col gap-3">
      <Input placeholder="Search tenants…" onChange={() => {}} />
      <Input defaultValue="€ 1.450,00" onChange={() => {}} />
      <Input disabled defaultValue="Amsterdam, 1017 PA" />
    </div>
  );
}

export function ErrorState() {
  return (
    <div className="flex w-80 flex-col gap-1.5">
      <span className="text-sm font-medium text-text-primary">IBAN</span>
      <Input error defaultValue="NL00 INGB 0000 0000" onChange={() => {}} />
      <span className="text-sm text-error-text">Invalid IBAN format</span>
    </div>
  );
}
