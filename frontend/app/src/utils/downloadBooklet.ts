import { downloadBlob } from './downloadBlob';

/**
 * Fetches a booklet PDF and triggers a browser download. The generated export functions
 * return a typed `application/pdf` Blob, so no re-wrapping is needed.
 */
export const downloadBookletPdf = async (
  fetchPdf: () => Promise<Blob>,
  filename: string
) => {
  downloadBlob(await fetchPdf(), filename);
};
