import { ChevronLeft, ChevronRight, Save } from 'lucide-react';
import {
  type ReactNode,
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useTranslation } from 'react-i18next';

interface StepperContextValue {
  isPhone: boolean;
  currentStep: number;
  totalSteps: number;
  go: (step: number) => void;
  next: () => void;
  prev: () => void;
}

const StepperContext = createContext<StepperContextValue | null>(null);

export function useMobileFormStepper(): StepperContextValue {
  const ctx = useContext(StepperContext);
  if (!ctx) {
    throw new Error(
      'useMobileFormStepper must be used inside <MobileFormStepperProvider>'
    );
  }
  return ctx;
}

export interface MobileFormStepperProviderProps {
  totalSteps: number;
  children: ReactNode;
  /** Below `md` (768 px) by default. Pass another query for testing. */
  phoneQuery?: string;
}

/**
 * Provides step state to <FormStepGate> and <FormStepperFooter> children.
 * Above `md`, isPhone is false and stepping is inert — every <FormStepGate>
 * renders its children so the desktop layout remains the single-scroll form.
 */
export function MobileFormStepperProvider({
  totalSteps,
  children,
  phoneQuery = '(max-width: 767px)',
}: MobileFormStepperProviderProps) {
  const [isPhone, setIsPhone] = useState(() =>
    typeof window !== 'undefined'
      ? window.matchMedia(phoneQuery).matches
      : false
  );
  const [currentStep, setCurrentStep] = useState(0);

  useEffect(() => {
    const mq = window.matchMedia(phoneQuery);
    const handler = (e: MediaQueryListEvent) => setIsPhone(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [phoneQuery]);

  const go = useCallback(
    (step: number) => {
      setCurrentStep(Math.max(0, Math.min(totalSteps - 1, step)));
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    },
    [totalSteps]
  );
  const next = useCallback(() => go(currentStep + 1), [currentStep, go]);
  const prev = useCallback(() => go(currentStep - 1), [currentStep, go]);

  const value = useMemo(
    () => ({ isPhone, currentStep, totalSteps, go, next, prev }),
    [isPhone, currentStep, totalSteps, go, next, prev]
  );

  return (
    <StepperContext.Provider value={value}>
      {children}
    </StepperContext.Provider>
  );
}

/**
 * Renders progress pips for the current step. Phone-only.
 */
export function MobileStepperHeader({
  labels,
  className = '',
}: {
  labels?: string[];
  className?: string;
}) {
  const { isPhone, currentStep, totalSteps } = useMobileFormStepper();
  if (!isPhone) {
    return null;
  }
  const label = labels?.[currentStep];
  return (
    <div className={`md:hidden mb-4 ${className}`}>
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium text-text-secondary">
          Step {currentStep + 1} of {totalSteps}
        </span>
        {label && (
          <span className="text-xs text-text-secondary truncate ml-2">
            {label}
          </span>
        )}
      </div>
      <div className="flex items-center gap-1.5">
        {Array.from({ length: totalSteps }).map((_, i) => (
          <span
            key={i}
            className={`h-1.5 flex-1 rounded-full ${
              i <= currentStep
                ? 'bg-primary-500'
                : 'bg-surface-inset border border-border-default'
            }`}
          />
        ))}
      </div>
    </div>
  );
}

/**
 * Hides children on phone unless `step === currentStep`. On `md+` always renders.
 */
export function FormStepGate({
  step,
  children,
}: {
  step: number;
  children: ReactNode;
}) {
  const { isPhone, currentStep } = useMobileFormStepper();
  if (isPhone && step !== currentStep) {
    return null;
  }
  return <>{children}</>;
}

export interface MobileFormStepperFooterProps {
  /** Called when the user presses Save on the last step. */
  onSubmit: () => void;
  /** Disables Save (e.g. while saving). */
  isSubmitting?: boolean;
  /** Label override for the final-step Save button. */
  saveLabel?: string;
  /** Label override for the Continue button. */
  continueLabel?: string;
  /** Optional Cancel button — shown only on the first step (in place of Back). */
  onCancel?: () => void;
}

/**
 * Sticky bottom action bar with Back/Continue or Save. Phone-only.
 * Reserves the iOS safe-area-inset-bottom and ducks above the keyboard
 * via the global --kbd-inset CSS variable.
 */
export function MobileFormStepperFooter({
  onSubmit,
  isSubmitting,
  saveLabel,
  continueLabel,
  onCancel,
}: MobileFormStepperFooterProps) {
  const { isPhone, currentStep, totalSteps, next, prev } =
    useMobileFormStepper();
  const { t } = useTranslation('common');
  if (!isPhone) {
    return null;
  }
  const isLast = currentStep === totalSteps - 1;
  const isFirst = currentStep === 0;
  return (
    <>
      {/* Spacer so the form content doesn't end behind the sticky footer. */}
      <div aria-hidden className="h-20" />
      <div
        className="md:hidden fixed inset-x-0 z-30 bg-surface-card/95 backdrop-blur-xl border-t border-border-default flex items-center gap-2 px-4 py-3"
        style={{
          bottom:
            'calc(var(--kbd-inset, 0px) + var(--safe-bottom, 0px) + var(--bottomnav-h, 0px))',
        }}
      >
        {isFirst ? (
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 inline-flex items-center justify-center gap-2 min-h-touch px-4 rounded-lg border border-border-strong text-text-secondary hover:bg-surface-inset"
          >
            {t('buttons.cancel', 'Cancel')}
          </button>
        ) : (
          <button
            type="button"
            onClick={prev}
            className="flex-1 inline-flex items-center justify-center gap-2 min-h-touch px-4 rounded-lg border border-border-strong text-text-secondary hover:bg-surface-inset"
          >
            <ChevronLeft className="h-4 w-4" />
            {t('buttons.back', 'Back')}
          </button>
        )}
        {isLast ? (
          <button
            type="submit"
            onClick={onSubmit}
            disabled={isSubmitting}
            className="flex-1 inline-flex items-center justify-center gap-2 min-h-touch px-4 rounded-lg bg-primary-500 text-white hover:bg-primary-600 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {saveLabel ?? t('buttons.save', 'Save')}
          </button>
        ) : (
          <button
            type="button"
            onClick={next}
            className="flex-1 inline-flex items-center justify-center gap-2 min-h-touch px-4 rounded-lg bg-primary-500 text-white hover:bg-primary-600"
          >
            {continueLabel ?? t('buttons.continue', 'Continue')}
            <ChevronRight className="h-4 w-4" />
          </button>
        )}
      </div>
    </>
  );
}
