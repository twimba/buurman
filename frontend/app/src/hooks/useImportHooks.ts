import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  uploadImportFile,
  previewImport,
  executeImport,
  listImports,
  getImport,
  revertImport,
  downloadImportErrorReport,
} from '@/generated/api/data-imports/data-imports';
import type {
  ImportPreviewRequest,
  ImportExecuteRequest,
} from '@/generated/models';
import { useToast } from '@/context/ToastContext';
import { getErrorMessage } from '@/utils/errorMessages';

export const useUploadImportFile = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      file,
      headerRow = true,
    }: {
      file: File;
      headerRow?: boolean;
    }) => uploadImportFile({ file }, { headerRow }),
    onError: (error: unknown) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePreviewImport = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (request: ImportPreviewRequest) => previewImport(request),
    onError: (error: unknown) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useExecuteImport = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (request: ImportExecuteRequest) => executeImport(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['imports'] });
      showToast(
        `Successfully imported ${data.importedCount} contacts`,
        'success'
      );
    },
    onError: (error: unknown) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useImports = (page: number, size: number) => {
  return useQuery({
    queryKey: ['imports', page, size],
    queryFn: () => listImports({ page, size }),
  });
};

export const useImportDetail = (identifier: string | null) => {
  return useQuery({
    queryKey: ['imports', identifier],
    queryFn: () => getImport(identifier ?? ''),
    enabled: !!identifier,
  });
};

export const useRevertImport = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (identifier: string) => revertImport(identifier),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['imports'] });
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      showToast(
        `Reverted import: ${data.deletedContactCount} contacts deleted`,
        'info'
      );
    },
    onError: (error: unknown) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDownloadErrorReport = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (identifier: string) => downloadImportErrorReport(identifier),
    onError: (error: unknown) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
