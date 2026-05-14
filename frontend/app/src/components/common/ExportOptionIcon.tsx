import { FileText, FileSpreadsheet } from 'lucide-react';

export type ExportFormat = 'csv' | 'excel' | 'google-sheets';

interface ExportOptionIconProps {
  format: ExportFormat;
  size?: 'sm' | 'md';
}

/**
 * Small coloured chip icon used inside the export dropdown to make each format visually distinct.
 * CSV is a neutral slate, Excel uses Microsoft's brand green, Google Sheets uses Sheets' green.
 * Sticks to inline SVG / lucide so we don't pull a brand-icon library.
 */
export const ExportOptionIcon = ({
  format,
  size = 'sm',
}: ExportOptionIconProps) => {
  const box = size === 'sm' ? 'h-5 w-5 rounded' : 'h-6 w-6 rounded-md';
  const icon = size === 'sm' ? 'h-3 w-3' : 'h-3.5 w-3.5';

  if (format === 'csv') {
    return (
      <span
        className={`inline-flex items-center justify-center bg-slate-500 text-white ${box}`}
        aria-hidden="true"
      >
        <FileText className={icon} />
      </span>
    );
  }
  if (format === 'excel') {
    return (
      <span
        className={`inline-flex items-center justify-center text-white ${box}`}
        style={{ backgroundColor: '#107C41' }}
        aria-hidden="true"
      >
        <FileSpreadsheet className={icon} />
      </span>
    );
  }
  // Google Sheets — custom inline SVG using Sheets' green; matches Google's brand identity
  // without depending on a brand-icon library or asset shipping.
  return (
    <span
      className={`inline-flex items-center justify-center text-white ${box}`}
      style={{ backgroundColor: '#0F9D58' }}
      aria-hidden="true"
    >
      <GoogleSheetsGlyph className={icon} />
    </span>
  );
};

const GoogleSheetsGlyph = ({ className }: { className: string }) => (
  <svg
    className={className}
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
    <path d="M14 3v6h6" />
    <path d="M8 13h8" />
    <path d="M8 17h8" />
    <path d="M12 13v4" />
  </svg>
);
