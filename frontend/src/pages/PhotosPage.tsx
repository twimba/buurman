import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Download,
  Trash2,
  Image as ImageIcon,
  Filter,
  CheckSquare,
  Square,
  ArrowUpDown,
  AlertCircle,
} from 'lucide-react';
import { useDocuments, useDeleteDocument } from '@/hooks/useDocumentHooks';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { DocumentResponse } from '@/types/property';
import { useTeam } from '@/context/TeamContext';
import { useFormatDate } from '@/hooks/useFormatDate';

export const PhotosPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
  const [searchTerm, setSearchTerm] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('');
  const [selectedPhotos, setSelectedPhotos] = useState<Set<string>>(new Set());
  const [previewPhoto, setPreviewPhoto] = useState<DocumentResponse | null>(
    null
  );
  const [imageErrors, setImageErrors] = useState<Set<string>>(new Set());

  // Sorting and pagination state
  const [sortField, setSortField] = useState<
    'title' | 'entityType' | 'fileSize' | 'uploadedAt'
  >('uploadedAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 24;

  const { data: allDocuments, isLoading } = useDocuments({
    search: searchTerm || undefined,
    entityType: entityTypeFilter || undefined,
  });

  const photos = useMemo(
    () => allDocuments?.filter((doc) => doc.category === 'PHOTO') || [],
    [allDocuments]
  );

  // Sorting and pagination
  const sortedPhotos = useMemo(() => {
    const sorted = [...photos];
    sorted.sort((a, b) => {
      let aVal: string | number, bVal: string | number;

      switch (sortField) {
        case 'title':
          aVal = (a.title ?? a.fileName).toLowerCase();
          bVal = (b.title ?? b.fileName).toLowerCase();
          break;
        case 'entityType':
          aVal = a.entityType;
          bVal = b.entityType;
          break;
        case 'fileSize':
          aVal = a.fileSize;
          bVal = b.fileSize;
          break;
        case 'uploadedAt':
          aVal = new Date(a.uploadedAt).getTime();
          bVal = new Date(b.uploadedAt).getTime();
          break;
        default:
          return 0;
      }

      if (aVal < bVal) return sortOrder === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    return sorted;
  }, [photos, sortField, sortOrder]);

  const paginatedPhotos = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return sortedPhotos.slice(startIndex, endIndex);
  }, [sortedPhotos, currentPage]);

  const totalPages = Math.ceil((sortedPhotos?.length || 0) / itemsPerPage);

  const deleteMutation = useDeleteDocument();

  const handleDelete = (id: string) => {
    if (window.confirm('Are you sure you want to delete this photo?')) {
      deleteMutation.mutate(id);
      setSelectedPhotos((prev) => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const handleSelectAll = () => {
    if (sortedPhotos && selectedPhotos.size < sortedPhotos.length) {
      setSelectedPhotos(new Set(sortedPhotos.map((photo) => photo.id)));
    } else {
      setSelectedPhotos(new Set());
    }
  };

  const handleSelectPhoto = (id: string) => {
    setSelectedPhotos((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="px-4 py-8">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-1">
          <ImageIcon className="h-8 w-8 text-blue-600 dark:text-blue-400" />
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">
            Photo Library
          </h1>
        </div>
        <p className="text-gray-600 dark:text-gray-400 ml-11">
          Browse and manage all your photos in one place
        </p>
      </div>

      {/* Search and Filter Bar */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm dark:shadow-gray-900 p-4 mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search photos by title, filename, or notes..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
            />
          </div>

          {/* Entity Type Filter */}
          <div className="w-full md:w-48 relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <select
              value={entityTypeFilter}
              onChange={(e) => setEntityTypeFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
            >
              <option value="">All Types</option>
              <option value="PROPERTY">Properties</option>
              <option value="TENANT">Tenants</option>
              <option value="CONTRACT">Contracts</option>
              <option value="PAYMENT">Payments</option>
              <option value="EXPENSE">Expenses</option>
            </select>
          </div>
        </div>

        {/* Bulk Actions */}
        {selectedPhotos.size > 0 && (
          <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700 flex items-center gap-4">
            <span className="text-sm text-gray-600 dark:text-gray-400">
              {selectedPhotos.size} photo(s) selected
            </span>
          </div>
        )}
      </div>

      {/* Photo Grid */}
      {isLoading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 dark:border-gray-100"></div>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            Loading photos...
          </p>
        </div>
      ) : !photos || photos.length === 0 ? (
        <div className="text-center py-12 bg-white dark:bg-gray-800 rounded-lg shadow-sm dark:shadow-gray-900">
          <ImageIcon className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600 dark:text-gray-400">No photos found</p>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm dark:shadow-gray-900 p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <button
                onClick={handleSelectAll}
                className="text-gray-500 hover:text-gray-700"
              >
                {sortedPhotos && selectedPhotos.size === sortedPhotos.length ? (
                  <CheckSquare className="h-5 w-5" />
                ) : (
                  <Square className="h-5 w-5" />
                )}
              </button>
              <span className="text-sm text-gray-600">
                {sortedPhotos.length} photo
                {sortedPhotos.length !== 1 ? 's' : ''}
              </span>
            </div>

            {/* Sort Controls */}
            <div className="flex items-center gap-2">
              <ArrowUpDown className="h-4 w-4 text-gray-400" />
              <select
                value={`${sortField}-${sortOrder}`}
                onChange={(e) => {
                  const [field, order] = e.target.value.split('-') as [
                    typeof sortField,
                    'asc' | 'desc',
                  ];
                  setSortField(field);
                  setSortOrder(order);
                  setCurrentPage(1);
                }}
                className="text-sm border border-gray-300 rounded-md px-2 py-1 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="uploadedAt-desc">Newest First</option>
                <option value="uploadedAt-asc">Oldest First</option>
                <option value="title-asc">Title (A-Z)</option>
                <option value="title-desc">Title (Z-A)</option>
                <option value="entityType-asc">Type (A-Z)</option>
                <option value="entityType-desc">Type (Z-A)</option>
                <option value="fileSize-desc">Largest First</option>
                <option value="fileSize-asc">Smallest First</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {paginatedPhotos.map((photo) => (
              <div
                key={photo.id}
                className={`relative group rounded-lg overflow-hidden border-2 transition-all ${
                  selectedPhotos.has(photo.id)
                    ? 'border-blue-500'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <div
                  className="w-full h-64 cursor-pointer bg-gray-100 relative"
                  onClick={() =>
                    !imageErrors.has(photo.id) && setPreviewPhoto(photo)
                  }
                >
                  {!photo.downloadUrl ? (
                    <div className="flex items-center justify-center h-full">
                      <div className="text-center p-4">
                        <AlertCircle className="h-8 w-8 text-gray-400 mx-auto mb-2" />
                        <p className="text-xs text-gray-500">No URL</p>
                      </div>
                    </div>
                  ) : imageErrors.has(photo.id) ? (
                    <div className="flex items-center justify-center h-full">
                      <div className="text-center p-4">
                        <AlertCircle className="h-8 w-8 text-red-400 mx-auto mb-2" />
                        <p className="text-xs text-gray-600 font-medium">
                          Failed to load
                        </p>
                        <p className="text-xs text-gray-500 mt-1 break-all px-2">
                          {photo.downloadUrl.substring(0, 50)}...
                        </p>
                      </div>
                    </div>
                  ) : (
                    <img
                      src={photo.downloadUrl}
                      alt={photo.title ?? photo.fileName}
                      className="w-full h-full object-cover"
                      crossOrigin="anonymous"
                      onError={() => {
                        setImageErrors((prev) => new Set(prev).add(photo.id));
                      }}
                    />
                  )}
                </div>

                {/* Overlay - Only visible on hover */}
                <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                  <div className="absolute inset-0 bg-black bg-opacity-40"></div>
                  <div className="absolute inset-0 flex items-start justify-between p-2 pointer-events-none">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSelectPhoto(photo.id);
                      }}
                      className="pointer-events-auto"
                    >
                      {selectedPhotos.has(photo.id) ? (
                        <CheckSquare className="h-5 w-5 text-white" />
                      ) : (
                        <Square className="h-5 w-5 text-white" />
                      )}
                    </button>

                    <div className="flex gap-2">
                      <a
                        href={photo.downloadUrl || undefined}
                        target="_blank"
                        rel="noopener noreferrer"
                        onClick={(e) => e.stopPropagation()}
                        className="p-1 bg-white rounded hover:bg-gray-100 pointer-events-auto"
                        title="Download"
                      >
                        <Download className="h-4 w-4 text-gray-700" />
                      </a>
                      {canEditData && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDelete(photo.id);
                          }}
                          className="p-1 bg-white rounded hover:bg-red-100 pointer-events-auto"
                          title="Delete"
                        >
                          <Trash2 className="h-4 w-4 text-red-600" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>

                {/* Info */}
                <div className="p-2 bg-white">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {photo.title ?? photo.fileName}
                  </p>
                  <div className="flex items-center justify-between mt-1">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        const entityPath =
                          photo.entityType.toLowerCase() === 'property'
                            ? `/properties/${photo.entityId}`
                            : photo.entityType.toLowerCase() === 'tenant'
                              ? `/tenants/${photo.entityId}`
                              : photo.entityType.toLowerCase() === 'contract'
                                ? `/contracts/${photo.entityId}`
                                : photo.entityType.toLowerCase() === 'payment'
                                  ? `/payments/${photo.entityId}`
                                  : photo.entityType.toLowerCase() === 'expense'
                                    ? `/expenses/${photo.entityId}`
                                    : '#';
                        if (entityPath !== '#') navigate(entityPath);
                      }}
                      className="text-xs text-blue-600 hover:underline"
                    >
                      {photo.entityType}
                    </button>
                    <span className="text-xs text-gray-500">
                      {formatFileSize(photo.fileSize)}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    {formatDate(photo.uploadedAt)}
                  </p>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-6 pt-6 border-t border-gray-200 flex items-center justify-between">
              <div className="text-sm text-gray-600">
                Showing {(currentPage - 1) * itemsPerPage + 1} to{' '}
                {Math.min(currentPage * itemsPerPage, sortedPhotos.length)} of{' '}
                {sortedPhotos.length} photos
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                >
                  Previous
                </button>
                <span className="px-3 py-1 text-sm text-gray-600">
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Preview Modal */}
      {previewPhoto && (
        <DocumentPreviewModal
          document={previewPhoto}
          onClose={() => setPreviewPhoto(null)}
        />
      )}
    </div>
  );
};
