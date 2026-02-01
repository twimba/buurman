import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useProperty,
  useDeleteProperty,
  usePropertyDocuments,
  useUploadPropertyDocument,
  useDeleteDocument,
} from '@/hooks/usePropertyHooks';
import { PropertyStatus } from '@/types/property';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { DocumentList } from '@/components/properties/DocumentList';
import { ArrowLeft, Edit, Trash2, Bed, Bath, Ruler, MapPin } from 'lucide-react';

const statusColors: Record<PropertyStatus, string> = {
  VACANT: 'bg-accent-100 text-accent-800',
  OCCUPIED: 'bg-primary-100 text-primary-800',
  MAINTENANCE: 'bg-yellow-100 text-yellow-800',
  UNAVAILABLE: 'bg-gray-100 text-gray-800',
};

const statusLabels: Record<PropertyStatus, string> = {
  VACANT: 'Vacant',
  OCCUPIED: 'Occupied',
  MAINTENANCE: 'Maintenance',
  UNAVAILABLE: 'Unavailable',
};

export const PropertyDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'info' | 'documents'>('info');
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { data: property, isLoading, error } = useProperty(id);
  const { data: documents = [], isLoading: docsLoading, error: docsError } = usePropertyDocuments(id);
  const deletePropertyMutation = useDeleteProperty();
  const uploadDocumentMutation = useUploadPropertyDocument(id!);
  const deleteDocumentMutation = useDeleteDocument(id!);

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deletePropertyMutation.mutateAsync(id);
      navigate('/properties');
    } catch (err) {
      console.error('Failed to delete property:', err);
    }
  };

  const handleUploadDocument = async (file: File, title?: string, notes?: string) => {
    await uploadDocumentMutation.mutateAsync({ file, title, notes });
  };

  const handleDeleteDocument = async (documentId: string) => {
    await deleteDocumentMutation.mutateAsync(documentId);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Property not found" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-start mb-6">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/properties')}
              className="p-2 hover:bg-gray-200 rounded transition-colors"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                {property.street}
              </h1>
              <p className="text-gray-600">
                {property.city}, {property.postalCode}
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => navigate(`/properties/${id}/edit`)}
              className="bg-white border border-gray-300 px-4 py-2 rounded hover:bg-background transition-colors flex items-center gap-2"
            >
              <Edit className="h-4 w-4" />
              Edit
            </button>
            <button
              onClick={() => setShowDeleteModal(true)}
              className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition-colors flex items-center gap-2"
            >
              <Trash2 className="h-4 w-4" />
              Delete
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b mb-6">
          <div className="flex gap-8">
            <button
              onClick={() => setActiveTab('info')}
              className={`px-4 py-2 border-b-2 transition-colors ${
                activeTab === 'info'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-gray-600 hover:text-gray-900'
              }`}
            >
              Info
            </button>
            <button
              onClick={() => setActiveTab('documents')}
              className={`px-4 py-2 border-b-2 transition-colors ${
                activeTab === 'documents'
                  ? 'border-primary text-primary font-semibold'
                  : 'border-transparent text-gray-600 hover:text-gray-900'
              }`}
            >
              Documents {documents.length > 0 && `(${documents.length})`}
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'info' ? (
          <div className="bg-white rounded-lg shadow p-6 space-y-6">
            {/* Status Badge */}
            <div>
              <span className={`px-4 py-2 rounded-full text-sm font-semibold ${statusColors[property.status]}`}>
                {statusLabels[property.status]}
              </span>
            </div>

            {/* Specifications Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {property.bedrooms !== null && (
                <div>
                  <div className="flex items-center gap-2 text-gray-600 mb-1">
                    <Bed className="h-5 w-5" />
                    <span className="text-sm font-medium">Bedrooms</span>
                  </div>
                  <p className="text-2xl font-semibold text-gray-900">{property.bedrooms}</p>
                </div>
              )}

              {property.bathrooms !== null && (
                <div>
                  <div className="flex items-center gap-2 text-gray-600 mb-1">
                    <Bath className="h-5 w-5" />
                    <span className="text-sm font-medium">Bathrooms</span>
                  </div>
                  <p className="text-2xl font-semibold text-gray-900">{property.bathrooms}</p>
                </div>
              )}

              {property.squareMeters !== null && (
                <div>
                  <div className="flex items-center gap-2 text-gray-600 mb-1">
                    <Ruler className="h-5 w-5" />
                    <span className="text-sm font-medium">Square Meters</span>
                  </div>
                  <p className="text-2xl font-semibold text-gray-900">{property.squareMeters}m²</p>
                </div>
              )}

              <div>
                <div className="flex items-center gap-2 text-gray-600 mb-1">
                  <MapPin className="h-5 w-5" />
                  <span className="text-sm font-medium">Type</span>
                </div>
                <p className="text-lg font-semibold text-gray-900">
                  {property.propertyType.replace('_', ' ')}
                </p>
              </div>

              <div>
                <div className="flex items-center gap-2 text-gray-600 mb-1">
                  <MapPin className="h-5 w-5" />
                  <span className="text-sm font-medium">Street</span>
                </div>
                <p className="text-lg text-gray-900">{property.street}</p>
              </div>

              <div>
                <div className="flex items-center gap-2 text-gray-600 mb-1">
                  <MapPin className="h-5 w-5" />
                  <span className="text-sm font-medium">City</span>
                </div>
                <p className="text-lg text-gray-900">{property.city}</p>
              </div>

              <div>
                <div className="flex items-center gap-2 text-gray-600 mb-1">
                  <MapPin className="h-5 w-5" />
                  <span className="text-sm font-medium">Postal Code</span>
                </div>
                <p className="text-lg text-gray-900">{property.postalCode}</p>
              </div>

              <div>
                <div className="flex items-center gap-2 text-gray-600 mb-1">
                  <MapPin className="h-5 w-5" />
                  <span className="text-sm font-medium">Country</span>
                </div>
                <p className="text-lg text-gray-900">{property.country}</p>
              </div>
            </div>

            {/* Metadata */}
            <div className="pt-6 border-t">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">Metadata</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-600">Created:</span>{' '}
                  <span className="text-gray-900">
                    {new Date(property.createdAt).toLocaleDateString()} at{' '}
                    {new Date(property.createdAt).toLocaleTimeString()}
                  </span>
                </div>
                <div>
                  <span className="text-gray-600">Last Updated:</span>{' '}
                  <span className="text-gray-900">
                    {new Date(property.updatedAt).toLocaleDateString()} at{' '}
                    {new Date(property.updatedAt).toLocaleTimeString()}
                  </span>
                </div>
              </div>
            </div>
          </div>
        ) : (
          <DocumentList
            propertyId={id!}
            documents={documents}
            isLoading={docsLoading}
            error={docsError}
            onUpload={handleUploadDocument}
            onDelete={handleDeleteDocument}
            isUploading={uploadDocumentMutation.isPending}
            isDeleting={deleteDocumentMutation.isPending}
          />
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Delete Property
            </h3>
            <p className="text-gray-600 mb-2">
              Are you sure you want to delete this property?
            </p>
            <p className="text-sm text-red-600 mb-6">
              This action cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="border border-gray-300 px-4 py-2 rounded hover:bg-background transition-colors"
                disabled={deletePropertyMutation.isPending}
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition-colors disabled:opacity-50"
                disabled={deletePropertyMutation.isPending}
              >
                {deletePropertyMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
