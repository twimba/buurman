import { useState, useCallback, useRef } from 'react';
import { PhotoResponse } from '@/types/property';

export const usePhotoSelection = (photos: PhotoResponse[]) => {
  const [selectedPhotos, setSelectedPhotos] = useState<Set<string>>(new Set());
  const lastSelectedIndex = useRef<number | null>(null);

  const handleSelectPhoto = useCallback(
    (id: string, shiftKey = false) => {
      const currentIndex = photos.findIndex((p) => p.identifier === id);

      if (
        shiftKey &&
        lastSelectedIndex.current !== null &&
        currentIndex !== -1
      ) {
        const start = Math.min(lastSelectedIndex.current, currentIndex);
        const end = Math.max(lastSelectedIndex.current, currentIndex);
        setSelectedPhotos((prev) => {
          const newSet = new Set(prev);
          for (let i = start; i <= end; i++) {
            newSet.add(photos[i].identifier);
          }
          return newSet;
        });
      } else {
        setSelectedPhotos((prev) => {
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
    [photos]
  );

  const handleSelectAll = useCallback(() => {
    setSelectedPhotos((prev) => {
      if (prev.size < photos.length) {
        return new Set(photos.map((p) => p.identifier));
      }
      return new Set();
    });
    lastSelectedIndex.current = null;
  }, [photos]);

  const clearSelection = useCallback(() => {
    setSelectedPhotos(new Set());
    lastSelectedIndex.current = null;
  }, []);

  return { selectedPhotos, handleSelectPhoto, handleSelectAll, clearSelection };
};
