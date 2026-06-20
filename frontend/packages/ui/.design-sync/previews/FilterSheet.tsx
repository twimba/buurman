import { FilterSheet, FormField, Select } from '@buurman/ui';

// Rendered at the inline (md+) breakpoint so the card shows the actual filter
// form. Below the collapse breakpoint FilterSheet renders a `[⛕ Filters (n)]`
// trigger that opens a bottom sheet with this same JSX.
export function InlineFilters() {
  return (
    <FilterSheet
      activeCount={2}
      collapseBelow="md"
      inlineTitle="Filters"
      triggerLabel="Filters"
      onClear={() => {}}
    >
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Status" htmlFor="status">
          <Select id="status" defaultValue="overdue">
            <option value="all">All statuses</option>
            <option value="overdue">Overdue</option>
            <option value="paid">Paid</option>
          </Select>
        </FormField>
        <FormField label="Property" htmlFor="property">
          <Select id="property" defaultValue="maple">
            <option value="all">All properties</option>
            <option value="maple">Maple Street 14</option>
            <option value="kerk">Kerkstraat 8</option>
          </Select>
        </FormField>
      </div>
    </FilterSheet>
  );
}
