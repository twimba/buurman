// Enums — re-exported from generated
export {
  PropertyResponsePropertyCategory as PropertyCategory,
  type PropertyResponsePropertyCategory,
} from '../generated/models';

export {
  PropertyResponsePropertyType as PropertyType,
  type PropertyResponsePropertyType,
} from '../generated/models';

export {
  PropertyResponseStatus as PropertyStatus,
  type PropertyResponseStatus,
} from '../generated/models';

// Imports for manual interfaces and constants

import { PropertyResponsePropertyCategory } from '../generated/models';
import { PropertyResponsePropertyType } from '../generated/models';
import { PropertyResponseStatus } from '../generated/models';

// --- Category-specific detail interfaces (response) ---

export interface ResidentialDetailsResponse {
  bedrooms: number | null;
  bathrooms: number | null;
  furnished: boolean | null;
  petPolicy: string | null;
}

export interface CommercialDetailsResponse {
  usableAreaValue: number | null;
  usableAreaUnit: string | null;
  commonAreaValue: number | null;
  commonAreaUnit: string | null;
  floorLevel: number | null;
  ceilingHeightM: number | null;
  hasStorefront: boolean | null;
  hasSignageRights: boolean | null;
  zoningClassification: string | null;
  maxOccupancy: number | null;
  restroomCount: number | null;
  hasKitchenFacility: boolean | null;
  accessibilityCompliant: boolean | null;
}

export interface IndustrialDetailsResponse {
  clearHeightM: number | null;
  loadingDocks: number | null;
  driveInDoors: number | null;
  floorLoadCapacityKgSqm: number | null;
  powerCapacityKva: number | null;
  hasThreePhasePower: boolean | null;
  hasCrane: boolean | null;
  craneCapacityTons: number | null;
  hasHazmatCertification: boolean | null;
  hasVentilationSystem: boolean | null;
  hasClimateControl: boolean | null;
  yardAreaValue: number | null;
  yardAreaUnit: string | null;
  zoningClassification: string | null;
}

export interface AgriculturalDetailsResponse {
  totalLandAreaValue: number | null;
  totalLandAreaUnit: string | null;
  arableAreaValue: number | null;
  arableAreaUnit: string | null;
  soilType: string | null;
  hasWaterRights: boolean | null;
  waterSource: string | null;
  irrigationType: string | null;
  fencingType: string | null;
  hasOutbuildings: boolean | null;
  outbuildingDetails: string | null;
  currentUse: string | null;
  zoningClassification: string | null;
}

// --- Outdoor Areas ---

export interface OutdoorAreaResponse {
  identifier: string;
  type: string;
  areaValue: number | null;
  areaUnit: string;
}

// --- Amenities ---

export interface AmenityResponse {
  identifier: string;
  name: string;
  category: string;
  icon: string | null;
  applicableCategories: string[] | null;
}

export interface PropertyAmenityResponse {
  amenityIdentifier: string;
  amenityName: string;
  amenityCategory: string;
  amenityIcon: string | null;
  notes: string | null;
}

// --- Property ---

export interface PropertySummary {
  identifier: string;
  street: string;
  city: string;
  postalCode: string;
  propertyCategory: PropertyResponsePropertyCategory;
  propertyType: PropertyResponsePropertyType;
  status: PropertyResponseStatus;
}

export interface PropertyResponse {
  identifier: string;
  propertyCategory: PropertyResponsePropertyCategory;
  propertyType: string;
  status: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude: number | null;
  longitude: number | null;
  geocodeAccuracy: string | null;
  areaValue: number | null;
  areaUnit: string;
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

  // Category-specific details (only one is non-null based on category)
  residentialDetails: ResidentialDetailsResponse | null;
  commercialDetails: CommercialDetailsResponse | null;
  industrialDetails: IndustrialDetailsResponse | null;
  agriculturalDetails: AgriculturalDetailsResponse | null;

  // Nested collections (only on detail endpoint, null on list)
  outdoorAreas: OutdoorAreaResponse[] | null;
  amenities: PropertyAmenityResponse[] | null;

  createdAt: string;
  updatedAt: string;
}

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

// --- Request interfaces (manual — generated adds | null to all optional fields) ---

export interface ResidentialDetailsRequest {
  bedrooms?: number | null;
  bathrooms?: number | null;
  furnished?: boolean | null;
  petPolicy?: string | null;
}

