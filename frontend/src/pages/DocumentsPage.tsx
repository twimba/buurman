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
  Folder,
} from 'lucide-react';
import {
  useDocuments,
  useDeleteDocument,
  useBulkDownload,
} from '@/hooks/useDocumentHooks';
import { DocumentPreviewModal } from '@/components/documents/DocumentPreviewModal';
import { DocumentResponse } from '@/types/property';
import { useFormatDate } from '@/hooks/useFormatDate';
import { useTeam } from '@/context/TeamContext';

export const DocumentsPage = () => {
  const navigate = useNavigate();
  const { canEditData } = useTeam();
  const { formatDate } = useFormatDate();
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
  const documents = useMemo(
    () => allDocuments?.filter((doc) => doc.category !== 'PHOTO') || [],
    [allDocuments]
  );

  const deleteMutation = useDeleteDocument();
  const bulkDownloadMutation = useBulkDownload();

  // Sorting and pagination
  const sortedDocuments = useMemo(() => {
    if (!documents) return [];

    const sorted = [...documents];
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
    return <FileText className="h-8 w-8 text-[#6b7194] dark:text-[#8b90a8]" />;
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
          <Folder className="h-8 w-8 text-primary-500 dark:text-primary-300" />
          <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
            Document Library
          </h1>
        </div>
        <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
          Search and manage all your documents in one place
        </p>
      </div>

      {/* Search and Filter Bar */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm p-4 mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <input
              type="text"
              placeholder="Search documents by title, filename, or notes..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6]"
            />
          </div>

          {/* Entity Type Filter */}
          <div className="w-full md:w-48 relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
            <select
              value={entityTypeFilter}
              onChange={(e) => setEntityTypeFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-[#14161f] text-[#1a1d2e] dark:text-[#eef0f6]"
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
          <div className="mt-4 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f] flex items-center gap-4">
            <span className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
              {selectedDocuments.size} document(s) selected
            </span>
            <button
              onClick={handleBulkDownload}
              disabled={bulkDownloadMutation.isPending}
              className="px-4 py-2 bg-[#5c7cfa] text-white rounded-md hover:bg-[#4c6ef5] flex items-center gap-2 disabled:opacity-50"
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
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-[#1e2130] dark:border-[#edf0f7] dark:border-[#2a2e3f]"></div>
          <p className="mt-2 text-[#6b7194] dark:text-[#8b90a8]">
            Loading documents...
          </p>
        </div>
      ) : !documents || documents.length === 0 ? (
        <div className="text-center py-12 bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
          <FileText className="h-12 w-12 text-[#9ca0b8] dark:text-[#5c6180] mx-auto mb-4" />
          <p className="text-[#6b7194] dark:text-[#8b90a8]">
            No documents found
          </p>
        </div>
      ) : (
        <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-[#edf0f7] dark:divide-[#2a2e3f] dark:divide-[#2a2e3f]">
            <thead className="bg-[#f8f9fc] dark:bg-[#0c0d14]">
              <tr>
                <th className="px-6 py-3 text-left">
                  <button
                    onClick={handleSelectAll}
                    className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
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
                  className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                  className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                  className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                  className="px-6 py-3 text-left text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider cursor-pointer hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130]"
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
                <th className="px-6 py-3 text-right text-xs font-medium text-[#6b7194] dark:text-[#8b90a8] uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-[#14161f] divide-y divide-[#edf0f7] dark:divide-[#2a2e3f]">
              {paginatedDocuments.map((doc) => (
                <tr
                  key={doc.id}
                  className={`hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] cursor-pointer ${
                    selectedDocuments.has(doc.id)
                      ? 'bg-blue-50 dark:bg-blue-900/20'
                      : ''
                  }`}
                  onClick={() => setPreviewDocument(doc)}
                >
                  <td
                    className="px-6 py-4"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      onClick={() => handleSelectDocument(doc.id)}
                      className="text-[#6b7194] dark:text-[#8b90a8] hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
                    >
                      {selectedDocuments.has(doc.id) ? (
                        <CheckSquare className="h-5 w-5 text-[#5c7cfa] dark:text-[#91a7ff]" />
                      ) : (
                        <Square className="h-5 w-5" />
                      )}
                    </button>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      {getFileIcon(doc.mimeType)}
                      <div>
                        <div className="text-sm font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                          {doc.title ?? doc.fileName}
                        </div>
                        {doc.title && doc.title !== doc.fileName ? (
                          <div className="text-xs text-[#6b7194] dark:text-[#8b90a8]">
                            {doc.fileName}
                          </div>
                        ) : null}
                        {doc.notes ? (
                          <div className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
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
                      className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-[#f1f3f9] dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#c4c8db] hover:bg-blue-100 hover:text-blue-800 transition-colors"
                    >
                      {doc.entityType}
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
                    {formatFileSize(doc.fileSize)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#6b7194] dark:text-[#8b90a8]">
                    {formatDate(doc.uploadedAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div
                      className="flex justify-end gap-2"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <a
                        href={doc.downloadUrl || undefined}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-[#5c7cfa] hover:text-blue-900"
                        title="Download"
                      >
                        <Download className="h-4 w-4" />
                      </a>
                      {canEditData && (
                        <button
                          onClick={() => handleDelete(doc.id)}
                          className="text-red-600 hover:text-red-900"
                          title="Delete"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="px-6 py-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f] flex items-center justify-between">
              <div className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Showing {(currentPage - 1) * itemsPerPage + 1} to{' '}
                {Math.min(currentPage * itemsPerPage, sortedDocuments.length)}{' '}
                of {sortedDocuments.length} documents
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]"
                >
                  Previous
                </button>
                <span className="px-3 py-1 text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 border border-[#c9cfd9] dark:border-[#3a3f54] rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db]"
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
