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

// --- Dropdown option constants (must match backend CHECK constraints exactly) ---

export const CONSTRUCTION_TYPES = [
  'BRICK',
  'CONCRETE',
  'WOOD',
  'STEEL',
  'MIXED',
  'OTHER',
] as const;
export const FOUNDATION_TYPES = [
  'CONCRETE_SLAB',
  'CRAWL_SPACE',
  'BASEMENT',
  'PILE',
  'OTHER',
] as const;
export const ROOF_TYPES = [
  'FLAT',
  'PITCHED',
  'HIP',
  'GABLE',
  'MANSARD',
  'OTHER',
] as const;
export const FLOORING_TYPES = [
  'HARDWOOD',
  'LAMINATE',
  'TILE',
  'VINYL',
  'CARPET',
  'CONCRETE',
  'MIXED',
  'OTHER',
] as const;
export const WINDOW_TYPES = [
  'SINGLE_PANE',
  'DOUBLE_PANE',
  'TRIPLE_PANE',
  'OTHER',
] as const;
export const HEATING_TYPES = [
  'CENTRAL',
  'DISTRICT',
  'HEAT_PUMP',
  'ELECTRIC',
  'GAS',
  'NONE',
  'OTHER',
] as const;
export const COOLING_TYPES = [
  'CENTRAL_AC',
  'SPLIT_AC',
  'EVAPORATIVE',
  'NONE',
  'OTHER',
] as const;
export const HOT_WATER_SYSTEMS = [
  'BOILER',
  'TANKLESS',
  'HEAT_PUMP',
  'SOLAR',
  'ELECTRIC',
  'OTHER',
] as const;
export const SEWAGE_TYPES = ['MUNICIPAL', 'SEPTIC', 'OTHER'] as const;
export const INTERNET_CONNECTION_TYPES = [
  'FIBER',
  'CABLE',
  'DSL',
  'NONE',
  'OTHER',
] as const;
export const INTERNET_STATUSES = [
  'ACTIVE',
  'AVAILABLE',
  'NOT_AVAILABLE',
  'UNKNOWN',
] as const;
export const PARKING_TYPES = [
  'GARAGE',
  'CARPORT',
  'DRIVEWAY',
  'STREET',
  'UNDERGROUND',
  'NONE',
  'OTHER',
] as const;
export const OUTDOOR_AREA_TYPES = [
  'BALCONY',
  'TERRACE',
  'GARDEN',
  'ROOFTOP',
  'PATIO',
  'YARD',
  'OTHER',
] as const;
export const AREA_UNITS = ['sqm', 'sqft'] as const;
export const ENERGY_EFFICIENCY_RATINGS = [
  'A++',
  'A+',
  'A',
  'B',
  'C',
  'D',
  'E',
  'F',
  'G',
] as const;
export const ELECTRICITY_CONNECTION_TYPES = [
  'SINGLE_PHASE',
  'THREE_PHASE',
] as const;
export const WATER_CONNECTION_TYPES = [
  'MUNICIPAL',
  'WELL',
  'SHARED',
  'OTHER',
] as const;

// --- Outdoor Areas ---

export interface OutdoorAreaResponse {
  identifier: string;
  type: string;
  areaValue: number | null;
  areaUnit: string;
}

export interface OutdoorAreaRequest {
  type: string;
  areaValue: number | null;
  areaUnit?: string;
}

// --- Amenities ---

export interface AmenityResponse {
  identifier: string;
  name: string;
  category: string;
  icon: string | null;
}

export interface PropertyAmenityResponse {
  amenityIdentifier: string;
  amenityName: string;
  amenityCategory: string;
  amenityIcon: string | null;
  notes: string | null;
}

export interface PropertyAmenityRequest {
  amenityIdentifier: string;
  notes?: string;
}

// --- Property ---

export interface PropertySummary {
  identifier: string;
  street: string;
  city: string;
  postalCode: string;
  propertyType: PropertyType;
  status: PropertyStatus;
}

export interface PropertyResponse {
  identifier: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude: number | null;
  longitude: number | null;
  bedrooms: number | null;
  bathrooms: number | null;
  areaValue: number | null;
  areaUnit: string;
  propertyType: string;
  status: string;
  mainPhotoUrl: string | null;
  mainPhotoThumbnailUrl: string | null;