export interface CommercialDetailsRequest {
  usableAreaValue?: number | null;
  usableAreaUnit?: string | null;
  commonAreaValue?: number | null;
  commonAreaUnit?: string | null;
  floorLevel?: number | null;
  ceilingHeightM?: number | null;
  hasStorefront?: boolean | null;
  hasSignageRights?: boolean | null;
  zoningClassification?: string | null;
  maxOccupancy?: number | null;
  restroomCount?: number | null;
  hasKitchenFacility?: boolean | null;
  accessibilityCompliant?: boolean | null;
}

export interface IndustrialDetailsRequest {
  clearHeightM?: number | null;
  loadingDocks?: number | null;
  driveInDoors?: number | null;
  floorLoadCapacityKgSqm?: number | null;
  powerCapacityKva?: number | null;
  hasThreePhasePower?: boolean | null;
  hasCrane?: boolean | null;
  craneCapacityTons?: number | null;
  hasHazmatCertification?: boolean | null;
  hasVentilationSystem?: boolean | null;
  hasClimateControl?: boolean | null;
  yardAreaValue?: number | null;
  yardAreaUnit?: string | null;
  zoningClassification?: string | null;
}

export interface AgriculturalDetailsRequest {
  totalLandAreaValue?: number | null;
  totalLandAreaUnit?: string | null;
  arableAreaValue?: number | null;
  arableAreaUnit?: string | null;
  soilType?: string | null;
  hasWaterRights?: boolean | null;
  waterSource?: string | null;
  irrigationType?: string | null;
  fencingType?: string | null;
  hasOutbuildings?: boolean | null;
  outbuildingDetails?: string | null;
  currentUse?: string | null;
  zoningClassification?: string | null;
}

export interface OutdoorAreaRequest {
  type: string;
  areaValue: number | null;
  areaUnit?: string;
}

export interface PropertyAmenityRequest {
  amenityIdentifier: string;
  notes?: string;
}

export interface CreatePropertyRequest {
  propertyCategory: PropertyResponsePropertyCategory;
  propertyType: PropertyResponsePropertyType;
  status: PropertyResponseStatus;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  geocodeAccuracy?: string | null;
  areaValue?: number | null;
  areaUnit?: string | null;
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
  energyEfficiencyRating?: string | null;
  energyCertificateExpiryDate?: string | null;
  heatingType?: string | null;
  coolingType?: string | null;
  hotWaterSystem?: string | null;
  insulationNotes?: string | null;
  electricityConnectionType?: string | null;
  electricityCapacityAmps?: number | null;
  waterConnectionType?: string | null;
  hasGasConnection?: boolean;
  sewageType?: string | null;
  internetConnectionType?: string | null;
  internetMaxSpeedMbps?: number | null;
  internetStatus?: string | null;
  parkingSpaces?: number | null;
  parkingType?: string | null;
  hasSmokeDetectors?: boolean;
  hasCoDetectors?: boolean;
  hasFireExtinguisher?: boolean;
  hasSprinklerSystem?: boolean;
  hasAlarmSystem?: boolean;
  hasSecurityCameras?: boolean;
  hasSecureEntry?: boolean;
  safetyNotes?: string | null;
  isWheelchairAccessible?: boolean;
  hasElevator?: boolean;
  hasStepFreeEntrance?: boolean;
  hasAdaptedBathroom?: boolean;
  accessibilityNotes?: string | null;
  residentialDetails?: ResidentialDetailsRequest | null;
  commercialDetails?: CommercialDetailsRequest | null;
  industrialDetails?: IndustrialDetailsRequest | null;
  agriculturalDetails?: AgriculturalDetailsRequest | null;
}

