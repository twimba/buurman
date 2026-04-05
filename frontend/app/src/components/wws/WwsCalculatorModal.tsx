import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@buurman/ui';
import {
  X,
  Calculator,
  ChevronLeft,
  Save,
  Check,
  Home,
  Flame,
  Euro,
  TreePine,
  Accessibility,
  Info,
} from 'lucide-react';
import {
  useWwsPreFill,
  useCalculateWws,
  useCalculateAndSaveWws,
  useLatestWwsCalculation,
} from '@/hooks/useWwsHooks';
import type {
  WwsCalculationRequest,
  WwsCalculationResponse,
} from '@/types/wws';

interface WwsCalculatorModalProps {
  propertyIdentifier: string;
  onClose: () => void;
  isOpen: boolean;
}

const ENERGY_LABELS = [
  'A++++',
  'A+++',
  'A++',
  'A+',
  'A',
  'B',
  'C',
  'D',
  'E',
  'F',
  'G',
];

const PARKING_TYPES = [
  { value: '', label: 'None' },
  { value: 'GARAGE', label: 'Garage' },
  { value: 'CARPORT', label: 'Carport' },
  { value: 'OUTDOOR', label: 'Outdoor space' },
  { value: 'PERMIT', label: 'Parking permit' },
];

const CLASSIFICATION_STYLES: Record<
  string,
  { bg: string; text: string; label: string; border: string }
> = {
  REGULATED: {
    bg: 'bg-success-bg',
    text: 'text-success-text',
    border: 'border-success-border',
    label: 'Regulated (Gereguleerd)',
  },
  MID_SEGMENT: {
    bg: 'bg-warning-bg',
    text: 'text-warning-text',
    border: 'border-warning-border',
    label: 'Mid-Segment (Middenhuur)',
  },
  FREE_SECTOR: {
    bg: 'bg-error-bg',
    text: 'text-error-text',
    border: 'border-error-border',
    label: 'Free Sector (Vrije sector)',
  },
};

const inputClass =
  'w-full px-3 py-2 rounded-lg border border-border-strong bg-surface-card text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm';

const labelClass = 'block text-sm font-medium text-text-secondary mb-1';

const hintClass = 'text-xs text-text-secondary mt-1';

interface FormState {
  systemVersion: string;
  surfaceAreaSqm: string;
  numberOfRooms: string;
  numberOfHeatedRooms: string;
  energyLabel: string;
  kitchenQualityPoints: string;
  bathroomQualityPoints: string;
  wozValue: string;
  outdoorSpaceSqm: string;
  parkingType: string;
  parkingSpaces: string;
  locationBonus: string;
  renovationInvestment: string;
  accessibilityFeatures: string;
  commonAreaSqm: string;
}

const initialForm: FormState = {
  systemVersion: '2026',
  surfaceAreaSqm: '',
  numberOfRooms: '',
  numberOfHeatedRooms: '',
  energyLabel: '',
  kitchenQualityPoints: '',
  bathroomQualityPoints: '',
  wozValue: '',
  outdoorSpaceSqm: '',
  parkingType: '',
  parkingSpaces: '',
  locationBonus: '',
  renovationInvestment: '',
  accessibilityFeatures: '',
  commonAreaSqm: '',
};

const SectionHeader = ({
  icon: Icon,
  title,
}: {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
}) => (
  <div className="flex items-center gap-2 pb-2 border-b border-border-default">
    <Icon className="h-4 w-4 text-primary-500" />
    <h4 className="text-xs font-semibold text-text-secondary uppercase tracking-wide">
      {title}
    </h4>
  </div>
);

