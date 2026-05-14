import { useState } from 'react';
import { ExportDropdown } from './ExportDropdown';
import { ExportOptionIcon } from './ExportOptionIcon';
import { GoogleSheetExportPill } from './GoogleSheetExportPill';
import { useFeatureFlags } from '@/context/FeatureFlagContext';
import { FeatureFlags } from '@/constants/featureFlags';
import { useGoogleSheetsExport } from '@/hooks/useGoogleSheetsExport';
import { downloadBlob } from '@/api/listExports';
import type { GoogleSheetExportResult } from '@/api/googleSheetsExport';

export interface EntityExportControlsProps {
  /** Base filename without extension (e.g. "properties" → "properties.csv"). */
  filenameStem: string;
  /** CSV download endpoint adapter. */
  csv: () => Promise<Blob>;
  /** Excel download endpoint adapter. */
  xlsx: () => Promise<Blob>;
  /** Google Sheets endpoint adapter — receives the access token. */
  googleSheet: (accessToken: string) => Promise<GoogleSheetExportResult>;
  /** Visual size of the dropdown trigger. */
  size?: 'sm' | 'md';
}

/**
 * Drop-in export control for entity list pages: renders the persistent "Open last Google Sheet"
 * pill (if a Sheet was just created this session) followed by an ExportDropdown with CSV, Excel
 * (gated by `excel_export`), and Google Sheets (gated by `google_sheets_export`) options.
 *
 * Centralises the boilerplate so adding a new entity export is two lines: import this and pass
 * the three endpoint adapters.
 */
export const EntityExportControls = ({
  filenameStem,
  csv,
  xlsx,
  googleSheet,
  size = 'md',
}: EntityExportControlsProps) => {
  const { isEnabled } = useFeatureFlags();
  const {
    triggerExport: triggerGoogleSheet,
    isExporting: isGoogleExporting,
    lastResult: lastGoogleSheet,
    clearLastResult: clearLastGoogleSheet,
  } = useGoogleSheetsExport();
  const [isDownloading, setIsDownloading] = useState(false);

  const handleCsv = async () => {
    setIsDownloading(true);
    try {
      const blob = await csv();
      downloadBlob(blob, `${filenameStem}.csv`);
    } finally {
      setIsDownloading(false);
    }
  };

  const handleXlsx = async () => {
    setIsDownloading(true);
    try {
      const blob = await xlsx();
      downloadBlob(blob, `${filenameStem}.xlsx`);
    } finally {
      setIsDownloading(false);
    }
  };

  const busy = isDownloading || isGoogleExporting;

  return (
    <>
      <GoogleSheetExportPill
        result={lastGoogleSheet}
        onDismiss={clearLastGoogleSheet}
        size={size}
      />
      <ExportDropdown
        size={size}
        disabled={busy}
        exporting={busy}
        options={[
          {
            label: 'CSV',
            icon: <ExportOptionIcon format="csv" />,
            onExport: handleCsv,
          },
          ...(isEnabled(FeatureFlags.EXCEL_EXPORT)
            ? [
                {
                  label: 'Excel',
                  icon: <ExportOptionIcon format="excel" />,
                  onExport: handleXlsx,
                },
              ]
            : []),
          ...(isEnabled(FeatureFlags.GOOGLE_SHEETS_EXPORT)
            ? [
                {
                  label: 'Google Sheets',
                  icon: <ExportOptionIcon format="google-sheets" />,
                  onExport: () =>
                    triggerGoogleSheet((token) => googleSheet(token)),
                },
              ]
            : []),
        ]}
      />
    </>
  );
};

EntityExportControls.displayName = 'EntityExportControls';
