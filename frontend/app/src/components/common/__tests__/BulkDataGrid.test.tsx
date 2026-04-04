import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { BulkDataGrid, type BulkColumnDef } from '../BulkDataGrid';

const columns: BulkColumnDef[] = [
  { key: 'name', label: 'Name', type: 'text', required: true, placeholder: 'Enter name' },
  { key: 'amount', label: 'Amount', type: 'number', required: true, placeholder: '0.00' },
  { key: 'date', label: 'Date', type: 'date', required: false },
];

function renderGrid(overrides: Partial<Parameters<typeof BulkDataGrid>[0]> = {}) {
  const onSubmit = vi.fn();
  const result = renderWithProviders(
    <BulkDataGrid
      columns={columns}
      onSubmit={onSubmit}
      isSubmitting={false}
      {...overrides}
    />
  );
  return { ...result, onSubmit };
}

describe('BulkDataGrid', () => {
  it('renders with initial empty row', () => {
    renderGrid();

    // Should have column headers
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Amount')).toBeInTheDocument();
    expect(screen.getByText('Date')).toBeInTheDocument();

    // Should have one row with empty inputs (row number "1")
    const inputs = screen.getAllByRole('textbox');
    expect(inputs.length).toBeGreaterThanOrEqual(1);

    // Stats should show 0 rows (non-empty)
    expect(screen.getByText('0 rows')).toBeInTheDocument();
  });

  it('renders column headers from column definitions', () => {
    renderGrid();

    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Amount')).toBeInTheDocument();
    expect(screen.getByText('Date')).toBeInTheDocument();

    // Required columns show asterisk (rendered as *)
    // The header cell for Name should contain the asterisk span
    const nameHeader = screen.getByText('Name').closest('th');
    expect(nameHeader).toHaveTextContent('Name*');
  });

  it('adds a new row when "Add row" is clicked', async () => {
    const user = userEvent.setup();
    renderGrid();

    const addButton = screen.getByRole('button', { name: /add row/i });
    await user.click(addButton);

    // Should now have inputs for 2 rows — each row has 3 text/number/date inputs
    // The date input is type="date" so it may not be a textbox. Check placeholders instead.
    const nameInputs = screen.getAllByPlaceholderText('Enter name');
    expect(nameInputs).toHaveLength(2);
  });

  it('can type in input cells', async () => {
    const user = userEvent.setup();
    renderGrid();

    const nameInput = screen.getByPlaceholderText('Enter name');
    await user.type(nameInput, 'John Doe');
    expect(nameInput).toHaveValue('John Doe');
  });

  it('clears all rows and resets to a single empty row', async () => {
    const user = userEvent.setup();
    renderGrid();

    // Type something so the row is non-empty, which reveals the "Clear all" button
    const nameInput = screen.getByPlaceholderText('Enter name');
    await user.type(nameInput, 'test');

    const clearButton = screen.getByRole('button', { name: /clear all/i });
    await user.click(clearButton);

    // Should be back to one empty row
    const nameInputs = screen.getAllByPlaceholderText('Enter name');
    expect(nameInputs).toHaveLength(1);
    expect(nameInputs[0]).toHaveValue('');
    expect(screen.getByText('0 rows')).toBeInTheDocument();
  });

  it('disables submit button when no valid rows exist', () => {
    renderGrid();

    const submitButton = screen.getByRole('button', { name: /submit/i });
    expect(submitButton).toBeDisabled();
  });
});
