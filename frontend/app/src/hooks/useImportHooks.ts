import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
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
import { useToast } from '@buurman/ui';
import { queryKeys } from '../lib/queryKeys';

export const useUploadImportFile = () => {
  return useMutationWithToast({
    mutationFn: ({
      file,
      headerRow = true,
    }: {
      file: File;
      headerRow?: boolean;
    }) => uploadImportFile({ file }, { headerRow }),
  });
};

export const usePreviewImport = () => {
  return useMutationWithToast({
    mutationFn: (request: ImportPreviewRequest) => previewImport(request),
  });
};

export const useExecuteImport = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (request: ImportExecuteRequest) => executeImport(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.imports.all() });
      showToast(
        `Successfully imported ${data.importedCount} contacts`,
        'success'
      );
    },
  });
};

export const useImports = (page: number, size: number) => {
  return useQuery({
    queryKey: queryKeys.imports.all(page, size),
    queryFn: () => listImports({ page, size }),
  });
};

export const useImportDetail = (identifier: string | null) => {
  return useQuery({
    queryKey: queryKeys.imports.detail(identifier),
    queryFn: () => getImport(identifier ?? ''),
    enabled: !!identifier,
  });
};

export const useRevertImport = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (identifier: string) => revertImport(identifier),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.imports.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      showToast(
        `Reverted import: ${data.deletedContactCount} contacts deleted`,
        'info'
      );
    },
  });
};

export const useDownloadErrorReport = () => {
  return useMutationWithToast({
    mutationFn: (identifier: string) => downloadImportErrorReport(identifier),
  });
};