/** Inner component that remounts on each open via key, resetting all state. */
const WwsCalculatorModalInner = ({
  propertyIdentifier,
  onClose,
}: Omit<WwsCalculatorModalProps, 'isOpen'>) => {
  const { t } = useTranslation('common');
  const { data: preFill, isLoading: preFillLoading } =
    useWwsPreFill(propertyIdentifier);
  const { data: latestCalc, isLoading: latestLoading } =
    useLatestWwsCalculation(propertyIdentifier);

  const preFillForm = useMemo<FormState>(() => {
    const input = latestCalc?.inputData;
    if (input) {
      return {
        systemVersion: input.systemVersion ?? '2026',
        surfaceAreaSqm: input.surfaceAreaSqm?.toString() ?? '',
        numberOfRooms: input.numberOfRooms?.toString() ?? '',
        numberOfHeatedRooms: input.numberOfHeatedRooms?.toString() ?? '',
        energyLabel: input.energyLabel ?? '',
        kitchenQualityPoints: input.kitchenQualityPoints?.toString() ?? '',
        bathroomQualityPoints: input.bathroomQualityPoints?.toString() ?? '',
        wozValue: input.wozValue?.toString() ?? '',
        outdoorSpaceSqm: input.outdoorSpaceSqm?.toString() ?? '',
        parkingType: input.parkingType ?? '',
        parkingSpaces: input.parkingSpaces?.toString() ?? '',
        locationBonus: input.locationBonus?.toString() ?? '',
        renovationInvestment: input.renovationInvestment?.toString() ?? '',
        accessibilityFeatures: input.accessibilityFeatures?.toString() ?? '',
        commonAreaSqm: input.commonAreaSqm?.toString() ?? '',
      };
    }
    if (!preFill) {
      return initialForm;
    }
    return {
      ...initialForm,
      surfaceAreaSqm: preFill.surfaceAreaSqm?.toString() ?? '',
      numberOfRooms: preFill.numberOfRooms?.toString() ?? '',
      numberOfHeatedRooms: preFill.numberOfHeatedRooms?.toString() ?? '',
      energyLabel: preFill.energyLabel ?? '',
      outdoorSpaceSqm: preFill.outdoorSpaceSqm?.toString() ?? '',
      parkingType: preFill.parkingType ?? '',
      parkingSpaces: preFill.parkingSpaces?.toString() ?? '',
      accessibilityFeatures: preFill.accessibilityFeatures?.toString() ?? '',
    };
  }, [preFill, latestCalc]);

  const [step, setStep] = useState<'input' | 'results'>('input');
  const [form, setForm] = useState<FormState>(initialForm);
  const [formApplied, setFormApplied] = useState(false);
  const [result, setResult] = useState<WwsCalculationResponse | null>(null);
  const [saved, setSaved] = useState(false);

  // Apply form values once both queries settle (only first time)
  if (!preFillLoading && !latestLoading && !formApplied) {
    setForm(preFillForm);
    setFormApplied(true);
  }

  const calculateMutation = useCalculateWws();
  const saveAndCalculateMutation = useCalculateAndSaveWws();

  const updateField = (field: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const buildRequest = (): WwsCalculationRequest => {
    const req: WwsCalculationRequest = {
      systemVersion: form.systemVersion,
      propertyIdentifier,
    };
    if (form.surfaceAreaSqm) {
      req.surfaceAreaSqm = parseFloat(form.surfaceAreaSqm);
    }
    if (form.numberOfRooms) {
      req.numberOfRooms = parseInt(form.numberOfRooms, 10);
    }
    if (form.numberOfHeatedRooms) {
      req.numberOfHeatedRooms = parseInt(form.numberOfHeatedRooms, 10);
    }
    if (form.energyLabel) {
      req.energyLabel = form.energyLabel;
    }
    if (form.kitchenQualityPoints) {
      req.kitchenQualityPoints = parseFloat(form.kitchenQualityPoints);
    }
    if (form.bathroomQualityPoints) {
      req.bathroomQualityPoints = parseFloat(form.bathroomQualityPoints);
    }
    if (form.wozValue) {
      req.wozValue = parseFloat(form.wozValue);
    }
    if (form.outdoorSpaceSqm) {
      req.outdoorSpaceSqm = parseFloat(form.outdoorSpaceSqm);
    }
    if (form.parkingType) {
      req.parkingType = form.parkingType;
    }
    if (form.parkingSpaces) {
      req.parkingSpaces = parseInt(form.parkingSpaces, 10);
    }
    if (form.locationBonus) {
      req.locationBonus = parseFloat(form.locationBonus);
    }
    if (form.renovationInvestment) {
      req.renovationInvestment = parseFloat(form.renovationInvestment);
    }
    if (form.accessibilityFeatures) {
      req.accessibilityFeatures = parseInt(form.accessibilityFeatures, 10);
    }
    if (form.commonAreaSqm) {
      req.commonAreaSqm = parseFloat(form.commonAreaSqm);
    }
    return req;
  };

  const handleCalculate = () => {
    calculateMutation.mutate(buildRequest(), {
      onSuccess: (data) => {
        setResult(data);
        setSaved(false);
        setStep('results');
      },
    });
  };

  const handleSave = () => {
    saveAndCalculateMutation.mutate(buildRequest(), {
      onSuccess: (data) => {
        setResult(data);
        setSaved(true);
      },
    });
  };

  const classStyle = result
    ? CLASSIFICATION_STYLES[result.sectorClassification]
    : null;

  const maxPoints = result
    ? Math.max(...result.breakdown.map((c) => Math.abs(c.points)), 1)
    : 1;

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg max-w-2xl w-full mx-4 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border-default shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-primary-500/10 flex items-center justify-center">
              <Calculator className="h-4 w-4 text-primary-500" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-text-primary">
                {t('wws.title')}
              </h3>
              {preFill?.propertyAddress && (
                <p className="text-xs text-text-secondary">
                  {preFill.propertyAddress}
                </p>
              )}
            </div>
          </div>
          <div className="flex items-center gap-3">
            <select
              value={form.systemVersion}
              onChange={(e) => updateField('systemVersion', e.target.value)}
              className="text-xs px-2 py-1 rounded border border-border-strong bg-surface-card text-text-secondary focus:outline-none"
            >
              <option value="2026">v2026</option>
              <option value="2025">v2025</option>
              <option value="2024">v2024</option>
              <option value="2023">v2023</option>
            </select>
            <button
              onClick={onClose}
              className="text-text-secondary hover:text-text-primary transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Scrollable body */}
        <div className="overflow-y-auto flex-1 px-6 py-5">
          {/* Input step */}
          {step === 'input' && (
            <div className="space-y-6">
              {preFillLoading || latestLoading ? (
                <div className="flex items-center justify-center gap-2 py-16 text-sm text-text-secondary">
                  <div className="h-4 w-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
                  {t('wws.loadingPropertyData')}
                </div>
              ) : (
                <>
                  {preFill && (
                    <div className="flex items-start gap-2 bg-primary-500/5 dark:bg-primary-500/10 rounded-lg px-3 py-2.5">
                      <Info className="h-4 w-4 text-primary-500 mt-0.5 shrink-0" />
                      <p className="text-xs text-primary-500">
                        {t('wws.preFillHint')}
                      </p>
                    </div>
                  )}

                  {/* Property & Rooms */}
                  <div className="space-y-3">
                    <SectionHeader
                      icon={Home}
                      title={t('wws.sections.propertyRooms')}
                    />
                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.surfaceArea')} (m{'\u00B2'})
                          {preFill?.surfaceAreaSqm != null && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <input
                          type="number"
                          value={form.surfaceAreaSqm}
                          onChange={(e) =>
                            updateField('surfaceAreaSqm', e.target.value)
                          }
                          placeholder="e.g. 65"
                          min="0"
                          step="0.1"
                          className={inputClass}
                        />
                        <p className={hintClass}>Usable floor area (GBO)</p>
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.rooms')}
                          {preFill?.numberOfRooms != null && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <input
                          type="number"
                          value={form.numberOfRooms}
                          onChange={(e) =>
                            updateField('numberOfRooms', e.target.value)
                          }
                          placeholder="e.g. 3"
                          min="0"
                          step="1"
                          className={inputClass}
                        />
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.heatedRooms')}
                          {preFill?.numberOfHeatedRooms != null && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <input
                          type="number"
                          value={form.numberOfHeatedRooms}
                          onChange={(e) =>
                            updateField('numberOfHeatedRooms', e.target.value)
                          }
                          placeholder="e.g. 2"
                          min="0"
                          step="1"
                          className={inputClass}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Energy & Quality */}
                  <div className="space-y-3">
                    <SectionHeader
                      icon={Flame}
                      title={t('wws.sections.energyQuality')}
                    />
                    <div>
                      <label className={labelClass}>
                        {t('wws.fields.energyLabel')}
                        {preFill?.energyLabel && (
                          <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                        )}
                      </label>
                      <select
                        value={form.energyLabel}
                        onChange={(e) =>
                          updateField('energyLabel', e.target.value)
                        }
                        className={inputClass}
                      >
                        <option value="">
                          {t('wws.fields.selectEnergyLabel')}
                        </option>
                        {ENERGY_LABELS.map((label) => (
                          <option key={label} value={label}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.kitchenQuality')}
                        </label>
                        <input
                          type="number"
                          value={form.kitchenQualityPoints}
                          onChange={(e) =>
                            updateField('kitchenQualityPoints', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="0.5"
                          className={inputClass}
                        />
                        <p className={hintClass}>
                          Points from the official WWS kitchen checklist (0-44)
                        </p>
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.bathroomQuality')}
                        </label>
                        <input
                          type="number"
                          value={form.bathroomQualityPoints}
                          onChange={(e) =>
                            updateField('bathroomQualityPoints', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="0.5"
                          className={inputClass}
                        />
                        <p className={hintClass}>
                          Points from the official WWS bathroom checklist (0-44)
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Valuation */}
                  <div className="space-y-3">
                    <SectionHeader
                      icon={Euro}
                      title={t('wws.sections.valuationLocation')}
                    />
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.wozValue')}
                        </label>
                        <div className="relative">
                          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-text-secondary">
                            EUR
                          </span>
                          <input
                            type="number"
                            value={form.wozValue}
                            onChange={(e) =>
                              updateField('wozValue', e.target.value)
                            }
                            placeholder="e.g. 250000"
                            min="0"
                            step="1000"
                            className={`${inputClass} pl-12`}
                          />
                        </div>
                        <p className={hintClass}>
                          Latest WOZ-beschikking from the municipality
                        </p>
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.locationBonus')}
                        </label>
                        <input
                          type="number"
                          value={form.locationBonus}
                          onChange={(e) =>
                            updateField('locationBonus', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="0.5"
                          className={inputClass}
                        />
                        <p className={hintClass}>
                          Extra points for desirable neighbourhood
                        </p>
                      </div>
                    </div>
                    <div>
                      <label className={labelClass}>
                        Renovation investment
                      </label>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-text-secondary">
                          EUR
                        </span>
                        <input
                          type="number"
                          value={form.renovationInvestment}
                          onChange={(e) =>
                            updateField('renovationInvestment', e.target.value)
                          }
                          placeholder="e.g. 10000"
                          min="0"
                          step="100"
                          className={`${inputClass} pl-12`}
                        />
                      </div>
                      <p className={hintClass}>
                        Investment in the last 5 years (qualifying improvements)
                      </p>
                    </div>
                  </div>

                  {/* Outdoor & Parking */}
                  <div className="space-y-3">
                    <SectionHeader
                      icon={TreePine}
                      title={t('wws.sections.outdoorParking')}
                    />
                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.outdoorSpace')} (m{'\u00B2'})
                          {preFill?.outdoorSpaceSqm != null && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <input
                          type="number"
                          value={form.outdoorSpaceSqm}
                          onChange={(e) =>
                            updateField('outdoorSpaceSqm', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="0.1"
                          className={inputClass}
                        />
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.parkingType')}
                          {preFill?.parkingType && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <select
                          value={form.parkingType}
                          onChange={(e) =>
                            updateField('parkingType', e.target.value)
                          }
                          className={inputClass}
                        >
                          {PARKING_TYPES.map((pt) => (
                            <option key={pt.value} value={pt.value}>
                              {pt.label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.parkingSpaces')}
                          {preFill?.parkingSpaces != null && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <input
                          type="number"
                          value={form.parkingSpaces}
                          onChange={(e) =>
                            updateField('parkingSpaces', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="1"
                          className={inputClass}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Additional */}
                  <div className="space-y-3">
                    <SectionHeader
                      icon={Accessibility}
                      title={t('wws.sections.additional')}
                    />
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.accessibilityFeatures')}
                          {preFill?.accessibilityFeatures != null && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-primary-500 ml-1.5 align-middle" />
                          )}
                        </label>
                        <input
                          type="number"
                          value={form.accessibilityFeatures}
                          onChange={(e) =>
                            updateField('accessibilityFeatures', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="1"
                          className={inputClass}
                        />
                        <p className={hintClass}>
                          Count of qualifying accessibility adaptations
                        </p>
                      </div>
                      <div>
                        <label className={labelClass}>
                          {t('wws.fields.commonArea')} (m{'\u00B2'})
                        </label>
                        <input
                          type="number"
                          value={form.commonAreaSqm}
                          onChange={(e) =>
                            updateField('commonAreaSqm', e.target.value)
                          }
                          placeholder="0"
                          min="0"
                          step="0.1"
                          className={inputClass}
                        />
                        <p className={hintClass}>
                          Shared spaces allocated to this unit
                        </p>
                      </div>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* Results step */}
          {step === 'results' && result && (
            <div className="space-y-5">
              {/* Summary card */}
              <div
                className={`rounded-lg p-5 border ${classStyle ? `${classStyle.bg} ${classStyle.border}` : 'bg-surface-inset border-border-default '}`}
              >
                <div className="flex items-start justify-between">
                  <div>
                    <div className="text-xs font-medium text-text-secondary uppercase tracking-wide">
                      {t('wws.totalPoints')}
                    </div>
                    <div className="text-3xl font-bold text-text-primary mt-0.5">
                      {result.totalPoints}
                    </div>
                    {result.maxRentIndication != null && (
                      <div className="text-sm text-text-secondary mt-1">
                        {t('wws.maxRent')}{' '}
                        <span className="font-semibold text-text-primary">
                          EUR {result.maxRentIndication.toFixed(2)}
                        </span>
                        /month
                      </div>
                    )}
                  </div>
                  {classStyle && (
                    <span
                      className={`px-3 py-1.5 rounded-full text-sm font-semibold border ${classStyle.bg} ${classStyle.text} ${classStyle.border}`}
                    >
                      {classStyle.label}
                    </span>
                  )}
                </div>
                <div className="text-xs text-text-secondary mt-3">
                  WWS v{result.systemVersion} &middot;{' '}
                  {new Date(result.calculationDate).toLocaleDateString()}
                </div>
              </div>

              {/* Breakdown */}
              <div>
                <h4 className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-3">
                  {t('wws.pointsBreakdown')}
                </h4>
                <div className="space-y-1.5">
                  {result.breakdown
                    .slice()
                    .sort((a, b) => b.points - a.points)
                    .map((cat) => (
                      <div
                        key={cat.key}
                        className="group bg-surface-card rounded-lg border border-border-default px-4 py-2.5"
                      >
                        <div className="flex items-center justify-between">
                          <div className="min-w-0 flex-1">
                            <div className="flex items-baseline gap-2">
                              <span className="text-sm font-medium text-text-primary">
                                {cat.name}
                              </span>
                              <span className="text-xs text-text-secondary">
                                {cat.nameNl}
                              </span>
                            </div>
                            {cat.explanation && (
                              <p className="text-xs text-text-secondary mt-0.5">
                                {cat.explanation}
                              </p>
                            )}
                          </div>
                          <div
                            className={`ml-4 text-sm font-bold tabular-nums shrink-0 ${
                              cat.points > 0
                                ? 'text-primary-500'
                                : cat.points < 0
                                  ? 'text-error-text'
                                  : 'text-text-secondary'
                            }`}
                          >
                            {cat.points > 0 ? '+' : ''}
                            {cat.points}
                          </div>
                        </div>
                        {/* Proportional bar */}
                        {cat.points !== 0 && (
                          <div className="mt-2 h-1 rounded-full bg-surface-inset overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all ${
                                cat.points > 0
                                  ? 'bg-primary-500/40'
                                  : 'bg-error-text/40'
                              }`}
                              style={{
                                width: `${(Math.abs(cat.points) / maxPoints) * 100}%`,
                              }}
                            />
                          </div>
                        )}
                      </div>
                    ))}
                </div>
              </div>

              {/* Saved confirmation */}
              {saved && (
                <div className="flex items-center gap-2 bg-success-bg border border-success-border rounded-lg px-3 py-2.5">
                  <Check className="h-4 w-4 text-success-text shrink-0" />
                  <p className="text-sm text-success-text">
                    {t('wws.calculationSaved')}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-border-default shrink-0">
          {step === 'input' && (
            <div className="flex justify-end">
              <Button
                variant="primary"
                onClick={handleCalculate}
                isLoading={calculateMutation.isPending}
                leftIcon={<Calculator className="h-4 w-4" />}
              >
                Calculate Points
              </Button>
            </div>
          )}
          {step === 'results' && (
            <div className="flex justify-between">
              <Button
                variant="ghost"
                onClick={() => setStep('input')}
                leftIcon={<ChevronLeft className="h-4 w-4" />}
              >
                Adjust Inputs
              </Button>
              <div className="flex gap-2">
                {saved ? (
                  <Button variant="secondary" onClick={onClose}>
                    Done
                  </Button>
                ) : (
                  <>
                    <Button variant="secondary" onClick={onClose}>
                      {t('common:buttons.close')}
                    </Button>
                    {!result?.identifier && (
                      <Button
                        variant="primary"
                        onClick={handleSave}
                        isLoading={saveAndCalculateMutation.isPending}
                        leftIcon={<Save className="h-4 w-4" />}
                      >
                        {t('wws.saveResult')}
                      </Button>
                    )}
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/**
 * Outer wrapper: renders nothing when closed, remounts inner component
 * on each open so all state resets cleanly without useEffect.
 */
export const WwsCalculatorModal = ({
  isOpen,
  propertyIdentifier,
  onClose,
}: WwsCalculatorModalProps) => {
  if (!isOpen) {
    return null;
  }
  return (
    <WwsCalculatorModalInner
      key={propertyIdentifier}
      propertyIdentifier={propertyIdentifier}
      onClose={onClose}
    />
  );
};
