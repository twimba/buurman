export enum PropertyType {
  APARTMENT = 'APARTMENT',
  HOUSE = 'HOUSE',
  STUDIO = 'STUDIO',
  COMMERCIAL = 'COMMERCIAL',
}

export enum PropertyStatus {
  VACANT = 'VACANT',
  OCCUPIED = 'OCCUPIED',
  MAINTENANCE = 'MAINTENANCE',
  UNAVAILABLE = 'UNAVAILABLE',
}

export interface PropertySummary {
  id: string;
  identifier: string;
  street: string;
  city: string;
  postalCode: string;
  propertyType: PropertyType;
  status: PropertyStatus;
}

export interface PropertyResponse {
  id: string;
  identifier: string;
  teamId: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude: number | null;
  longitude: number | null;
  bedrooms: number | null;
  bathrooms: number | null;
  squareMeters: number | null;
  propertyType: PropertyType;
  status: PropertyStatus;
  mainPhotoUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePropertyRequest {
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  bedrooms: number | null;
  bathrooms: number | null;
  squareMeters: number | null;
  propertyType: PropertyType;
  status: PropertyStatus;
}

export type UpdatePropertyRequest = CreatePropertyRequest;

export interface DocumentResponse {
  id: string;
  teamId: string;
  entityType: string;
  entityId: string;
  fileKey: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  title: string | null;
  notes: string | null;
  category: string;
  isMainPhoto: boolean;
  uploadedBy: string;
  uploadedAt: string;
  downloadUrl: string | null;
}
