import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BellRing, Eye, Loader2, Plus, Save, Trash2 } from 'lucide-react';
import { useTeam } from '../../context/TeamContext';
import {
  useCurrentTeam,
  useReminderSettings,
  useUpdateReminderSettings,
} from '@/hooks/useTeamHooks';
import type {
  PaymentReminderStep,
  ReminderSettingsResponse,
  ReminderTone,
} from '../../generated/models';
import { ReminderPreviewModal } from './ReminderPreviewModal';

const TONES: ReminderTone[] = ['FRIENDLY', 'FIRM', 'FINAL'];

const DEFAULT_LADDER: PaymentReminderStep[] = [
  { offsetDays: -3, tone: 'FRIENDLY', enabled: true },
  { offsetDays: 3, tone: 'FRIENDLY', enabled: true },
  { offsetDays: 14, tone: 'FIRM', enabled: true },
  { offsetDays: 30, tone: 'FINAL', enabled: true },
];

/**
 * Team-level dunning ladder. Automatic reminders are off by default and, even when on, only
 * reach tenants whose contract or contact has reminders explicitly enabled.
 */
export const TenantRemindersSection = () => {
  const { t } = useTranslation('settings');
  const { canEditTeamSettings } = useTeam();
  const { data: team } = useCurrentTeam();
  const { data: settings, isLoading } = useReminderSettings(team?.identifier);
  const updateMutation = useUpdateReminderSettings(team?.identifier ?? '');

  const [draft, setDraft] = useState<ReminderSettingsResponse | null>(null);
  const [previewStep, setPreviewStep] = useState<PaymentReminderStep | null>(
    null
  );
  const [synced, setSynced] = useState<ReminderSettingsResponse | undefined>(
    undefined
  );
  if (settings && settings !== synced) {
    setSynced(settings);
    setDraft({
      automaticRemindersEnabled: settings.automaticRemindersEnabled,
      steps: [...settings.steps],
    });
  }

  const hasChanges =
    !!draft &&
    !!settings &&
    JSON.stringify(draft) !==
      JSON.stringify({
        automaticRemindersEnabled: settings.automaticRemindersEnabled,
        steps: settings.steps,
      });

  const updateStep = (index: number, patch: Partial<PaymentReminderStep>) => {
    setDraft((prev) =>
      prev
        ? {
            ...prev,
            steps: prev.steps.map((s, i) =>
              i === index ? { ...s, ...patch } : s
            ),
          }
        : prev
    );
  };

  const removeStep = (index: number) => {
    setDraft((prev) =>
      prev ? { ...prev, steps: prev.steps.filter((_, i) => i !== index) } : prev
    );
  };

  const addStep = () => {
    setDraft((prev) => {
      if (!prev) {
        return prev;
      }
      const last = prev.steps[prev.steps.length - 1];
      const next: PaymentReminderStep = {
        offsetDays: last ? last.offsetDays + 7 : 3,
        tone: last?.tone === 'FINAL' ? 'FINAL' : last ? 'FIRM' : 'FRIENDLY',
        enabled: true,
      };
      return { ...prev, steps: [...prev.steps, next] };
    });
  };

  const useDefaults = () => {
    setDraft((prev) =>
      prev ? { ...prev, steps: DEFAULT_LADDER.map((s) => ({ ...s })) } : prev
    );
  };

  const duplicateOffsets =
    draft !== null &&
    new Set(draft.steps.map((s) => s.offsetDays)).size !== draft.steps.length;

  const handleSave = () => {
    if (!draft || duplicateOffsets) {
      return;
    }
    updateMutation.mutate({
      automaticRemindersEnabled: draft.automaticRemindersEnabled,
      steps: [...draft.steps].sort((a, b) => a.offsetDays - b.offsetDays),
    });
  };

  if (isLoading || !draft) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500 dark:text-primary-300" />
      </div>
    );
  }

  const offsetLabel = (days: number) => {
    if (days < 0) {
      return t('tenantReminders.offset.before', { count: -days });
    }
    if (days === 0) {
      return t('tenantReminders.offset.onDue');
    }
    return t('tenantReminders.offset.after', { count: days });
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default">
      <div className="p-6 border-b border-border-default">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-start gap-3">
            <BellRing className="h-6 w-6 text-primary-500 dark:text-primary-300 mt-0.5" />
            <div>
              <h2 className="text-xl font-semibold text-text-primary">
                {t('tenantReminders.title')}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {t('tenantReminders.subtitle')}
              </p>
            </div>
          </div>
          {hasChanges && canEditTeamSettings && (
            <button
              onClick={handleSave}
              disabled={updateMutation.isPending || duplicateOffsets}
              className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              {updateMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Save className="h-4 w-4" />
              )}
              {t('common:buttons.saveChanges')}
            </button>
          )}
        </div>
      </div>

      <div className="p-6 space-y-6">
        <div className="rounded-md border border-info-border bg-info-bg px-4 py-3 text-sm text-info-text">
          {t('tenantReminders.consentNotice')}
        </div>

        {/* Master switch */}
        <div className="flex items-center justify-between gap-4">
          <div>
            <label
              htmlFor="automaticRemindersEnabled"
              className="text-sm font-medium text-text-primary"
            >
              {t('tenantReminders.automatic.label')}
            </label>
            <p className="text-sm text-text-secondary">
              {t('tenantReminders.automatic.description')}
            </p>
          </div>
          <button
            type="button"
            id="automaticRemindersEnabled"
            role="switch"
            aria-checked={draft.automaticRemindersEnabled}
            disabled={!canEditTeamSettings}
            onClick={() =>
              setDraft({
                ...draft,
                automaticRemindersEnabled: !draft.automaticRemindersEnabled,
              })
            }
            className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors focus-ring ${
              draft.automaticRemindersEnabled
                ? 'bg-primary-500'
                : 'bg-surface-inset'
            } ${canEditTeamSettings ? 'cursor-pointer' : 'opacity-50 cursor-not-allowed'}`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                draft.automaticRemindersEnabled
                  ? 'translate-x-6'
                  : 'translate-x-1'
              }`}
            />
          </button>
        </div>

        {/* Ladder */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-semibold text-text-primary">
              {t('tenantReminders.steps.title')}
            </h3>
            {canEditTeamSettings && draft.steps.length === 0 && (
              <button
                type="button"
                onClick={useDefaults}
                className="text-sm text-primary-600 dark:text-primary-300 hover:underline focus-ring rounded"
              >
                {t('tenantReminders.steps.useDefaults')}
              </button>
            )}
          </div>
          <p className="text-sm text-text-secondary mb-3">
            {t('tenantReminders.steps.description')}
          </p>

          {draft.steps.length === 0 ? (
            <p className="text-sm text-text-muted italic">
              {t('tenantReminders.steps.empty')}
            </p>
          ) : (
            <ul className="space-y-2">
              {draft.steps.map((step, index) => (
                <li
                  key={index}
                  className="flex flex-wrap items-center gap-3 rounded-md border border-border-default px-3 py-2"
                >
                  <input
                    type="checkbox"
                    checked={step.enabled}
                    disabled={!canEditTeamSettings}
                    onChange={(e) =>
                      updateStep(index, { enabled: e.target.checked })
                    }
                    aria-label={t('tenantReminders.steps.enabled')}
                    className="h-4 w-4 rounded border-border-strong text-primary-500 focus-ring"
                  />
                  <label className="flex items-center gap-2 text-sm text-text-secondary">
                    <span className="sr-only">
                      {t('tenantReminders.steps.offsetDays')}
                    </span>
                    <input
                      type="number"
                      min={-30}
                      max={365}
                      value={step.offsetDays}
                      disabled={!canEditTeamSettings}
                      onChange={(e) =>
                        updateStep(index, {
                          offsetDays: Number.parseInt(e.target.value || '0', 10),
                        })
                      }
                      className="w-20 border border-border-strong rounded px-2 py-1 text-text-primary bg-surface-card"
                    />
                    <span className="whitespace-nowrap">
                      {offsetLabel(step.offsetDays)}
                    </span>
                  </label>
                  <select
                    value={step.tone}
                    disabled={!canEditTeamSettings}
                    onChange={(e) =>
                      updateStep(index, { tone: e.target.value as ReminderTone })
                    }
                    aria-label={t('tenantReminders.steps.tone')}
                    className="border border-border-strong rounded px-2 py-1 text-sm text-text-primary bg-surface-card"
                  >
                    {TONES.map((tone) => (
                      <option key={tone} value={tone}>
                        {t(`tenantReminders.tones.${tone}`)}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => setPreviewStep(step)}
                    className="ml-auto inline-flex items-center gap-1.5 text-sm font-medium text-primary-600 dark:text-primary-300 hover:underline focus-ring rounded px-1.5 py-1"
                  >
                    <Eye className="h-4 w-4" />
                    {t('tenantReminders.steps.preview')}
                  </button>
                  {canEditTeamSettings && (
                    <button
                      type="button"
                      onClick={() => removeStep(index)}
                      aria-label={t('tenantReminders.steps.remove')}
                      className="p-1.5 rounded text-text-secondary hover:text-error-text hover:bg-error-bg focus-ring"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
          {duplicateOffsets && (
            <p className="text-error-text text-sm mt-2">
              {t('tenantReminders.steps.duplicateOffsets')}
            </p>
          )}
          {canEditTeamSettings && draft.steps.length < 10 && (
            <button
              type="button"
              onClick={addStep}
              className="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-primary-600 dark:text-primary-300 hover:underline focus-ring rounded"
            >
              <Plus className="h-4 w-4" />
              {t('tenantReminders.steps.add')}
            </button>
          )}
        </div>
      </div>

      {previewStep && (
        <ReminderPreviewModal
          open
          teamId={team?.identifier}
          tone={previewStep.tone}
          offsetDays={previewStep.offsetDays}
          onClose={() => setPreviewStep(null)}
        />
      )}
    </div>
  );
};
