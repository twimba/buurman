import { FormField, Input, Select } from '@buurman/ui';

export function Default() {
  return (
    <div className="w-96">
      <FormField label="Monthly rent" htmlFor="rent" required hint="Amount in euros, excluding utilities">
        <Input id="rent" defaultValue="€ 1.450,00" onChange={() => {}} />
      </FormField>
    </div>
  );
}

export function WithError() {
  return (
    <div className="w-96">
      <FormField label="Email address" htmlFor="email" required error="Enter a valid email address">
        <Input id="email" error defaultValue="jan.devries@" onChange={() => {}} />
      </FormField>
    </div>
  );
}

export function LeftLabel() {
  return (
    <div className="w-[28rem]">
      <FormField label="Property type" htmlFor="type" labelPosition="left">
        <Select id="type" defaultValue="apartment" onChange={() => {}}>
          <option value="apartment">Apartment</option>
          <option value="house">House</option>
          <option value="studio">Studio</option>
        </Select>
      </FormField>
    </div>
  );
}

export function ReadOnly() {
  return (
    <div className="w-96">
      <FormField label="Tenant" htmlFor="tenant" readOnly hint="Assigned from the active lease">
        <Input id="tenant" disabled defaultValue="Sophie Bakker" />
      </FormField>
    </div>
  );
}
