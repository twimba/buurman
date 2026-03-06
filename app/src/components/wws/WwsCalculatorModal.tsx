import { useState, useMemo } from 'react';
import { Button } from '@/components/ui';
import { X, Calculator, ChevronRight, ChevronLeft, Save } from 'lucide-react';
import {
  useWwsPreFill,
  useCalculateWws,
  useCalculateAndSaveWws,
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

const ENERGY_LABELS = ['A++++', 'A+++', 'A++', 'A+', 'A', 'B', 'C', 'D', 'E', 'F', 'G'];

const PARKING_TYPES = [
  { value: '', label: 'None' },
  { value: 'GARAGE', label: 'Garage' },
  { value: 'CARPORT', label: 'Carport' },
  { value: 'OUTDOOR', label: 'Outdoor space' },
  { value: 'PERMIT', label: 'Parking permit' },
];

const CLASSIFICATION_STYLES: Record<string, { bg: string; text: string; label: string }> = {
  REGULATED: {
    bg: 'bg-green-100 dark:bg-green-900/30',
    text: 'text-green-800 dark:text-green-300',
    label: 'Regulated (Gereguleerd)',
  },
  MID_SEGMENT: {
    bg: 'bg-amber-100 dark:bg-amber-900/30',
    text: 'text-amber-800 dark:text-amber-300',
    label: 'Mid-Segment (Middenhuur)',
  },
  FREE_SECTOR: {
    bg: 'bg-red-100 dark:bg-red-900/30',
    text: 'text-red-800 dark:text-red-300',
    label: 'Free Sector (Vrije sector)',
  },
};

const inputClass =
  'w-full px-3 py-2 rounded-lg border border-[#c9cfd9] dark:border-[#3a3f54] bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] focus:outline-none focus:ring-2 focus:ring-[#5c7cfa] text-sm';

const labelClass =
  'block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1';

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
  systemVersion: '2024',
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

/** Inner component that remounts on each open via key, resetting all state. */
const WwsCalculatorModalInner = ({
  propertyIdentifier,
  onClose,
}: Omit<WwsCalculatorModalProps, 'isOpen'>) => {
  const { data: preFill, isLoading: preFillLoading } =
    useWwsPreFill(propertyIdentifier);

  const preFillForm = useMemo<FormState>(() => {
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
  }, [preFill]);

  const [step, setStep] = useState(1);
  const [form, setForm] = useState<FormState>(initialForm);
  const [formApplied, setFormApplied] = useState(false);
  const [result, setResult] = useState<WwsCalculationResponse | null>(null);

  // Apply preFill once it loads (only first time)
  if (preFill && !formApplied) {
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
    if (form.surfaceAreaSqm) { req.surfaceAreaSqm = parseFloat(form.surfaceAreaSqm); }
    if (form.numberOfRooms) { req.numberOfRooms = parseInt(form.numberOfRooms, 10); }
    if (form.numberOfHeatedRooms) { req.numberOfHeatedRooms = parseInt(form.numberOfHeatedRooms, 10); }
    if (form.energyLabel) { req.energyLabel = form.energyLabel; }
    if (form.kitchenQualityPoints) { req.kitchenQualityPoints = parseFloat(form.kitchenQualityPoints); }
    if (form.bathroomQualityPoints) { req.bathroomQualityPoints = parseFloat(form.bathroomQualityPoints); }
    if (form.wozValue) { req.wozValue = parseFloat(form.wozValue); }
    if (form.outdoorSpaceSqm) { req.outdoorSpaceSqm = parseFloat(form.outdoorSpaceSqm); }
    if (form.parkingType) { req.parkingType = form.parkingType; }
    if (form.parkingSpaces) { req.parkingSpaces = parseInt(form.parkingSpaces, 10); }
    if (form.locationBonus) { req.locationBonus = parseFloat(form.locationBonus); }
    if (form.renovationInvestment) { req.renovationInvestment = parseFloat(form.renovationInvestment); }
    if (form.accessibilityFeatures) { req.accessibilityFeatures = parseInt(form.accessibilityFeatures, 10); }
    if (form.commonAreaSqm) { req.commonAreaSqm = parseFloat(form.commonAreaSqm); }
    return req;
  };

  const runningTotal = useMemo(() => {
    // Simple estimate: just sum the numeric fields that directly map to points.
    // The actual backend calculates the real total. This is just a rough indicator.
    let total = 0;
    if (form.surfaceAreaSqm) { total += parseFloat(form.surfaceAreaSqm) * 1; }
    if (form.kitchenQualityPoints) { total += parseFloat(form.kitchenQualityPoints); }
    if (form.bathroomQualityPoints) { total += parseFloat(form.bathroomQualityPoints); }
    if (form.locationBonus) { total += parseFloat(form.locationBonus); }
    if (form.accessibilityFeatures) { total += parseFloat(form.accessibilityFeatures); }
    return Math.round(total * 10) / 10;
  }, [form]);

  const handleCalculate = () => {
    calculateMutation.mutate(buildRequest(), {
      onSuccess: (data) => {
        setResult(data);
        setStep(3);
      },
    });
  };

  const handleSave = () => {
    saveAndCalculateMutation.mutate(buildRequest(), {
      onSuccess: (data) => {
        setResult(data);
      },
    });
  };

  const classStyle = result
    ? CLASSIFICATION_STYLES[result.sectorClassification]
    : null;

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white dark:bg-[#14161f] rounded-xl p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Calculator className="h-5 w-5 text-[#5c7cfa]" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              WWS Points Calculator
            </h3>
          </div>
          <button
            onClick={onClose}
            className="text-[#6b7194] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Step Indicator */}
        <div className="flex items-center gap-2 mb-6">
          {[1, 2, 3].map((s) => (
            <div key={s} className="flex items-center gap-2">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  s === step
                    ? 'bg-[#5c7cfa] text-white'
                    : s < step
                      ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                      : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]'
                }`}
              >
                {s}
              </div>
              {s < 3 && (
                <div
                  className={`w-12 h-0.5 ${
                    s < step
                      ? 'bg-green-400 dark:bg-green-600'
                      : 'bg-[#e2e6f0] dark:bg-[#2a2e3f]'
                  }`}
                />
              )}
            </div>
          ))}
          <span className="ml-3 text-sm text-[#6b7194] dark:text-[#8b90a8]">
            {step === 1 && 'System Version'}
            {step === 2 && 'Category Inputs'}
            {step === 3 && 'Results'}
          </span>
        </div>

        {/* Step 1: System Version */}
        {step === 1 && (
          <div className="space-y-4">
            <div>
              <label className={labelClass}>System Version</label>
              <select
                value={form.systemVersion}
                onChange={(e) => updateField('systemVersion', e.target.value)}
                className={inputClass}
              >
                <option value="2024">2024</option>
              </select>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Woningwaarderingsstelsel version for the calculation.
              </p>
            </div>

            {preFillLoading ? (
              <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Loading property data...
              </div>
            ) : preFill?.propertyAddress ? (
              <div className="bg-[#f1f3f9] dark:bg-[#1e2130] rounded-lg p-4">
                <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide mb-1">
                  Property
                </div>
                <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  {preFill.propertyAddress}
                </div>
                <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-2">
                  Some fields will be pre-filled from property data.
                </div>
              </div>
            ) : null}

            <div className="flex justify-end pt-2">
              <Button variant="primary" onClick={() => setStep(2)}>
                Next
                <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          </div>
        )}

        {/* Step 2: Category Inputs */}
        {step === 2 && (
          <div className="space-y-5">
            {/* Running total banner */}
            <div className="bg-[#f1f3f9] dark:bg-[#1e2130] rounded-lg p-3 flex items-center justify-between">
              <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Estimated running total
              </span>
              <span className="text-lg font-bold text-[#5c7cfa]">
                ~{runningTotal} pts
              </span>
            </div>

            {/* Surface area & rooms */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <div>
                <label className={labelClass}>Surface Area (m2)</label>
                <input
                  type="number"
                  value={form.surfaceAreaSqm}
                  onChange={(e) => updateField('surfaceAreaSqm', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="0.1"
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>Number of Rooms</label>
                <input
                  type="number"
                  value={form.numberOfRooms}
                  onChange={(e) => updateField('numberOfRooms', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="1"
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>Heated Rooms</label>
                <input
                  type="number"
                  value={form.numberOfHeatedRooms}
                  onChange={(e) => updateField('numberOfHeatedRooms', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="1"
                  className={inputClass}
                />
              </div>
            </div>

            {/* Energy */}
            <div>
              <label className={labelClass}>Energy Label</label>
              <select
                value={form.energyLabel}
                onChange={(e) => updateField('energyLabel', e.target.value)}
                className={inputClass}
              >
                <option value="">Select...</option>
                {ENERGY_LABELS.map((label) => (
                  <option key={label} value={label}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            {/* Kitchen & Bathroom quality */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>Kitchen Quality Points</label>
                <input
                  type="number"
                  value={form.kitchenQualityPoints}
                  onChange={(e) => updateField('kitchenQualityPoints', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="0.5"
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>Bathroom Quality Points</label>
                <input
                  type="number"
                  value={form.bathroomQualityPoints}
                  onChange={(e) => updateField('bathroomQualityPoints', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="0.5"
                  className={inputClass}
                />
              </div>
            </div>

            {/* WOZ value */}
            <div>
              <label className={labelClass}>WOZ Value (EUR)</label>
              <input
                type="number"
                value={form.wozValue}
                onChange={(e) => updateField('wozValue', e.target.value)}
                placeholder="0"
                min="0"
                step="1000"
                className={inputClass}
              />
            </div>

            {/* Outdoor & Parking */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <div>
                <label className={labelClass}>Outdoor Space (m2)</label>
                <input
                  type="number"
                  value={form.outdoorSpaceSqm}
                  onChange={(e) => updateField('outdoorSpaceSqm', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="0.1"
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>Parking Type</label>
                <select
                  value={form.parkingType}
                  onChange={(e) => updateField('parkingType', e.target.value)}
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
                <label className={labelClass}>Parking Spaces</label>
                <input
                  type="number"
                  value={form.parkingSpaces}
                  onChange={(e) => updateField('parkingSpaces', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="1"
                  className={inputClass}
                />
              </div>
            </div>

            {/* Other points */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <div>
                <label className={labelClass}>Location Bonus</label>
                <input
                  type="number"
                  value={form.locationBonus}
                  onChange={(e) => updateField('locationBonus', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="0.5"
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>Renovation Investment (EUR)</label>
                <input
                  type="number"
                  value={form.renovationInvestment}
                  onChange={(e) => updateField('renovationInvestment', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="100"
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>Accessibility Features</label>
                <input
                  type="number"
                  value={form.accessibilityFeatures}
                  onChange={(e) => updateField('accessibilityFeatures', e.target.value)}
                  placeholder="0"
                  min="0"
                  step="1"
                  className={inputClass}
                />
              </div>
            </div>

            {/* Common area */}
            <div>
              <label className={labelClass}>Common Area (m2)</label>
              <input
                type="number"
                value={form.commonAreaSqm}
                onChange={(e) => updateField('commonAreaSqm', e.target.value)}
                placeholder="0"
                min="0"
                step="0.1"
                className={inputClass}
              />
            </div>

            {/* Actions */}
            <div className="flex justify-between pt-2">
              <Button variant="secondary" onClick={() => setStep(1)}>
                <ChevronLeft className="h-4 w-4 mr-1" />
                Back
              </Button>
              <Button
                variant="primary"
                onClick={handleCalculate}
                isLoading={calculateMutation.isPending}
              >
                <Calculator className="h-4 w-4 mr-1" />
                Calculate
              </Button>
            </div>
          </div>
        )}

        {/* Step 3: Results */}
        {step === 3 && result && (
          <div className="space-y-5">
            {/* Summary card */}
            <div className="bg-[#f1f3f9] dark:bg-[#1e2130] rounded-lg p-5">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <div className="text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wide">
                    Total Points
                  </div>
                  <div className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                    {result.totalPoints}
                  </div>
                </div>
                {classStyle && (
                  <span
                    className={`px-3 py-1.5 rounded-full text-sm font-semibold ${classStyle.bg} ${classStyle.text}`}
                  >
                    {classStyle.label}
                  </span>
                )}
              </div>
              {result.maxRentIndication != null && (
                <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Maximum rent indication:{' '}
                  <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                    EUR {result.maxRentIndication.toFixed(2)}
                  </span>
                </div>
              )}
              <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                System version {result.systemVersion} &middot;{' '}
                {new Date(result.calculationDate).toLocaleDateString()}
              </div>
            </div>

            {/* Breakdown */}
            <div>
              <h4 className="text-sm font-semibold text-[#3d4463] dark:text-[#c4c8db] mb-3">
                Breakdown
              </h4>
              <div className="space-y-2">
                {result.breakdown.map((cat) => (
                  <div
                    key={cat.key}
                    className="flex items-center justify-between bg-white dark:bg-[#0c0d14] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] px-4 py-2.5"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                        {cat.name}
                      </div>
                      <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] truncate">
                        {cat.nameNl}
                      </div>
                      {cat.explanation && (
                        <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-0.5">
                          {cat.explanation}
                        </div>
                      )}
                    </div>
                    <div
                      className={`ml-4 text-sm font-bold tabular-nums ${
                        cat.points > 0
                          ? 'text-[#5c7cfa]'
                          : 'text-[#6b7194] dark:text-[#8b90a8]'
                      }`}
                    >
                      {cat.points > 0 ? '+' : ''}
                      {cat.points}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-between pt-2">
              <Button variant="secondary" onClick={() => setStep(2)}>
                <ChevronLeft className="h-4 w-4 mr-1" />
                Adjust
              </Button>
              <div className="flex gap-3">
                <Button variant="secondary" onClick={onClose}>
                  Close
                </Button>
                {!result.identifier && (
                  <Button
                    variant="primary"
                    onClick={handleSave}
                    isLoading={saveAndCalculateMutation.isPending}
                  >
                    <Save className="h-4 w-4 mr-1" />
                    Save
                  </Button>
                )}
              </div>
            </div>
          </div>
        )}
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
