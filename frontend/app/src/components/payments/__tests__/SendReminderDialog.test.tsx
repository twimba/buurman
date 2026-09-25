import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { SendReminderDialog } from '../SendReminderDialog';

function renderDialog(overrides: Partial<Parameters<typeof SendReminderDialog>[0]> = {}) {
  const onConfirm = vi.fn();
  const onClose = vi.fn();
  const result = renderWithProviders(
    <SendReminderDialog open count={1} onConfirm={onConfirm} onClose={onClose} {...overrides} />
  );
  return { ...result, onConfirm, onClose };
}

describe('SendReminderDialog', () => {
  it('offers the three tones with friendly selected by default', () => {
    renderDialog();
    const group = screen.getByRole('radiogroup');
    const radios = within(group).getAllByRole('radio');
    expect(radios).toHaveLength(3);
    expect(radios[0]).toHaveAccessibleName('Friendly');
    expect(radios[0]).toHaveAttribute('aria-checked', 'true');
    expect(radios[1]).toHaveAttribute('aria-checked', 'false');
    expect(radios[2]).toHaveAttribute('aria-checked', 'false');
    expect(screen.getByText(/polite nudge/i)).toBeInTheDocument();
  });

  it('explains the final notice when that tone is chosen', async () => {
    const user = userEvent.setup();
    renderDialog();
    await user.click(screen.getByRole('radio', { name: 'Final notice' }));
    expect(screen.getByRole('radio', { name: 'Final notice' })).toHaveAttribute(
      'aria-checked',
      'true'
    );
    expect(screen.getByText(/formal-notice PDF is attached/i)).toBeInTheDocument();
  });

  it('submits trimmed notes together with the selected tone', async () => {
    const user = userEvent.setup();
    const { onConfirm } = renderDialog();
    await user.click(screen.getByRole('radio', { name: 'Firm' }));
    await user.type(screen.getByLabelText(/personal message/i), '  Please pay by Friday.  ');
    await user.click(screen.getByRole('button', { name: 'Send reminder' }));
    expect(onConfirm).toHaveBeenCalledWith('Please pay by Friday.', 'FIRM');
  });

  it('submits undefined notes when the message is empty', async () => {
    const user = userEvent.setup();
    const { onConfirm } = renderDialog();
    await user.click(screen.getByRole('button', { name: 'Send reminder' }));
    expect(onConfirm).toHaveBeenCalledWith(undefined, 'FRIENDLY');
  });

  it('uses bulk wording when several payments are selected', () => {
    renderDialog({ count: 3 });
    expect(screen.getByRole('button', { name: 'Send 3 reminders' })).toBeInTheDocument();
    expect(screen.getByText(/without a tenant email address are skipped/i)).toBeInTheDocument();
  });

  it('closes without confirming on cancel', async () => {
    const user = userEvent.setup();
    const { onConfirm, onClose } = renderDialog();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onClose).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