  // Construction & Structure
  yearBuilt: number | null;
  yearLastRenovated: number | null;
  constructionType: string | null;
  foundationType: string | null;
  roofType: string | null;
  wallConstruction: string | null;
  flooringType: string | null;
  windowType: string | null;
  numberOfFloors: number | null;
  structuralNotes: string | null;

  // Energy & Climate
  energyEfficiencyRating: string | null;
  energyCertificateExpiryDate: string | null;
  heatingType: string | null;
  coolingType: string | null;
  hotWaterSystem: string | null;
  insulationNotes: string | null;

  // Utilities & Connections
  electricityConnectionType: string | null;
  electricityCapacityAmps: number | null;
  waterConnectionType: string | null;
  hasGasConnection: boolean;
  sewageType: string | null;
  internetConnectionType: string | null;
  internetMaxSpeedMbps: number | null;
  internetStatus: string | null;

  // Parking
  parkingSpaces: number | null;
  parkingType: string | null;

  // Safety & Security
  hasSmokeDetectors: boolean;
  hasCoDetectors: boolean;
  hasFireExtinguisher: boolean;
  hasSprinklerSystem: boolean;
  hasAlarmSystem: boolean;
  hasSecurityCameras: boolean;
  hasSecureEntry: boolean;
  safetyNotes: string | null;

  // Accessibility
  isWheelchairAccessible: boolean;
  hasElevator: boolean;
  hasStepFreeEntrance: boolean;
  hasAdaptedBathroom: boolean;
  accessibilityNotes: string | null;

  // Nested (only on detail endpoint, null on list)
  outdoorAreas: OutdoorAreaResponse[] | null;
  amenities: PropertyAmenityResponse[] | null;

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
  areaValue?: number | null;
  areaUnit?: string | null;
  propertyType: PropertyType;
  status: PropertyStatus;

  // Construction & Structure
  yearBuilt?: number | null;
  yearLastRenovated?: number | null;
  constructionType?: string | null;
  foundationType?: string | null;
  roofType?: string | null;
  wallConstruction?: string | null;
  flooringType?: string | null;
  windowType?: string | null;
  numberOfFloors?: number | null;
  structuralNotes?: string | null;

  // Energy & Climate
  energyEfficiencyRating?: string | null;
  energyCertificateExpiryDate?: string | null;
  heatingType?: string | null;
  coolingType?: string | null;
  hotWaterSystem?: string | null;
  insulationNotes?: string | null;

  // Utilities & Connections
  electricityConnectionType?: string | null;
  electricityCapacityAmps?: number | null;
  waterConnectionType?: string | null;
  hasGasConnection?: boolean;
  sewageType?: string | null;
  internetConnectionType?: string | null;
  internetMaxSpeedMbps?: number | null;
  internetStatus?: string | null;

  // Parking
  parkingSpaces?: number | null;
  parkingType?: string | null;

  // Safety & Security
  hasSmokeDetectors?: boolean;
  hasCoDetectors?: boolean;
  hasFireExtinguisher?: boolean;
  hasSprinklerSystem?: boolean;
  hasAlarmSystem?: boolean;
  hasSecurityCameras?: boolean;
  hasSecureEntry?: boolean;
  safetyNotes?: string | null;

  // Accessibility
  isWheelchairAccessible?: boolean;
  hasElevator?: boolean;
  hasStepFreeEntrance?: boolean;
  hasAdaptedBathroom?: boolean;
  accessibilityNotes?: string | null;
}

export type UpdatePropertyRequest = CreatePropertyRequest;

export interface DocumentResponse {
  identifier: string;
  entityType: string;
  entityIdentifier: string;
  fileKey: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  title: string | null;
  notes: string | null;
  uploadedAt: string;
  downloadUrl: string | null;
}

export interface PhotoResponse {
  identifier: string;
  entityType: string;
  entityIdentifier: string;
  fileKey: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  title: string | null;
  notes: string | null;
  isMainPhoto: boolean;
  uploadedAt: string;
  downloadUrl: string | null;
  thumbnailUrl: string | null;
}

export interface AuditLogEntry {
  entityType: string;
  entityIdentifier: string;
  entityName: string;
  action: 'CREATE' | 'UPDATE' | 'DELETE';
  userName: string;
  timestamp: string;
  description: string;
  changedFields?: Record<string, unknown>;
  oldValues?: Record<string, unknown>;
  newValues?: Record<string, unknown>;
}
