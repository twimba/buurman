import { Select } from '@buurman/ui';

export function Sizes() {
  return (
    <div className="flex w-80 flex-col gap-3">
      <Select size="sm" defaultValue="apartment" onChange={() => {}}>
        <option value="apartment">Apartment</option>
        <option value="house">House</option>
        <option value="studio">Studio</option>
      </Select>
      <Select size="md" defaultValue="house" onChange={() => {}}>
        <option value="apartment">Apartment</option>
        <option value="house">House</option>
        <option value="studio">Studio</option>
      </Select>
      <Select size="lg" defaultValue="studio" onChange={() => {}}>
        <option value="apartment">Apartment</option>
        <option value="house">House</option>
        <option value="studio">Studio</option>
      </Select>
    </div>
  );
}

export function States() {
  return (
    <div className="flex w-80 flex-col gap-3">
      <Select defaultValue="active" onChange={() => {}}>
        <option value="active">Active</option>
        <option value="ended">Ended</option>
        <option value="pending">Pending</option>
      </Select>
      <Select disabled defaultValue="ended">
        <option value="ended">Ended</option>
      </Select>
      <div className="flex flex-col gap-1.5">
        <Select error defaultValue="" onChange={() => {}}>
          <option value="" disabled>
            Select a property…
          </option>
          <option value="p1">Prinsengracht 263</option>
        </Select>
        <span className="text-sm text-error-text">A property is required</span>
      </div>
    </div>
  );
}
