# Phase 2.1 Frontend Implementation Guide

## ✅ Already Completed

### 1. Types (`/frontend/src/types/tenant.ts`)
All TypeScript interfaces created matching backend DTOs.

### 2. API Client (`/frontend/src/api/tenants.ts`)
Complete API client with all endpoints:
- getTenants(search?)
- getTenant(id)
- createTenant(data)
- updateTenant(id, data)
- deleteTenant(id)
- linkTenantToProperty(tenantId, data)
- unlinkTenantFromProperty(tenantId)
- getTenantHistory(tenantId)

### 3. React Hooks (`/frontend/src/hooks/useTenantHooks.ts`)
All custom hooks created:
- useTenants(search)
- useTenant(id)
- useCreateTenant()
- useUpdateTenant(id)
- useDeleteTenant()
- useLinkTenantToProperty(tenantId)
- useUnlinkTenantFromProperty(tenantId)
- useTenantHistory(tenantId)

---

## 🔨 Components to Create

### TenantCard Component
**File**: `/frontend/src/components/tenants/TenantCard.tsx`

```tsx
import { TenantResponse } from '@/types/tenant';
import { User, Mail, Phone, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface TenantCardProps {
  tenant: TenantResponse;
}

export const TenantCard = ({ tenant }: TenantCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer p-6"
      onClick={() => navigate(`/tenants/${tenant.id}`)}
    >
      {/* Tenant Name with Icon */}
      <div className="flex items-center gap-3 mb-4">
        <div className="bg-blue-100 rounded-full p-3">
          <User className="h-6 w-6 text-blue-600" />
        </div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900">{tenant.name}</h3>
          <p className="text-sm text-gray-500">#{tenant.identifier}</p>
        </div>
      </div>

      {/* Contact Info */}
      <div className="space-y-2 mb-4">
        <div className="flex items-center gap-2 text-gray-600">
          <Mail className="h-4 w-4" />
          <span className="text-sm">{tenant.email}</span>
        </div>
        {tenant.phone && (
          <div className="flex items-center gap-2 text-gray-600">
            <Phone className="h-4 w-4" />
            <span className="text-sm">{tenant.phone}</span>
          </div>
        )}
      </div>

      {/* Current Property */}
      {tenant.currentProperty ? (
        <div className="flex items-center gap-2 bg-green-50 text-green-700 px-3 py-2 rounded">
          <Home className="h-4 w-4" />
          <span className="text-sm font-medium">
            {tenant.currentProperty.street}, {tenant.currentProperty.city}
          </span>
        </div>
      ) : (
        <div className="text-sm text-gray-400 italic">No property assigned</div>
      )}
    </div>
  );
};
```

### TenantForm Component
**File**: `/frontend/src/components/tenants/TenantForm.tsx`

```tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Save } from 'lucide-react';
import {
  TenantResponse,
  CreateTenantRequest,
} from '@/types/tenant';

interface TenantFormProps {
  tenant?: TenantResponse;
  onSubmit: (data: CreateTenantRequest) => Promise<void>;
  isLoading: boolean;
}

export const TenantForm = ({
  tenant,
  onSubmit,
  isLoading,
}: TenantFormProps) => {
  const navigate = useNavigate();
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateTenantRequest>({
    name: tenant?.name || '',
    email: tenant?.email || '',
    phone: tenant?.phone || '',
    taxNumber: tenant?.taxNumber || '',
    idNumber: tenant?.idNumber || '',
  });

  useEffect(() => {
    if (tenant) {
      setFormData({
        name: tenant.name,
        email: tenant.email,
        phone: tenant.phone || '',
        taxNumber: tenant.taxNumber || '',
        idNumber: tenant.idNumber || '',
      });
    }
  }, [tenant]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) newErrors.name = 'Name is required';
    if (!formData.email.trim()) newErrors.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Email must be valid';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      await onSubmit(formData);
      navigate('/tenants');
    } catch (error) {
      console.error('Failed to save tenant:', error);
    }
  };

  const handleChange = (field: keyof CreateTenantRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Personal Information */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Personal Information
        </h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Name <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => handleChange('name', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="John Doe"
            />
            {errors.name && (
              <p className="text-red-600 text-sm mt-1">{errors.name}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email <span className="text-red-500">*</span>
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => handleChange('email', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="john@example.com"
            />
            {errors.email && (
              <p className="text-red-600 text-sm mt-1">{errors.email}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone
            </label>
            <input
              type="tel"
              value={formData.phone || ''}
              onChange={(e) => handleChange('phone', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="+31 6 1234 5678"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tax Number
            </label>
            <input
              type="text"
              value={formData.taxNumber || ''}
              onChange={(e) => handleChange('taxNumber', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="123456789"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              ID Number
            </label>
            <input
              type="text"
              value={formData.idNumber || ''}
              onChange={(e) => handleChange('idNumber', e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
              placeholder="AB123456"
            />
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 justify-end mt-6 pt-6 border-t">
        <button
          type="button"
          onClick={() => navigate('/tenants')}
          className="border border-gray-300 px-4 py-2 rounded hover:bg-gray-50 transition-colors flex items-center gap-2"
          disabled={isLoading}
        >
          <X className="h-4 w-4" />
          Cancel
        </button>
        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center gap-2"
          disabled={isLoading}
        >
          <Save className="h-4 w-4" />
          {isLoading ? 'Saving...' : tenant ? 'Update Tenant' : 'Create Tenant'}
        </button>
      </div>
    </form>
  );
};
```

