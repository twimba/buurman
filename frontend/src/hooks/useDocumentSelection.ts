import { useState, useCallback, useRef } from 'react';
import { DocumentResponse } from '@/types/property';

export const useDocumentSelection = (documents: DocumentResponse[]) => {
  const [selectedDocuments, setSelectedDocuments] = useState<Set<string>>(
    new Set()
  );
  const lastSelectedIndex = useRef<number | null>(null);

  const handleSelectDocument = useCallback(
    (id: string, shiftKey = false) => {
      const currentIndex = documents.findIndex((d) => d.identifier === id);

      if (
        shiftKey &&
        lastSelectedIndex.current !== null &&
        currentIndex !== -1
      ) {
        const start = Math.min(lastSelectedIndex.current, currentIndex);
        const end = Math.max(lastSelectedIndex.current, currentIndex);
        setSelectedDocuments((prev) => {
          const newSet = new Set(prev);
          for (let i = start; i <= end; i++) {
            newSet.add(documents[i].identifier);
          }
          return newSet;
        });
      } else {
        setSelectedDocuments((prev) => {
          const newSet = new Set(prev);
          if (newSet.has(id)) {
            newSet.delete(id);
          } else {
            newSet.add(id);
          }
          return newSet;
        });
      }

      lastSelectedIndex.current = currentIndex;
    },
    [documents]
  );

  const handleSelectAll = useCallback(() => {
    setSelectedDocuments((prev) => {
      if (prev.size < documents.length) {
        return new Set(documents.map((d) => d.identifier));
      }
      return new Set();
    });
    lastSelectedIndex.current = null;
  }, [documents]);

  const clearSelection = useCallback(() => {
    setSelectedDocuments(new Set());
    lastSelectedIndex.current = null;
  }, []);

  return {
    selectedDocuments,
    handleSelectDocument,
    handleSelectAll,
    clearSelection,
  };
};
