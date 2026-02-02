import { PropertySummary } from './property';

export interface TenantResponse {
  id: string;
  identifier: string;
  teamId: string;
  name: string;
  email: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
  currentProperty?: PropertySummary;
  createdAt: string;
  updatedAt: string;
}

export interface TenantSummary {
  id: string;
  identifier: string;
  name: string;
  email: string;
  phone?: string;
}

export interface CreateTenantRequest {
  name: string;
  email: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
}

export interface UpdateTenantRequest {
  name: string;
  email: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
}

export interface LinkTenantToPropertyRequest {
  propertyId: string;
  movedInAt?: string;
}

export enum PropertyTenantActionType {
  LINKED = 'LINKED',
  UNLINKED = 'UNLINKED',
}

export interface PropertyTenantHistoryResponse {
  id: string;
  property: PropertySummary;
  movedInAt?: string;
  movedOutAt?: string;
  actionType: PropertyTenantActionType;
  performedBy: string;
  performedAt: string;
}