export interface UpdatePropertyRequest {
  propertyType: PropertyResponsePropertyType;
  status: PropertyResponseStatus;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  geocodeAccuracy?: string | null;
  areaValue?: number | null;
  areaUnit?: string | null;
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
  energyEfficiencyRating?: string | null;
  energyCertificateExpiryDate?: string | null;
  heatingType?: string | null;
  coolingType?: string | null;
  hotWaterSystem?: string | null;
  insulationNotes?: string | null;
  electricityConnectionType?: string | null;
  electricityCapacityAmps?: number | null;
  waterConnectionType?: string | null;
  hasGasConnection?: boolean;
  sewageType?: string | null;
  internetConnectionType?: string | null;
  internetMaxSpeedMbps?: number | null;
  internetStatus?: string | null;
  parkingSpaces?: number | null;
  parkingType?: string | null;
  hasSmokeDetectors?: boolean;
  hasCoDetectors?: boolean;
  hasFireExtinguisher?: boolean;
  hasSprinklerSystem?: boolean;
  hasAlarmSystem?: boolean;
  hasSecurityCameras?: boolean;
  hasSecureEntry?: boolean;
  safetyNotes?: string | null;
  isWheelchairAccessible?: boolean;
  hasElevator?: boolean;
  hasStepFreeEntrance?: boolean;
  hasAdaptedBathroom?: boolean;
  accessibilityNotes?: string | null;
  residentialDetails?: ResidentialDetailsRequest | null;
  commercialDetails?: CommercialDetailsRequest | null;
  industrialDetails?: IndustrialDetailsRequest | null;
  agriculturalDetails?: AgriculturalDetailsRequest | null;
}

// --- Property Dashboard ---

export interface PropertyDashboardResponse {
  summary: DashboardSummaryMetrics;
  cashFlow: CashFlowChartData;
  equity: EquityChartData;
  expenseBreakdown: ExpenseBreakdownChartData;
  occupancy: OccupancyChartData;
  dataCompleteness: DashboardDataCompleteness;
  futureTrend: FutureTrendData;
}

export interface FutureTrendData {
  months: FutureMonthDataPoint[];
}

export interface FutureMonthDataPoint {
  month: string;
  expectedIncome: number;
  expectedExpenses: number;
  expectedNet: number;
}

export interface DashboardSummaryMetrics {
  totalRoiPercent: number | null;
  annualizedRoiPercent: number | null;
  capRatePercent: number | null;
  cashOnCashPercent: number | null;
  monthlyCashFlow: number | null;
  annualNoi: number | null;
  totalEquity: number | null;
  equityGrowthPercent: number | null;
  occupancyRatePercent: number | null;
  grossRentMultiplier: number | null;
  currency: string | null;
}

export interface CashFlowChartData {
  months: MonthlyDataPoint[];
}

export interface MonthlyDataPoint {
  month: string;
  income: number;
  expenses: number;
  mortgage: number;
  net: number;
}

export interface EquityChartData {
  purchasePrice: number | null;
  currentMarketValue: number | null;
  mortgageBalance: number | null;
}

export interface ExpenseBreakdownChartData {
  categories: CategorySlice[];
  timeline: ExpenseTimelineMonth[];
}

export interface ExpenseTimelineMonth {
  month: string;
  categoryAmounts: Record<string, number>;
}

export interface CategorySlice {
  category: string;
  amount: number;
}

export interface OccupancyChartData {
  months: OccupancyDataPoint[];
}

export interface OccupancyDataPoint {
  month: string;
  tenantOccupancyPercent: number;
  selfOccupancyPercent: number;
}

export interface DashboardDataCompleteness {
  hasPurchasePrice: boolean;
  hasMarketValue: boolean;
  hasMortgageInfo: boolean;
  hasOperatingCosts: boolean;
  hasContracts: boolean;
  hasPayments: boolean;
  hasExpenses: boolean;
  completenessPercent: number;
}

// --- Category → Sub-type mapping ---

export const PROPERTY_TYPES_BY_CATEGORY: Record<
  PropertyResponsePropertyCategory,
  PropertyResponsePropertyType[]
