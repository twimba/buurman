export interface Currency {
  code: string;
  name: string;
  symbol: string;
  fractionalDigits?: number;
}

// Top world currencies (ordered by importance/usage)
// Includes major global currencies and all European currencies
const topCurrencies: Currency[] = [
  // Major global currencies
  { code: 'USD', name: 'US Dollar', symbol: '$' },
  { code: 'EUR', name: 'Euro', symbol: '€' },
  { code: 'GBP', name: 'British Pound', symbol: '£' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥' },
  { code: 'CHF', name: 'Swiss Franc', symbol: 'Fr' },
  { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$' },
  { code: 'CNY', name: 'Chinese Yuan', symbol: '¥' },
  // European currencies (non-Euro)
  { code: 'BGN', name: 'Bulgarian Lev', symbol: 'лв' },
  { code: 'CZK', name: 'Czech Koruna', symbol: 'Kč' },
  { code: 'DKK', name: 'Danish Krone', symbol: 'kr' },
  { code: 'HUF', name: 'Hungarian Forint', symbol: 'Ft' },
  { code: 'ISK', name: 'Icelandic Króna', symbol: 'kr' },
  { code: 'NOK', name: 'Norwegian Krone', symbol: 'kr' },
  { code: 'PLN', name: 'Polish Zloty', symbol: 'zł' },
  { code: 'RON', name: 'Romanian Leu', symbol: 'lei' },
  { code: 'SEK', name: 'Swedish Krona', symbol: 'kr' },
  { code: 'HRK', name: 'Croatian Kuna', symbol: 'kn' },
  { code: 'ALL', name: 'Albanian Lek', symbol: 'L' },
  { code: 'BAM', name: 'Bosnia-Herzegovina Mark', symbol: 'KM' },
  { code: 'MKD', name: 'Macedonian Denar', symbol: 'ден' },
  { code: 'RSD', name: 'Serbian Dinar', symbol: 'дин' },
  { code: 'UAH', name: 'Ukrainian Hryvnia', symbol: '₴' },
];

// Rest of world currencies (sorted alphabetically by name)
// Excludes currencies already in topCurrencies
const otherCurrencies: Currency[] = [
  { code: 'AED', name: 'UAE Dirham', symbol: 'د.إ' },
  { code: 'ARS', name: 'Argentine Peso', symbol: '$' },
  { code: 'BRL', name: 'Brazilian Real', symbol: 'R$' },
  { code: 'CLP', name: 'Chilean Peso', symbol: '$' },
  { code: 'COP', name: 'Colombian Peso', symbol: '$' },
  { code: 'EGP', name: 'Egyptian Pound', symbol: 'E£' },
  { code: 'GEL', name: 'Georgian Lari', symbol: '₾' },
  { code: 'HKD', name: 'Hong Kong Dollar', symbol: 'HK$' },
  { code: 'IDR', name: 'Indonesian Rupiah', symbol: 'Rp' },
  { code: 'ILS', name: 'Israeli Shekel', symbol: '₪' },
  { code: 'INR', name: 'Indian Rupee', symbol: '₹' },
  { code: 'KRW', name: 'South Korean Won', symbol: '₩' },
  { code: 'MXN', name: 'Mexican Peso', symbol: '$' },
  { code: 'MYR', name: 'Malaysian Ringgit', symbol: 'RM' },
  { code: 'NZD', name: 'New Zealand Dollar', symbol: 'NZ$' },
  { code: 'PHP', name: 'Philippine Peso', symbol: '₱' },
  { code: 'RUB', name: 'Russian Ruble', symbol: '₽' },
  { code: 'SAR', name: 'Saudi Riyal', symbol: 'ر.س' },
  { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$' },
  { code: 'THB', name: 'Thai Baht', symbol: '฿' },
  { code: 'TRY', name: 'Turkish Lira', symbol: '₺' },
  { code: 'TWD', name: 'Taiwan Dollar', symbol: 'NT$' },
  { code: 'VND', name: 'Vietnamese Dong', symbol: '₫' },
  { code: 'ZAR', name: 'South African Rand', symbol: 'R' },
].sort((a, b) => a.name.localeCompare(b.name));

export const allCurrencies = [...topCurrencies, ...otherCurrencies];
export { topCurrencies };

export const getCurrencyByCode = (code: string): Currency | undefined => {
  return allCurrencies.find((c) => c.code === code);
};

export const formatCurrency = (code: string): string => {
  const currency = getCurrencyByCode(code);
  if (!currency) return code;
  return `${currency.symbol} ${currency.name} (${currency.code})`;
};

export const getCurrencySymbol = (code: string): string => {
  const currency = getCurrencyByCode(code);
  return currency?.symbol ?? code;
};
