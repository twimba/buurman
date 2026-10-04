import { render, screen, within } from '@testing-library/react';
import { ChangeContractStatusModal } from '../ChangeContractStatusModal';
import { ContractStatus } from '@/types/contract';

// Guards the NOTICE_GIVEN -> TERMINATED transition offered by this modal. The backend's
// changeContractStatus endpoint deliberately rejects any request whose TARGET status is
// NOTICE_GIVEN (giving notice must go through the dedicated termination wizard), but leaving
// NOTICE_GIVEN for TERMINATED through this generic endpoint is still valid and allowed
// server-side (see ContractService#validateStatusTransition). So the modal must offer
// NOTICE_GIVEN -> TERMINATED, but never a transition INTO NOTICE_GIVEN.

describe('ChangeContractStatusModal valid transitions', () => {
  it('offers TERMINATED as the only transition for a NOTICE_GIVEN contract', () => {
    render(
      <ChangeContractStatusModal
        currentStatus={ContractStatus.NOTICE_GIVEN}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />
    );

    const select = screen.getByLabelText(/new status/i);
    const options = within(select).getAllByRole('option');
    expect(options).toHaveLength(1);
    expect(options[0]).toHaveTextContent('Terminated');
  });

  it('never offers NOTICE_GIVEN as a transition target from any status', () => {
    const statuses = [
      ContractStatus.DRAFT,
      ContractStatus.PENDING_SIGNATURE,
      ContractStatus.ACTIVE,
      ContractStatus.NOTICE_GIVEN,
      ContractStatus.EXPIRED,
      ContractStatus.TERMINATED,
    ];

    for (const currentStatus of statuses) {
      const { unmount, container } = render(
        <ChangeContractStatusModal
          currentStatus={currentStatus}
          onClose={vi.fn()}
          onConfirm={vi.fn()}
        />
      );
      const select = container.querySelector('select#newStatus');
      if (select) {
        const labels = Array.from(select.querySelectorAll('option')).map(
          (o) => o.textContent
        );
        expect(labels).not.toContain('Notice Given');
      }
      unmount();
    }
  });
});
