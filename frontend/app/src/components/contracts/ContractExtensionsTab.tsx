import { ExtensionTimeline } from '@/components/contracts/ExtensionTimeline';
import { ContractResponse } from '@/types/contract';

interface ContractExtensionsTabProps {
  contract: ContractResponse;
  contractId: string;
}

export const ContractExtensionsTab = ({
  contract,
  contractId,
}: ContractExtensionsTabProps) => {
  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <ExtensionTimeline
        contractIdentifier={contractId}
        contractStatus={contract.status}
        currency={contract.rentAmountCurrency}
        currentRentAmount={contract.rentAmount}
        currentEndDate={contract.effectiveEndDate ?? contract.endDate}
        countryCode={contract.countryCode}
        renewalTermMonths={contract.renewalTermMonths}
        rentAdjustmentType={contract.rentAdjustmentType}
        rentAdjustmentValue={contract.rentAdjustmentValue}
        documentLanguages={contract.documentLanguages}
      />
    </div>
  );
};
