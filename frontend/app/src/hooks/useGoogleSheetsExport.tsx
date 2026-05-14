import { useCallback, useState } from 'react';
import { useToast } from '@buurman/ui';
import {
  useGoogleAccessToken,
  GoogleAccessTokenError,
} from './useGoogleAccessToken';
import type { GoogleSheetExportResult } from '@/api/googleSheetsExport';

/**
 * High-level handler for Google Sheets exports. Returns a stable `triggerExport` function plus
 * loading + last-result state. Per spec FR-10 the hook does NOT auto-open the resulting Sheet —
 * the caller renders an "Open in Google Sheets" link near the export control when `lastResult`
 * is set, and the success toast carries the same affordance for users who dismiss the inline link.
 */
export interface UseGoogleSheetsExportResult {
  triggerExport: (
    exporter: (accessToken: string) => Promise<GoogleSheetExportResult>
  ) => Promise<void>;
  isExporting: boolean;
  /** Result of the most recent successful export in this session; null until the first success. */
  lastResult: GoogleSheetExportResult | null;
  /** Clears `lastResult` (e.g. after the user dismisses the inline "Open" link). */
  clearLastResult: () => void;
}

export function useGoogleSheetsExport(): UseGoogleSheetsExportResult {
  const requestAccessToken = useGoogleAccessToken();
  const { showToast } = useToast();
  const [isExporting, setIsExporting] = useState(false);
  const [lastResult, setLastResult] = useState<GoogleSheetExportResult | null>(
    null
  );

  const triggerExport = useCallback(
    async (
      exporter: (accessToken: string) => Promise<GoogleSheetExportResult>
    ) => {
      setIsExporting(true);
      let accessToken: string;
      try {
        accessToken = await requestAccessToken();
      } catch (error) {
        setIsExporting(false);
        if (
          error instanceof GoogleAccessTokenError &&
          error.type === 'popup_closed'
        ) {
          return;
        }
        if (
          error instanceof GoogleAccessTokenError &&
          error.type === 'misconfigured'
        ) {
          showToast(
            'Google Sheets export is not configured for this environment',
            'error'
          );
          return;
        }
        showToast(
          error instanceof Error
            ? error.message
            : 'Could not authorize Google access',
          'error'
        );
        return;
      }

      try {
        const result = await exporter(accessToken);
        setLastResult(result);
        showToast(
          <>
            Google Sheet ready — open it{' '}
            <a
              href={result.url}
              target="_blank"
              rel="noopener noreferrer"
              className="underline font-semibold hover:opacity-80"
            >
              here
            </a>
            .
          </>,
          'success'
        );
      } catch (error) {
        showToast(
          error instanceof Error
            ? `Google Sheets export failed: ${error.message}`
            : 'Google Sheets export failed',
          'error'
        );
      } finally {
        setIsExporting(false);
      }
    },
    [requestAccessToken, showToast]
  );

  const clearLastResult = useCallback(() => setLastResult(null), []);

  return { triggerExport, isExporting, lastResult, clearLastResult };
}
