import { useState, useMemo } from 'react';
import {
  Globe,
  Coins,
  Calendar,
  ArrowRight,
  ArrowLeft,
  Check,
} from 'lucide-react';
import {
  useCompleteOnboarding,
  useCountryCurrencies,
} from '@/hooks/useOnboarding';
import { getCountryByCode } from '@/utils/countries';
import { useCurrencies, getCurrencyByCode } from '@/hooks/useCurrencies';
import { getCurrencyFlag } from '@/utils/currencyFlags';

interface OnboardingWizardProps {
  onComplete: () => void;
  currentCountryCode: string;
  currentCurrency: string;
}

const STEPS = ['country', 'currency', 'dateFormat'] as const;

const DATE_FORMATS = [
  { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY', example: '25/03/2026' },
  { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY', example: '03/25/2026' },
  { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD', example: '2026-03-25' },
];

const COUNTRIES = [
  { code: 'NL', name: 'Netherlands' },
  { code: 'DE', name: 'Germany' },
  { code: 'FR', name: 'France' },
  { code: 'BE', name: 'Belgium' },
  { code: 'PT', name: 'Portugal' },
  { code: 'ES', name: 'Spain' },
  { code: 'IT', name: 'Italy' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'US', name: 'United States' },
  { code: 'AT', name: 'Austria' },
  { code: 'CH', name: 'Switzerland' },
  { code: 'DK', name: 'Denmark' },
  { code: 'SE', name: 'Sweden' },
  { code: 'FI', name: 'Finland' },
  { code: 'NO', name: 'Norway' },
  { code: 'IE', name: 'Ireland' },
  { code: 'PL', name: 'Poland' },
  { code: 'CZ', name: 'Czech Republic' },
  { code: 'HU', name: 'Hungary' },
  { code: 'RO', name: 'Romania' },
  { code: 'BG', name: 'Bulgaria' },
  { code: 'SK', name: 'Slovakia' },
  { code: 'SI', name: 'Slovenia' },
  { code: 'HR', name: 'Croatia' },
  { code: 'LT', name: 'Lithuania' },
  { code: 'LV', name: 'Latvia' },
  { code: 'EE', name: 'Estonia' },
  { code: 'GR', name: 'Greece' },
  { code: 'MT', name: 'Malta' },
  { code: 'CY', name: 'Cyprus' },
  { code: 'LU', name: 'Luxembourg' },
  { code: 'RS', name: 'Serbia' },
  { code: 'BA', name: 'Bosnia and Herzegovina' },
  { code: 'AL', name: 'Albania' },
  { code: 'ME', name: 'Montenegro' },
  { code: 'MK', name: 'North Macedonia' },
  { code: 'XK', name: 'Kosovo' },
  { code: 'CA', name: 'Canada' },
  { code: 'MX', name: 'Mexico' },
  { code: 'BR', name: 'Brazil' },
  { code: 'AR', name: 'Argentina' },
  { code: 'CL', name: 'Chile' },
  { code: 'CO', name: 'Colombia' },
  { code: 'PE', name: 'Peru' },
  { code: 'UY', name: 'Uruguay' },
];

const COMMON_CURRENCIES = [
  'EUR',
  'GBP',
  'USD',
  'CHF',
  'SEK',
  'NOK',
  'DKK',
  'PLN',
  'CZK',
  'HUF',
  'RON',
  'BGN',
  'CAD',
  'MXN',
  'BRL',
  'ARS',
  'CLP',
  'COP',
  'PEN',
  'UYU',
];

export const OnboardingWizard = ({
  onComplete,
  currentCountryCode,
  currentCurrency,
}: OnboardingWizardProps) => {
  const [stepIndex, setStepIndex] = useState(0);
  const [country, setCountry] = useState(currentCountryCode || 'NL');
  const [currency, setCurrency] = useState(currentCurrency || 'EUR');
  const [suggestedCurrency, setSuggestedCurrency] = useState(
    currentCurrency || 'EUR'
  );
  const [dateFormat, setDateFormat] = useState('DD/MM/YYYY');
  const [countrySearch, setCountrySearch] = useState('');

  const { data: countryCurrencies } = useCountryCurrencies();
  const { data: currencyList } = useCurrencies();
  const completeMutation = useCompleteOnboarding();

  const currentStep = STEPS[stepIndex];

  const filteredCountries = useMemo(() => {
    if (!countrySearch.trim()) {
      return COUNTRIES;
    }
    const lower = countrySearch.toLowerCase();
    return COUNTRIES.filter(
      (c) =>
        c.name.toLowerCase().includes(lower) ||
        c.code.toLowerCase().includes(lower)
    );
  }, [countrySearch]);

  const handleCountrySelect = (code: string) => {
    setCountry(code);
    if (countryCurrencies?.[code]) {
      const suggested = countryCurrencies[code];
      setSuggestedCurrency(suggested);
      setCurrency(suggested);
    }
  };

  const handleNext = () => {
    if (stepIndex < STEPS.length - 1) {
      setStepIndex(stepIndex + 1);
    }
  };

  const handleBack = () => {
    if (stepIndex > 0) {
      setStepIndex(stepIndex - 1);
    }
  };

  const handleComplete = () => {
    completeMutation.mutate(
      { countryCode: country, currency, dateFormat },
      { onSuccess: onComplete }
    );
  };

  const isLastStep = stepIndex === STEPS.length - 1;

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-[#14161f] rounded-2xl shadow-xl max-w-lg w-full overflow-hidden">
        {/* Header */}
        <div className="px-8 pt-8 pb-4">
          <h2 className="text-2xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Welcome to Buurman
          </h2>
          <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
            Let&apos;s set up your team preferences
          </p>

          {/* Step indicator */}
          <div className="flex items-center gap-2 mt-6">
            {STEPS.map((step, i) => (
              <div key={step} className="flex items-center gap-2">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium transition-colors ${
                    i < stepIndex
                      ? 'bg-green-500 text-white'
                      : i === stepIndex
                        ? 'bg-[#5c7cfa] text-white'
                        : 'bg-[#edf0f7] dark:bg-[#2a2e3f] text-[#6b7194] dark:text-[#8b90a8]'
                  }`}
                >
                  {i < stepIndex ? <Check className="h-4 w-4" /> : i + 1}
                </div>
                {i < STEPS.length - 1 && (
                  <div
                    className={`w-12 h-0.5 ${
                      i < stepIndex
                        ? 'bg-green-500'
                        : 'bg-[#edf0f7] dark:bg-[#2a2e3f]'
                    }`}
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Content */}
        <div className="px-8 py-6 min-h-[320px]">
          {currentStep === 'country' && (
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Globe className="h-5 w-5 text-[#5c7cfa]" />
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Where are your properties?
                </h3>
              </div>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
                This sets your default currency and date format.
              </p>
              <input
                type="text"
                placeholder="Search countries..."
                value={countrySearch}
                onChange={(e) => setCountrySearch(e.target.value)}
                className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] mb-3 focus:outline-none focus:ring-1 focus:ring-[#5c7cfa]"
              />
              <div className="max-h-48 overflow-y-auto space-y-1">
                {filteredCountries.map((c) => {
                  const flag = getCountryByCode(c.code)?.flag;
                  return (
                    <button
                      key={c.code}
                      onClick={() => handleCountrySelect(c.code)}
                      className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors flex items-center gap-2.5 ${
                        country === c.code
                          ? 'bg-[#5c7cfa] text-white'
                          : 'hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6]'
                      }`}
                    >
                      {flag && <span className="text-base">{flag}</span>}
                      {c.name}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {currentStep === 'currency' &&
            (() => {
              const suggestedInfo = getCurrencyByCode(
                currencyList,
                suggestedCurrency
              );
              const selectedInfo = getCurrencyByCode(currencyList, currency);
              const countryName = COUNTRIES.find(
                (c) => c.code === country
              )?.name;
              const isSuggested = currency === suggestedCurrency;

              return (
                <div>
                  <div className="flex items-center gap-2 mb-4">
                    <Coins className="h-5 w-5 text-[#5c7cfa]" />
                    <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                      Team Currency
                    </h3>
                  </div>
                  <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
                    All financial data will use this currency. You can change it
                    later in settings.
                  </p>

                  {/* Suggested + Selected cards */}
                  <div
                    className={`grid gap-3 mb-5 ${isSuggested ? 'grid-cols-1' : 'grid-cols-2'}`}
                  >
                    {/* Suggested */}
                    <button
                      onClick={() => setCurrency(suggestedCurrency)}
                      className={`rounded-xl border-2 p-4 text-left transition-all ${
                        isSuggested
                          ? 'border-[#5c7cfa] bg-[#5c7cfa]/5 dark:bg-[#5c7cfa]/10'
                          : 'border-[#e2e6f0] dark:border-[#2a2e3f] hover:border-[#5c7cfa]/40'
                      }`}
                    >
                      <div className="flex items-center gap-1.5 mb-2">
                        <span className="text-[10px] font-semibold uppercase tracking-wider text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/20 px-1.5 py-0.5 rounded">
                          Suggested for {countryName}
                        </span>
                        {isSuggested && (
                          <span className="text-[10px] font-semibold uppercase tracking-wider text-[#5c7cfa] bg-[#5c7cfa]/10 px-1.5 py-0.5 rounded">
                            Selected
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2.5">
                        <span className="text-2xl">
                          {getCurrencyFlag(suggestedCurrency)}
                        </span>
                        <div>
                          <div className="text-lg font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                            {suggestedCurrency}
                            {suggestedInfo && (
                              <span className="ml-1.5 text-sm font-normal text-[#6b7194] dark:text-[#8b90a8]">
                                {suggestedInfo.symbol}
                              </span>
                            )}
                          </div>
                          {suggestedInfo && (
                            <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                              {suggestedInfo.name}
                            </div>
                          )}
                        </div>
                      </div>
                    </button>

                    {/* Selected (only when different from suggested) */}
                    {!isSuggested && (
                      <div className="rounded-xl border-2 border-[#5c7cfa] bg-[#5c7cfa]/5 dark:bg-[#5c7cfa]/10 p-4">
                        <div className="mb-2">
                          <span className="text-[10px] font-semibold uppercase tracking-wider text-[#5c7cfa] bg-[#5c7cfa]/10 px-1.5 py-0.5 rounded">
                            Selected
                          </span>
                        </div>
                        <div className="flex items-center gap-2.5">
                          <span className="text-2xl">
                            {getCurrencyFlag(currency)}
                          </span>
                          <div>
                            <div className="text-lg font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                              {currency}
                              {selectedInfo && (
                                <span className="ml-1.5 text-sm font-normal text-[#6b7194] dark:text-[#8b90a8]">
                                  {selectedInfo.symbol}
                                </span>
                              )}
                            </div>
                            {selectedInfo && (
                              <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                                {selectedInfo.name}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Currency grid */}
                  <div className="grid grid-cols-4 gap-2">
                    {COMMON_CURRENCIES.map((c) => {
                      const info = getCurrencyByCode(currencyList, c);
                      return (
                        <button
                          key={c}
                          onClick={() => setCurrency(c)}
                          className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors text-left ${
                            currency === c
                              ? 'bg-[#5c7cfa] text-white'
                              : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] hover:bg-[#e2e6f0] dark:hover:bg-[#262a3a]'
                          }`}
                        >
                          <div className="flex items-center gap-1.5">
                            <span className="text-sm">
                              {getCurrencyFlag(c)}
                            </span>
                            <span>{c}</span>
                          </div>
                          {info && (
                            <div
                              className={`text-[10px] mt-0.5 truncate ${
                                currency === c
                                  ? 'text-white/70'
                                  : 'text-[#6b7194] dark:text-[#8b90a8]'
                              }`}
                            >
                              {info.symbol}
                            </div>
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })()}

          {currentStep === 'dateFormat' && (
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Calendar className="h-5 w-5 text-[#5c7cfa]" />
                <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                  Date Format
                </h3>
              </div>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-4">
                How should dates be displayed?
              </p>
              <div className="space-y-3">
                {DATE_FORMATS.map((fmt) => (
                  <button
                    key={fmt.value}
                    onClick={() => setDateFormat(fmt.value)}
                    className={`w-full flex items-center justify-between px-4 py-3 rounded-lg border transition-colors ${
                      dateFormat === fmt.value
                        ? 'border-[#5c7cfa] bg-[#5c7cfa]/5'
                        : 'border-[#edf0f7] dark:border-[#2a2e3f] hover:border-[#5c7cfa]/50'
                    }`}
                  >
                    <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {fmt.label}
                    </span>
                    <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      {fmt.example}
                    </span>
                  </button>
                ))}
              </div>

              {/* Summary */}
              <div className="mt-6 bg-[#f1f3f9] dark:bg-[#1e2130] rounded-lg p-4">
                <h4 className="text-sm font-medium text-[#6b7194] dark:text-[#8b90a8] mb-2">
                  Summary
                </h4>
                <div className="space-y-1 text-sm">
                  <div className="flex justify-between">
                    <span className="text-[#6b7194] dark:text-[#8b90a8]">
                      Country
                    </span>
                    <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {getCountryByCode(country)?.flag}{' '}
                      {COUNTRIES.find((c) => c.code === country)?.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#6b7194] dark:text-[#8b90a8]">
                      Currency
                    </span>
                    <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {getCurrencyFlag(currency)} {currency}
                      {getCurrencyByCode(currencyList, currency) && (
                        <span className="text-[#6b7194] dark:text-[#8b90a8] font-normal ml-1">
                          ({getCurrencyByCode(currencyList, currency)?.symbol})
                        </span>
                      )}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#6b7194] dark:text-[#8b90a8]">
                      Date format
                    </span>
                    <span className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {dateFormat}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-8 pb-8 flex items-center justify-between">
          <button
            onClick={stepIndex === 0 ? onComplete : handleBack}
            className="px-4 py-2 text-sm font-medium text-[#6b7194] dark:text-[#8b90a8] hover:text-[#1a1d2e] dark:hover:text-[#eef0f6] transition-colors flex items-center gap-1"
          >
            {stepIndex === 0 ? (
              'Skip for now'
            ) : (
              <>
                <ArrowLeft className="h-4 w-4" />
                Back
              </>
            )}
          </button>
          <button
            onClick={isLastStep ? handleComplete : handleNext}
            disabled={completeMutation.isPending}
            className="px-6 py-2.5 bg-[#5c7cfa] hover:bg-[#4263eb] text-white rounded-lg text-sm font-medium transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            {completeMutation.isPending ? (
              'Saving...'
            ) : isLastStep ? (
              <>
                Complete Setup
                <Check className="h-4 w-4" />
              </>
            ) : (
              <>
                Next
                <ArrowRight className="h-4 w-4" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