> = {
  [PropertyResponsePropertyCategory.RESIDENTIAL]: [
    PropertyResponsePropertyType.APARTMENT,
    PropertyResponsePropertyType.HOUSE,
    PropertyResponsePropertyType.STUDIO,
    PropertyResponsePropertyType.ROOM,
    PropertyResponsePropertyType.VILLA,
    PropertyResponsePropertyType.TOWNHOUSE,
    PropertyResponsePropertyType.OTHER_RESIDENTIAL,
  ],
  [PropertyResponsePropertyCategory.COMMERCIAL]: [
    PropertyResponsePropertyType.OFFICE,
    PropertyResponsePropertyType.RETAIL,
    PropertyResponsePropertyType.RESTAURANT,
    PropertyResponsePropertyType.HOTEL,
    PropertyResponsePropertyType.SHOWROOM,
    PropertyResponsePropertyType.AUTO_DEALERSHIP,
    PropertyResponsePropertyType.SNACKBAR,
    PropertyResponsePropertyType.CAFE,
    PropertyResponsePropertyType.MOTEL,
    PropertyResponsePropertyType.BAR,
    PropertyResponsePropertyType.BED_AND_BREAKFAST,
    PropertyResponsePropertyType.OTHER_COMMERCIAL,
  ],
  [PropertyResponsePropertyCategory.INDUSTRIAL]: [
    PropertyResponsePropertyType.WAREHOUSE,
    PropertyResponsePropertyType.WORKSHOP,
    PropertyResponsePropertyType.FACTORY,
    PropertyResponsePropertyType.DATA_CENTER,
    PropertyResponsePropertyType.COLD_STORAGE,
    PropertyResponsePropertyType.GARAGE,
    PropertyResponsePropertyType.OTHER_INDUSTRIAL,
  ],
  [PropertyResponsePropertyCategory.AGRICULTURAL]: [
    PropertyResponsePropertyType.FARMLAND,
    PropertyResponsePropertyType.RANCH,
    PropertyResponsePropertyType.GREENHOUSE,
    PropertyResponsePropertyType.ORCHARD,
    PropertyResponsePropertyType.VINEYARD,
    PropertyResponsePropertyType.OTHER_AGRICULTURAL,
  ],
  [PropertyResponsePropertyCategory.MIXED_USE]: [
    PropertyResponsePropertyType.MIXED_USE,
  ],
};

// --- Display labels ---

export const PROPERTY_CATEGORY_LABELS: Record<
  PropertyResponsePropertyCategory,
  string
> = {
  [PropertyResponsePropertyCategory.RESIDENTIAL]: 'Residential',
  [PropertyResponsePropertyCategory.COMMERCIAL]: 'Commercial',
  [PropertyResponsePropertyCategory.INDUSTRIAL]: 'Industrial',
  [PropertyResponsePropertyCategory.AGRICULTURAL]: 'Agricultural',
  [PropertyResponsePropertyCategory.MIXED_USE]: 'Mixed-Use',
};

export const PROPERTY_TYPE_LABELS: Record<string, string> = {
  APARTMENT: 'Apartment',
  HOUSE: 'House',
  STUDIO: 'Studio',
  ROOM: 'Room',
  VILLA: 'Villa',
  TOWNHOUSE: 'Townhouse',
  OTHER_RESIDENTIAL: 'Other',
  OFFICE: 'Office',
  RETAIL: 'Retail',
  RESTAURANT: 'Restaurant',
  HOTEL: 'Hotel',
  SHOWROOM: 'Showroom',
  AUTO_DEALERSHIP: 'Auto Dealership',
  SNACKBAR: 'Snackbar',
  CAFE: 'Caf\u00e9',
  MOTEL: 'Motel',
  BAR: 'Bar',
  BED_AND_BREAKFAST: 'B&B',
  OTHER_COMMERCIAL: 'Other',
  WAREHOUSE: 'Warehouse',
  WORKSHOP: 'Workshop',
  FACTORY: 'Factory',
  DATA_CENTER: 'Data Center',
  COLD_STORAGE: 'Cold Storage',
  GARAGE: 'Garage',
  OTHER_INDUSTRIAL: 'Other',
  FARMLAND: 'Farmland',
  RANCH: 'Ranch',
  GREENHOUSE: 'Greenhouse',
  ORCHARD: 'Orchard',
  VINEYARD: 'Vineyard',
  OTHER_AGRICULTURAL: 'Other',
  MIXED_USE: 'Mixed-Use',
  COMMERCIAL: 'Commercial',
};

export const PROPERTY_STATUS_LABELS: Record<PropertyResponseStatus, string> = {
  [PropertyResponseStatus.VACANT]: 'Vacant',
  [PropertyResponseStatus.OCCUPIED]: 'Occupied',
  [PropertyResponseStatus.SELF_OCCUPIED]: 'Self-Occupied',
  [PropertyResponseStatus.MAINTENANCE]: 'Maintenance',
  [PropertyResponseStatus.UNAVAILABLE]: 'Unavailable',
  [PropertyResponseStatus.UNDER_RENOVATION]: 'Under Renovation',
  [PropertyResponseStatus.FALLOW]: 'Fallow',
  [PropertyResponseStatus.LISTED]: 'Listed',
};

// --- Dropdown option constants ---

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
  'PARKING_LOT',
  'COURTYARD',
  'LOADING_AREA',
  'STORAGE_AREA',
  'PASTURE',
  'PADDOCK',
  'ORCHARD_AREA',
  'GREENHOUSE_AREA',
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
