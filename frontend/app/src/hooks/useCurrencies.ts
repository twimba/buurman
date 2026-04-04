import { useQuery } from '@tanstack/react-query';
import { getCurrencies, CurrencyInfo } from '../api/reference';
import { queryKeys } from '../lib/queryKeys';

export const useCurrencies = () => {
  return useQuery({
    queryKey: queryKeys.currencies.all(),
    queryFn: getCurrencies,
    staleTime: Infinity,
  });
};

/** Top currencies shown first in the selector (order matters). */
const TOP_CURRENCY_CODES = [
  'USD',
  'EUR',
  'GBP',
  'JPY',
  'CHF',
  'CAD',
  'AUD',
  'CNY',
  'BGN',
  'CZK',
  'DKK',
  'HUF',
  'ISK',
  'NOK',
  'PLN',
  'RON',
  'SEK',
  'HRK',
  'ALL',
  'BAM',
  'MKD',
  'RSD',
  'UAH',
];

const TOP_SET = new Set(TOP_CURRENCY_CODES);

export const splitCurrencies = (
  currencies: CurrencyInfo[]
): { top: CurrencyInfo[]; other: CurrencyInfo[] } => {
  const top: CurrencyInfo[] = [];
  const other: CurrencyInfo[] = [];
  for (const c of currencies) {
    if (TOP_SET.has(c.code)) {
      top.push(c);
    } else {
      other.push(c);
    }
  }
  // Preserve the predefined order for top currencies
  top.sort(
    (a, b) =>
      TOP_CURRENCY_CODES.indexOf(a.code) - TOP_CURRENCY_CODES.indexOf(b.code)
  );
  other.sort((a, b) => a.name.localeCompare(b.name));
  return { top, other };
};

export const getCurrencyByCode = (
  currencies: CurrencyInfo[] | undefined,
  code: string
): CurrencyInfo | undefined => {
  return currencies?.find((c) => c.code === code);
};

export const getCurrencySymbol = (
  currencies: CurrencyInfo[] | undefined,
  code: string
): string => {
  return getCurrencyByCode(currencies, code)?.symbol ?? code;
};

export const getFractionalDigits = (
  currencies: CurrencyInfo[] | undefined,
  code: string
): number => {
  return getCurrencyByCode(currencies, code)?.fractionalDigits ?? 2;
};
