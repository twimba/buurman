import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Download,
  Trash2,
  FileText,
  Image as ImageIcon,
  Filter,
  CheckSquare,
  Square,
  ChevronUp,
  ChevronDown,
} from 'lucide-react';
import {
  useDocuments,
  useDeleteDocument,
  useBulkDownload,
} from '@/hooks/useDocumentHooks';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { DocumentResponse } from '@/types/property';
import { format } from 'date-fns';

export const DocumentsPage = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('');
  const [selectedDocuments, setSelectedDocuments] = useState<Set<string>>(
    new Set()
  );
  const [previewDocument, setPreviewDocument] =
    useState<DocumentResponse | null>(null);

  // Sorting and pagination state
  const [sortField, setSortField] = useState<
    'title' | 'entityType' | 'fileSize' | 'uploadedAt'
  >('uploadedAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 20;

  const { data: allDocuments, isLoading } = useDocuments({
    search: searchTerm || undefined,
    entityType: entityTypeFilter || undefined,
  });

  // Filter out photos - only show documents
  const documents = allDocuments?.filter((doc) => doc.category !== 'PHOTO') || [];

  const deleteMutation = useDeleteDocument();
  const bulkDownloadMutation = useBulkDownload();

  // Sorting and pagination
  const sortedDocuments = useMemo(() => {
    if (!documents) return [];

    const sorted = [...documents];
    sorted.sort((a, b) => {
      let aVal: any, bVal: any;

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
  }, [documents, sortField, sortOrder]);

  const paginatedDocuments = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    return sortedDocuments.slice(startIndex, endIndex);
  }, [sortedDocuments, currentPage]);

  const totalPages = Math.ceil((sortedDocuments?.length || 0) / itemsPerPage);

  const handleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const handleDelete = (id: string) => {
    if (window.confirm('Are you sure you want to delete this document?')) {
      deleteMutation.mutate(id);
      setSelectedDocuments((prev) => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const handleBulkDownload = () => {
    if (selectedDocuments.size === 0) return;
    bulkDownloadMutation.mutate(Array.from(selectedDocuments));
  };

  const handleSelectAll = () => {
    if (sortedDocuments && selectedDocuments.size < sortedDocuments.length) {
      setSelectedDocuments(new Set(sortedDocuments.map((doc) => doc.id)));
    } else {
      setSelectedDocuments(new Set());
    }
  };

  const handleSelectDocument = (id: string) => {
    setSelectedDocuments((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) {
      return <ImageIcon className="h-8 w-8 text-blue-500" />;
    }
    return <FileText className="h-8 w-8 text-gray-500" />;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Document Library
        </h1>
        <p className="text-gray-600">
          Search and manage all your documents in one place
        </p>
      </div>

      {/* Search and Filter Bar */}
      <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search documents by title, filename, or notes..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Entity Type Filter */}
          <div className="w-full md:w-48 relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <select
              value={entityTypeFilter}
              onChange={(e) => setEntityTypeFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
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
        {selectedDocuments.size > 0 && (
          <div className="mt-4 pt-4 border-t border-gray-200 flex items-center gap-4">
            <span className="text-sm text-gray-600">
              {selectedDocuments.size} document(s) selected
            </span>
            <button
              onClick={handleBulkDownload}
              disabled={bulkDownloadMutation.isPending}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 flex items-center gap-2 disabled:opacity-50"
            >
              <Download className="h-4 w-4" />
              {bulkDownloadMutation.isPending
                ? 'Downloading...'
                : 'Download Selected'}
            </button>
          </div>
        )}
      </div>

      {/* Document List */}
      {isLoading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
          <p className="mt-2 text-gray-600">Loading documents...</p>
        </div>
      ) : !documents || documents.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg shadow-sm">
          <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600">No documents found</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left">
                  <button
                    onClick={handleSelectAll}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    {sortedDocuments &&
                    selectedDocuments.size === sortedDocuments.length ? (
                      <CheckSquare className="h-5 w-5" />
                    ) : (
                      <Square className="h-5 w-5" />
                    )}
                  </button>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('title')}
                >
                  <div className="flex items-center gap-1">
                    Document
                    {sortField === 'title' &&
                      (sortOrder === 'asc' ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      ))}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('entityType')}
                >
                  <div className="flex items-center gap-1">
                    Type
                    {sortField === 'entityType' &&
                      (sortOrder === 'asc' ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      ))}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('fileSize')}
                >
                  <div className="flex items-center gap-1">
                    Size
                    {sortField === 'fileSize' &&
                      (sortOrder === 'asc' ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      ))}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('uploadedAt')}
                >
                  <div className="flex items-center gap-1">
                    Uploaded
                    {sortField === 'uploadedAt' &&
                      (sortOrder === 'asc' ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      ))}
                  </div>
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {paginatedDocuments.map((doc) => (
                <tr
                  key={doc.id}
                  className={`hover:bg-gray-50 cursor-pointer ${
                    selectedDocuments.has(doc.id) ? 'bg-blue-50' : ''
                  }`}
                  onClick={() => setPreviewDocument(doc)}
                >
                  <td
                    className="px-6 py-4"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      onClick={() => handleSelectDocument(doc.id)}
                      className="text-gray-500 hover:text-gray-700"
                    >
                      {selectedDocuments.has(doc.id) ? (
                        <CheckSquare className="h-5 w-5 text-blue-600" />
                      ) : (
                        <Square className="h-5 w-5" />
                      )}
                    </button>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      {getFileIcon(doc.mimeType)}
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {doc.title ?? doc.fileName}
                        </div>
                        {doc.title && doc.title !== doc.fileName ? (
                          <div className="text-xs text-gray-500">
                            {doc.fileName}
                          </div>
                        ) : null}
                        {doc.notes ? (
                          <div className="text-xs text-gray-500 mt-1">
                            {doc.notes}
                          </div>
                        ) : null}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        const entityPath =
                          doc.entityType.toLowerCase() === 'property'
                            ? `/properties/${doc.entityId}`
                            : doc.entityType.toLowerCase() === 'tenant'
                              ? `/tenants/${doc.entityId}`
                              : doc.entityType.toLowerCase() === 'contract'
                                ? `/contracts/${doc.entityId}`
                                : doc.entityType.toLowerCase() === 'payment'
                                  ? `/payments/${doc.entityId}`
                                  : doc.entityType.toLowerCase() === 'expense'
                                    ? `/expenses/${doc.entityId}`
                                    : '#';
                        if (entityPath !== '#') navigate(entityPath);
                      }}
                      className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800 hover:bg-blue-100 hover:text-blue-800 transition-colors"
                    >
                      {doc.entityType}
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {formatFileSize(doc.fileSize)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {format(new Date(doc.uploadedAt), 'MMM d, yyyy')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div
                      className="flex justify-end gap-2"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <a
                        href={doc.downloadUrl || undefined}
                        download
                        className="text-blue-600 hover:text-blue-900"
                        title="Download"
                      >
                        <Download className="h-4 w-4" />
                      </a>
                      <button
                        onClick={() => handleDelete(doc.id)}
                        className="text-red-600 hover:text-red-900"
                        title="Delete"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
              <div className="text-sm text-gray-600">
                Showing {(currentPage - 1) * itemsPerPage + 1} to{' '}
                {Math.min(currentPage * itemsPerPage, sortedDocuments.length)}{' '}
                of {sortedDocuments.length} documents
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
      {previewDocument && (
        <DocumentPreviewModal
          document={previewDocument}
          onClose={() => setPreviewDocument(null)}
        />
      )}
    </div>
  );
};
