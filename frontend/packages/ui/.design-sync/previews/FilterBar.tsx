import { FilterBar } from '@buurman/ui';
import type { FilterDef } from '@buurman/ui';

const paymentFilters: FilterDef[] = [
  {
    type: 'search',
    key: 'q',
    label: 'Search tenants',
    placeholder: 'Search by tenant or property…',
  },
  {
    type: 'select',
    key: 'status',
    label: 'Status',
    options: [
      { value: 'paid', label: 'Paid' },
      { value: 'pending', label: 'Pending' },
      { value: 'overdue', label: 'Overdue' },
    ],
  },
  {
    type: 'select',
    key: 'property',
    label: 'Property',
    options: [
      { value: 'p1', label: 'Keizersgracht 124' },
      { value: 'p2', label: 'Witte de Withstraat 7B' },
      { value: 'p3', label: 'Oudegracht 210' },
    ],
  },
];

export function PaymentFilters() {
  return (
    <div className="max-w-3xl">
      <FilterBar
        filters={paymentFilters}
        values={{ status: 'overdue' }}
        onChange={() => {}}
        onReset={() => {}}
      />
    </div>
  );
}

const propertyFilters: FilterDef[] = [
  {
    type: 'toggle',
    key: 'occupancy',
    label: 'Occupancy',
    options: [
      { value: undefined, label: 'All' },
      { value: 'occupied', label: 'Occupied' },
      { value: 'vacant', label: 'Vacant' },
    ],
  },
  {
    type: 'select',
    key: 'type',
    label: 'Type',
    options: [
      { value: 'apartment', label: 'Apartment' },
      { value: 'house', label: 'House' },
      { value: 'studio', label: 'Studio' },
    ],
  },
  {
    type: 'select',
    key: 'city',
    label: 'City',
    options: [
      { value: 'amsterdam', label: 'Amsterdam' },
      { value: 'rotterdam', label: 'Rotterdam' },
      { value: 'utrecht', label: 'Utrecht' },
    ],
  },
];

export function PropertyFilters() {
  return (
    <div className="max-w-3xl">
      <FilterBar
        filters={propertyFilters}
        values={{ occupancy: 'occupied', city: 'amsterdam' }}
        onChange={() => {}}
        onReset={() => {}}
      />
    </div>
  );
}
