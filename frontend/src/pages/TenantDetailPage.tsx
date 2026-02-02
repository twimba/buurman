import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useTenant,
  useDeleteTenant,
  useTenantHistory,
  useUnlinkTenantFromProperty,
} from '@/hooks/useTenantHooks';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import {
  ArrowLeft,
  Edit,
  Trash2,
  Mail,
  Phone,
  User,
  CreditCard,
  IdCard,
  Home,
  X,
  Unlink,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

export const TenantDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'info' | 'history'>('info');
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { data: tenant, isLoading, error } = useTenant(id);
  const { data: history = [], isLoading: historyLoading } =
    useTenantHistory(id);
  const deleteTenantMutation = useDeleteTenant();
  const unlinkMutation = useUnlinkTenantFromProperty(id!);

  const handleDelete = async () => {
    if (!id) return;
    try {
      await deleteTenantMutation.mutateAsync(id);
      navigate('/tenants');
    } catch (err) {
      console.error('Failed to delete tenant:', err);
    }
  };

  const handleUnlink = async () => {
    try {
      await unlinkMutation.mutateAsync();
    } catch (err) {
      console.error('Failed to unlink tenant:', err);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !tenant) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load tenant" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/tenants')}
              className="p-2 hover:bg-gray-200 rounded transition-colors"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                {tenant.name}
              </h1>
              <p className="text-sm text-gray-500">#{tenant.identifier}</p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => navigate(`/tenants/${id}/edit`)}
              className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
            >
              <Edit className="h-4 w-4" />
              Edit
            </button>
            <button
              onClick={() => setShowDeleteModal(true)}
              className="flex items-center gap-2 px-4 py-2 border border-red-300 text-red-700 rounded hover:bg-red-50 transition-colors"
            >
              <Trash2 className="h-4 w-4" />
              Delete
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <div className="flex gap-6">
            <button
              onClick={() => setActiveTab('info')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'info'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Information
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`pb-3 px-1 font-medium transition-colors ${
                activeTab === 'history'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              History
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'info' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Contact Information */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Contact Information
              </h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Name</p>
                    <p className="font-medium text-gray-900">{tenant.name}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Email</p>
                    <p className="font-medium text-gray-900">{tenant.email}</p>
                  </div>
                </div>
                {tenant.phone && (
                  <div className="flex items-center gap-3">
                    <Phone className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Phone</p>
                      <p className="font-medium text-gray-900">
                        {tenant.phone}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Information */}
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Additional Information
              </h2>
              <div className="space-y-4">
                {tenant.taxNumber && (
                  <div className="flex items-center gap-3">
                    <CreditCard className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">Tax Number</p>
                      <p className="font-medium text-gray-900">
                        {tenant.taxNumber}
                      </p>
                    </div>
                  </div>
                )}
                {tenant.idNumber && (
                  <div className="flex items-center gap-3">
                    <IdCard className="h-5 w-5 text-gray-400" />
                    <div>
                      <p className="text-sm text-gray-500">ID Number</p>
                      <p className="font-medium text-gray-900">
                        {tenant.idNumber}
                      </p>
                    </div>
                  </div>
                )}
                {!tenant.taxNumber && !tenant.idNumber && (
                  <p className="text-sm text-gray-400 italic">
                    No additional information available
                  </p>
                )}
              </div>
            </div>

            {/* Current Property */}
            <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Current Property
              </h2>
              {tenant.currentProperty ? (
                <div className="flex items-center justify-between bg-green-50 p-4 rounded">
                  <div className="flex items-center gap-3">
                    <Home className="h-8 w-8 text-green-600" />
                    <div>
                      <p className="font-medium text-gray-900">
                        {tenant.currentProperty.street}
                      </p>
                      <p className="text-sm text-gray-600">
                        {tenant.currentProperty.city},{' '}
                        {tenant.currentProperty.postalCode}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={handleUnlink}
                    disabled={unlinkMutation.isPending}
                    className="flex items-center gap-2 px-4 py-2 border border-red-300 text-red-700 rounded hover:bg-red-50 transition-colors disabled:opacity-50"
                  >
                    <Unlink className="h-4 w-4" />
                    Unlink
                  </button>
                </div>
              ) : (
                <p className="text-sm text-gray-400 italic">
                  No property assigned
                </p>
              )}
            </div>
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white rounded-lg shadow">
            <div className="p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Property History
              </h2>
              {historyLoading ? (
                <LoadingSpinner />
              ) : history.length > 0 ? (
                <div className="space-y-4">
                  {history.map((record) => (
                    <div
                      key={record.id}
                      className="border-l-4 border-blue-500 pl-4 py-2"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="font-medium text-gray-900">
                            {record.property.street}, {record.property.city}
                          </p>
                          <p className="text-sm text-gray-600 mt-1">
                            {record.actionType === 'LINKED'
                              ? 'Linked to property'
                              : 'Unlinked from property'}
                          </p>
                          {record.movedInAt && (
                            <p className="text-sm text-gray-500 mt-1">
                              Moved in:{' '}
                              {new Date(record.movedInAt).toLocaleDateString()}
                            </p>
                          )}
                          {record.movedOutAt && (
                            <p className="text-sm text-gray-500">
                              Moved out:{' '}
                              {new Date(record.movedOutAt).toLocaleDateString()}
                            </p>
                          )}
                        </div>
                        <div className="text-right">
                          <p className="text-xs text-gray-500">
                            {formatDistanceToNow(new Date(record.performedAt), {
                              addSuffix: true,
                            })}
                          </p>
                          <p className="text-xs text-gray-500 mt-1">
                            by {record.performedBy}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-400 italic">No history yet</p>
              )}
            </div>
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900">
                  Delete Tenant
                </h3>
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
              <p className="text-gray-600 mb-6">
                Are you sure you want to delete this tenant? This action cannot
                be undone.
              </p>
              <div className="flex gap-3 justify-end">
                <button
                  onClick={() => setShowDeleteModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleDelete}
                  disabled={deleteTenantMutation.isPending}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition-colors disabled:opacity-50"
                >
                  {deleteTenantMutation.isPending ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
