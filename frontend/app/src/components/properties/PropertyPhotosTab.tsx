import {
  usePropertyPhotos,
  useUploadPropertyPhoto,
  useSetMainPhoto,
} from '@/hooks/usePropertyHooks';
import { useDeletePhoto } from '@/hooks/usePhotoHooks';
import { PhotoGallery } from '@/components/properties/PhotoGallery';
import { useTeam } from '@/context/TeamContext';

interface PropertyPhotosTabProps {
  propertyId: string;
}

export const PropertyPhotosTab = ({ propertyId }: PropertyPhotosTabProps) => {
  const { canEditData } = useTeam();
  const { data: photos = [], isLoading, error } = usePropertyPhotos(propertyId);
  const uploadMutation = useUploadPropertyPhoto(propertyId);
  const setMainMutation = useSetMainPhoto(propertyId);
  const deleteMutation = useDeletePhoto();

  const handleUpload = async (file: File, title?: string, notes?: string) => {
    await uploadMutation.mutateAsync({ file, title, notes });
  };

  const handleSetMain = async (photoId: string) => {
    await setMainMutation.mutateAsync(photoId);
  };

  const handleDelete = async (photoId: string) => {
    await deleteMutation.mutateAsync(photoId);
  };

  return (
    <div className="bg-surface-card rounded-lg shadow-sm border border-border-default p-6">
      <PhotoGallery
        photos={photos}
        isLoading={isLoading}
        error={error}
        onUpload={handleUpload}
        onSetMain={handleSetMain}
        onDelete={handleDelete}
        isUploading={uploadMutation.isPending}
        isDeleting={deleteMutation.isPending}
        readOnly={!canEditData}
      />
    </div>
  );
};