### TenantListPage
**File**: `/frontend/src/pages/TenantListPage.tsx`

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTenants } from '@/hooks/useTenantHooks';
import { TenantCard } from '@/components/tenants/TenantCard';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';
import { Plus, User, Search } from 'lucide-react';

export const TenantListPage = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const { data: tenants, isLoading, error } = useTenants(debouncedSearch);

  // Debounce search
  const handleSearch = (value: string) => {
    setSearchTerm(value);
    const timer = setTimeout(() => {
      setDebouncedSearch(value);
    }, 500);
    return () => clearTimeout(timer);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load tenants" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Tenants</h1>
          <button
            onClick={() => navigate('/tenants/new')}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Plus className="h-5 w-5" />
            Add Tenant
          </button>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search by name, email, or phone..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded focus:border-blue-600 focus:ring-1 focus:ring-blue-600"
            />
          </div>
        </div>

        {/* Tenant Count */}
        <p className="text-sm text-gray-600 mb-4">
          {tenants?.length || 0} {tenants?.length === 1 ? 'tenant' : 'tenants'}
        </p>

        {/* Tenants Grid */}
        {tenants && tenants.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {tenants.map((tenant) => (
              <TenantCard key={tenant.id} tenant={tenant} />
            ))}
          </div>
        ) : (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-16 bg-white rounded-lg">
            <User className="h-16 w-16 text-gray-300 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No tenants yet
            </h3>
            <p className="text-gray-600 mb-6">
              Get started by adding your first tenant
            </p>
            <button
              onClick={() => navigate('/tenants/new')}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <Plus className="h-5 w-5" />
              Add Tenant
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
```

### Remaining Files

Create these additional files following the same patterns as properties:

1. **TenantDetailPage.tsx** - Similar to PropertyDetailPage with tabs for Info, History, Documents
2. **TenantCreatePage.tsx** - Wraps TenantForm with useCreateTenant hook
3. **TenantEditPage.tsx** - Wraps TenantForm with useUpdateTenant hook

---

## 📝 Router Integration

Add to your router configuration:

```tsx
import { TenantListPage } from '@/pages/TenantListPage';
import { TenantDetailPage } from '@/pages/TenantDetailPage';
import { TenantCreatePage } from '@/pages/TenantCreatePage';
import { TenantEditPage } from '@/pages/TenantEditPage';

// In your routes:
{
  path: '/tenants',
  element: <TenantListPage />,
},
{
  path: '/tenants/new',
  element: <TenantCreatePage />,
},
{
  path: '/tenants/:id',
  element: <TenantDetailPage />,
},
{
  path: '/tenants/:id/edit',
  element: <TenantEditPage />,
},
```

## 🧭 Navigation Update

Add to your sidebar navigation:

```tsx
{
  name: 'Tenants',
  href: '/tenants',
  icon: User, // from lucide-react
}
```

---

## ✅ Testing Checklist

After implementation, test:

- [ ] List tenants page loads
- [ ] Search filters tenants
- [ ] Create new tenant
- [ ] Edit tenant
- [ ] Delete tenant (with confirmation)
- [ ] View tenant detail
- [ ] Link tenant to property
- [ ] Unlink tenant from property
- [ ] View tenant history

All backend functionality is ready and waiting!
