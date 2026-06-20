const EUR = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 2,
});

/** Formats EUR minor units (cents) as a currency string, e.g. 123456 -> "€1,234.56". */
export const formatEurMinor = (minor: number): string =>
  EUR.format(minor / 100);

/** Compact EUR for tight spaces, e.g. 123456 -> "€1.2K". */
export const formatEurMinorCompact = (minor: number): string => {
  const eur = minor / 100;
  if (Math.abs(eur) >= 1000) {
    return `€${(eur / 1000).toFixed(1)}K`;
  }
  return EUR.format(eur);
};
