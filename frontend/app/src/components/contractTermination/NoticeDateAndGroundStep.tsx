import { useTranslation } from 'react-i18next';

interface NoticeDateAndGroundStepProps {
  noticeDate: string;
  onNoticeDateChange: (noticeDate: string) => void;
  groundCode: string;
  onGroundCodeChange: (groundCode: string) => void;
  onNext: () => void;
  onBack: () => void;
}

export const NoticeDateAndGroundStep = ({
  noticeDate,
  onNoticeDateChange,
  groundCode,
  onGroundCodeChange,
  onNext,
  onBack,
}: NoticeDateAndGroundStepProps) => {
  const { t } = useTranslation('contracts');

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
        <div>
          <label
            htmlFor="notice-date"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('termination.noticeDateGround.noticeDateLabel')}
          </label>
          <input
            id="notice-date"
            type="date"
            value={noticeDate}
            onChange={(e) => onNoticeDateChange(e.target.value)}
            className="w-full max-w-xs px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
          />
          <p className="text-xs text-text-muted mt-1">
            {t('termination.noticeDateGround.noticeDateHelp')}
          </p>
        </div>

        <div>
          <label
            htmlFor="ground-code"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('termination.noticeDateGround.groundCodeLabel')}
          </label>
          <input
            id="ground-code"
            type="text"
            value={groundCode}
            onChange={(e) => onGroundCodeChange(e.target.value)}
            className="w-full max-w-xs px-3 py-2 border border-border-strong rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 bg-surface-card text-text-primary"
          />
          <p className="text-xs text-text-muted mt-1">
            {t('termination.noticeDateGround.groundCodeHelp')}
          </p>
        </div>
      </div>

      <div className="flex justify-between">
        <button
          onClick={onBack}
          className="px-6 py-2 rounded border border-border-default text-text-secondary hover:bg-surface-inset transition-colors"
        >
          {t('common:buttons.back')}
        </button>
        <button
          onClick={onNext}
          disabled={!noticeDate}
          className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {t('termination.noticeDateGround.next')}
        </button>
      </div>
    </div>
  );
};
