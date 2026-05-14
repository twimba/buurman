import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Globe,
  Languages,
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
import { supportedLanguages, useLocale } from '@/context/LocaleContext';

interface OnboardingWizardProps {
  onComplete: () => void;
  currentCountryCode: string;
  currentCurrency: string;
}

const STEPS = ['language', 'country', 'currency', 'dateFormat'] as const;

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
  const { t } = useTranslation('settings');
  const { locale, setLocale } = useLocale();
  const [stepIndex, setStepIndex] = useState(0);
  const [language, setLanguage] = useState(locale || 'en');
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
    if (currentStep === 'language') {
      setLocale(language);
    }
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
      <div className="bg-surface-card rounded-2xl shadow-xl max-w-lg w-full overflow-hidden">
        {/* Header */}
        <div className="px-8 pt-8 pb-4">
          <h2 className="text-2xl font-bold text-text-primary">
            {t('onboarding.welcome')}
          </h2>
          <p className="text-sm text-text-secondary mt-1">
            {t('onboarding.subtitle')}
          </p>

          {/* Step indicator */}
          <div className="flex items-center gap-2 mt-6">
            {STEPS.map((step, i) => (
              <div key={step} className="flex items-center gap-2">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium transition-colors ${
                    i < stepIndex
                      ? 'bg-success-text text-white'
                      : i === stepIndex
                        ? 'bg-primary-500 text-white'
                        : 'bg-surface-inset text-text-secondary'
                  }`}
                >
                  {i < stepIndex ? <Check className="h-4 w-4" /> : i + 1}
                </div>
                {i < STEPS.length - 1 && (
                  <div
                    className={`w-12 h-0.5 ${
                      i < stepIndex ? 'bg-success-text' : 'bg-surface-inset'
                    }`}
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Content */}
        <div className="px-8 py-6 min-h-[320px]">
          {currentStep === 'language' && (
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Languages className="h-5 w-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-text-primary">
                  {t('onboarding.chooseLanguage')}
                </h3>
              </div>
              <p className="text-sm text-text-secondary mb-4">
                {t('onboarding.chooseLanguageDesc')}
              </p>
              <div className="space-y-2">
                {supportedLanguages.map((lang) => (
                  <button
                    key={lang.value}
                    onClick={() => setLanguage(lang.value)}
                    className={`w-full text-left px-4 py-3 rounded-lg text-sm font-medium transition-colors flex items-center justify-between ${
                      language === lang.value
                        ? 'bg-primary-500 text-white'
                        : 'hover:bg-surface-inset text-text-primary border border-border-default'
                    }`}
                  >
                    <span>{lang.label}</span>
                    {language === lang.value && <Check className="h-4 w-4" />}
                  </button>
                ))}
              </div>
            </div>
          )}

          {currentStep === 'country' && (
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Globe className="h-5 w-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-text-primary">
                  {t('onboarding.whereProperties')}
                </h3>
              </div>
              <p className="text-sm text-text-secondary mb-4">
                {t('onboarding.wherePropertiesDesc')}
              </p>
              <input
                type="text"
                placeholder={t('onboarding.searchCountries')}
                value={countrySearch}
                onChange={(e) => setCountrySearch(e.target.value)}
                className="w-full px-3 py-2 border border-border-strong rounded-lg bg-surface-card text-text-primary mb-3 focus:outline-none focus:ring-1 focus:ring-primary-500"
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
                          ? 'bg-primary-500 text-white'
                          : 'hover:bg-surface-inset text-text-primary'
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
                    <Coins className="h-5 w-5 text-primary-500" />
                    <h3 className="text-lg font-semibold text-text-primary">
                      {t('onboarding.teamCurrency')}
                    </h3>
                  </div>
                  <p className="text-sm text-text-secondary mb-4">
                    {t('onboarding.teamCurrencyDesc')}
                  </p>

                  {/* Suggested + Selected cards */}
                  <div
                    className={`grid gap-3 mb-5 ${isSuggested ? 'grid-cols-1' : 'grid-cols-2'}`}
                  >
                    {/* Suggested */}
                    <button
                      onClick={() => setCurrency(suggestedCurrency)}
                      className={`rounded-lg border-2 p-4 text-left transition-all ${
                        isSuggested
                          ? 'border-primary-500 bg-primary-500/5 dark:bg-primary-500/10'
                          : 'border-border-default hover:border-primary-500/40'
                      }`}
                    >
                      <div className="flex items-center gap-1.5 mb-2">
                        <span className="text-[10px] font-semibold uppercase tracking-wider text-success-text bg-success-bg px-1.5 py-0.5 rounded">
                          {t('onboarding.suggestedFor', {
                            country: countryName,
                          })}
                        </span>
                        {isSuggested && (
                          <span className="text-[10px] font-semibold uppercase tracking-wider text-primary-500 bg-primary-500/10 px-1.5 py-0.5 rounded">
                            {t('onboarding.selected')}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2.5">
                        <span className="text-2xl">
                          {getCurrencyFlag(suggestedCurrency)}
                        </span>
                        <div>
                          <div className="text-lg font-bold text-text-primary">
                            {suggestedCurrency}
                            {suggestedInfo && (
                              <span className="ml-1.5 text-sm font-normal text-text-secondary">
                                {suggestedInfo.symbol}
                              </span>
                            )}
                          </div>
                          {suggestedInfo && (
                            <div className="text-xs text-text-secondary">
                              {suggestedInfo.name}
                            </div>
                          )}
                        </div>
                      </div>
                    </button>

                    {/* Selected (only when different from suggested) */}
                    {!isSuggested && (
                      <div className="rounded-lg border-2 border-primary-500 bg-primary-500/5 dark:bg-primary-500/10 p-4">
                        <div className="mb-2">
                          <span className="text-[10px] font-semibold uppercase tracking-wider text-primary-500 bg-primary-500/10 px-1.5 py-0.5 rounded">
                            {t('onboarding.selected')}
                          </span>
                        </div>
                        <div className="flex items-center gap-2.5">
                          <span className="text-2xl">
                            {getCurrencyFlag(currency)}
                          </span>
                          <div>
                            <div className="text-lg font-bold text-text-primary">
                              {currency}
                              {selectedInfo && (
                                <span className="ml-1.5 text-sm font-normal text-text-secondary">
                                  {selectedInfo.symbol}
                                </span>
                              )}
                            </div>
                            {selectedInfo && (
                              <div className="text-xs text-text-secondary">
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
                              ? 'bg-primary-500 text-white'
                              : 'bg-surface-inset text-text-primary hover:bg-neutral-100 dark:hover:bg-surface-raised'
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
                                  : 'text-text-secondary'
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
                <Calendar className="h-5 w-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-text-primary">
                  {t('onboarding.dateFormat')}
                </h3>
              </div>
              <p className="text-sm text-text-secondary mb-4">
                {t('onboarding.dateFormatDesc')}
              </p>
              <div className="space-y-3">
                {DATE_FORMATS.map((fmt) => (
                  <button
                    key={fmt.value}
                    onClick={() => setDateFormat(fmt.value)}
                    className={`w-full flex items-center justify-between px-4 py-3 rounded-lg border transition-colors ${
                      dateFormat === fmt.value
                        ? 'border-primary-500 bg-primary-500/5'
                        : 'border-border-default hover:border-primary-500/50'
                    }`}
                  >
                    <span className="font-medium text-text-primary">
                      {fmt.label}
                    </span>
                    <span className="text-sm text-text-secondary">
                      {fmt.example}
                    </span>
                  </button>
                ))}
              </div>

              {/* Summary */}
              <div className="mt-6 bg-surface-inset rounded-lg p-4">
                <h4 className="text-sm font-medium text-text-secondary mb-2">
                  {t('onboarding.summary')}
                </h4>
                <div className="space-y-1 text-sm">
                  <div className="flex justify-between">
                    <span className="text-text-secondary">
                      {t('onboarding.summaryLanguage')}
                    </span>
                    <span className="font-medium text-text-primary">
                      {supportedLanguages.find((l) => l.value === language)
                        ?.label ?? language}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-text-secondary">
                      {t('onboarding.summaryCountry')}
                    </span>
                    <span className="font-medium text-text-primary">
                      {getCountryByCode(country)?.flag}{' '}
                      {COUNTRIES.find((c) => c.code === country)?.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-text-secondary">
                      {t('onboarding.summaryCurrency')}
                    </span>
                    <span className="font-medium text-text-primary">
                      {getCurrencyFlag(currency)} {currency}
                      {getCurrencyByCode(currencyList, currency) && (
                        <span className="text-text-secondary font-normal ml-1">
                          ({getCurrencyByCode(currencyList, currency)?.symbol})
                        </span>
                      )}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-text-secondary">
                      {t('onboarding.summaryDateFormat')}
                    </span>
                    <span className="font-medium text-text-primary">
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
            className="px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary transition-colors flex items-center gap-1"
          >
            {stepIndex === 0 ? (
              t('onboarding.skipForNow')
            ) : (
              <>
                <ArrowLeft className="h-4 w-4" />
                {t('buttons.back', { ns: 'common' })}
              </>
            )}
          </button>
          <button
            onClick={isLastStep ? handleComplete : handleNext}
            disabled={completeMutation.isPending}
            className="px-6 py-2.5 bg-primary-500 hover:bg-primary-700 text-white rounded-lg text-sm font-medium transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            {completeMutation.isPending ? (
              t('onboarding.saving')
            ) : isLastStep ? (
              <>
                {t('onboarding.completeSetup')}
                <Check className="h-4 w-4" />
              </>
            ) : (
              <>
                {t('pagination.next', { ns: 'common' })}
                <ArrowRight className="h-4 w-4" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
