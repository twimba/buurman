export enum PropertyCategory {
  RESIDENTIAL = 'RESIDENTIAL',
  COMMERCIAL = 'COMMERCIAL',
  INDUSTRIAL = 'INDUSTRIAL',
  AGRICULTURAL = 'AGRICULTURAL',
  MIXED_USE = 'MIXED_USE',
}

export enum PropertyType {
  // Residential
  APARTMENT = 'APARTMENT',
  HOUSE = 'HOUSE',
  STUDIO = 'STUDIO',
  ROOM = 'ROOM',
  VILLA = 'VILLA',
  TOWNHOUSE = 'TOWNHOUSE',
  OTHER_RESIDENTIAL = 'OTHER_RESIDENTIAL',
  // Commercial
  OFFICE = 'OFFICE',
  RETAIL = 'RETAIL',
  RESTAURANT = 'RESTAURANT',
  HOTEL = 'HOTEL',
  SHOWROOM = 'SHOWROOM',
  AUTO_DEALERSHIP = 'AUTO_DEALERSHIP',
  SNACKBAR = 'SNACKBAR',
  CAFE = 'CAFE',
  MOTEL = 'MOTEL',
  BAR = 'BAR',
  BED_AND_BREAKFAST = 'BED_AND_BREAKFAST',
  OTHER_COMMERCIAL = 'OTHER_COMMERCIAL',
  // Industrial
  WAREHOUSE = 'WAREHOUSE',
  WORKSHOP = 'WORKSHOP',
  FACTORY = 'FACTORY',
  DATA_CENTER = 'DATA_CENTER',
  COLD_STORAGE = 'COLD_STORAGE',
  GARAGE = 'GARAGE',
  OTHER_INDUSTRIAL = 'OTHER_INDUSTRIAL',
  // Agricultural
  FARMLAND = 'FARMLAND',
  RANCH = 'RANCH',
  GREENHOUSE = 'GREENHOUSE',
  ORCHARD = 'ORCHARD',
  VINEYARD = 'VINEYARD',
  OTHER_AGRICULTURAL = 'OTHER_AGRICULTURAL',
  // Mixed-Use
  MIXED_USE = 'MIXED_USE',
  // Legacy
  COMMERCIAL = 'COMMERCIAL',
}

export enum PropertyStatus {
  VACANT = 'VACANT',
  OCCUPIED = 'OCCUPIED',
  MAINTENANCE = 'MAINTENANCE',
  UNAVAILABLE = 'UNAVAILABLE',
  UNDER_RENOVATION = 'UNDER_RENOVATION',
  FALLOW = 'FALLOW',
  LISTED = 'LISTED',
}

// --- Category → Sub-type mapping ---

export const PROPERTY_TYPES_BY_CATEGORY: Record<
  PropertyCategory,
  PropertyType[]
> = {
  [PropertyCategory.RESIDENTIAL]: [
    PropertyType.APARTMENT,
    PropertyType.HOUSE,
    PropertyType.STUDIO,
    PropertyType.ROOM,
    PropertyType.VILLA,
    PropertyType.TOWNHOUSE,
    PropertyType.OTHER_RESIDENTIAL,
  ],
  [PropertyCategory.COMMERCIAL]: [
    PropertyType.OFFICE,
    PropertyType.RETAIL,
    PropertyType.RESTAURANT,
    PropertyType.HOTEL,
    PropertyType.SHOWROOM,
    PropertyType.AUTO_DEALERSHIP,
    PropertyType.SNACKBAR,
    PropertyType.CAFE,
    PropertyType.MOTEL,
    PropertyType.BAR,
    PropertyType.BED_AND_BREAKFAST,
    PropertyType.OTHER_COMMERCIAL,
  ],
  [PropertyCategory.INDUSTRIAL]: [
    PropertyType.WAREHOUSE,
    PropertyType.WORKSHOP,
    PropertyType.FACTORY,
    PropertyType.DATA_CENTER,
    PropertyType.COLD_STORAGE,
    PropertyType.GARAGE,
    PropertyType.OTHER_INDUSTRIAL,
  ],
  [PropertyCategory.AGRICULTURAL]: [
    PropertyType.FARMLAND,
    PropertyType.RANCH,
    PropertyType.GREENHOUSE,
    PropertyType.ORCHARD,
    PropertyType.VINEYARD,
    PropertyType.OTHER_AGRICULTURAL,
  ],
  [PropertyCategory.MIXED_USE]: [PropertyType.MIXED_USE],
};

// --- Display labels ---

export const PROPERTY_CATEGORY_LABELS: Record<PropertyCategory, string> = {
  [PropertyCategory.RESIDENTIAL]: 'Residential',
  [PropertyCategory.COMMERCIAL]: 'Commercial',
  [PropertyCategory.INDUSTRIAL]: 'Industrial',
  [PropertyCategory.AGRICULTURAL]: 'Agricultural',
  [PropertyCategory.MIXED_USE]: 'Mixed-Use',
};

export const PROPERTY_TYPE_LABELS: Record<string, string> = {
  // Residential
  APARTMENT: 'Apartment',
  HOUSE: 'House',
  STUDIO: 'Studio',
  ROOM: 'Room',
  VILLA: 'Villa',
  TOWNHOUSE: 'Townhouse',
  OTHER_RESIDENTIAL: 'Other',
  // Commercial
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
  // Industrial
  WAREHOUSE: 'Warehouse',
  WORKSHOP: 'Workshop',
  FACTORY: 'Factory',
  DATA_CENTER: 'Data Center',
  COLD_STORAGE: 'Cold Storage',
  GARAGE: 'Garage',
  OTHER_INDUSTRIAL: 'Other',
  // Agricultural
  FARMLAND: 'Farmland',
  RANCH: 'Ranch',
  GREENHOUSE: 'Greenhouse',
  ORCHARD: 'Orchard',
  VINEYARD: 'Vineyard',
  OTHER_AGRICULTURAL: 'Other',
  // Mixed-Use / Legacy
  MIXED_USE: 'Mixed-Use',
  COMMERCIAL: 'Commercial',
};

export const PROPERTY_STATUS_LABELS: Record<PropertyStatus, string> = {
  [PropertyStatus.VACANT]: 'Vacant',
  [PropertyStatus.OCCUPIED]: 'Occupied',
  [PropertyStatus.MAINTENANCE]: 'Maintenance',
  [PropertyStatus.UNAVAILABLE]: 'Unavailable',
  [PropertyStatus.UNDER_RENOVATION]: 'Under Renovation',
  [PropertyStatus.FALLOW]: 'Fallow',
  [PropertyStatus.LISTED]: 'Listed',
};

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
  // Residential
  'BALCONY',
  'TERRACE',
  'GARDEN',
  'ROOFTOP',
  'PATIO',
  'YARD',
  'OTHER',
  // Commercial / Industrial
  'PARKING_LOT',
  'COURTYARD',
  'LOADING_AREA',
  'STORAGE_AREA',
  // Agricultural
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

// --- Investment & Financial enums ---

export enum MortgageType {
  FIXED_RATE = 'FIXED_RATE',
  VARIABLE_RATE = 'VARIABLE_RATE',
  INTEREST_ONLY = 'INTEREST_ONLY',
  NONE = 'NONE',
}

export enum DepreciationMethod {
  STRAIGHT_LINE = 'STRAIGHT_LINE',
  DECLINING_BALANCE = 'DECLINING_BALANCE',
  NONE = 'NONE',
}

export const MORTGAGE_TYPE_LABELS: Record<MortgageType, string> = {
  [MortgageType.FIXED_RATE]: 'Fixed Rate',
  [MortgageType.VARIABLE_RATE]: 'Variable Rate',
  [MortgageType.INTEREST_ONLY]: 'Interest Only',
  [MortgageType.NONE]: 'None',
};

export const DEPRECIATION_METHOD_LABELS: Record<DepreciationMethod, string> = {
  [DepreciationMethod.STRAIGHT_LINE]: 'Straight Line',
  [DepreciationMethod.DECLINING_BALANCE]: 'Declining Balance',
  [DepreciationMethod.NONE]: 'None',
};

// --- Category-specific detail interfaces ---

export interface ResidentialDetailsResponse {
  bedrooms: number | null;
  bathrooms: number | null;
  furnished: boolean | null;
  petPolicy: string | null;
}

export interface ResidentialDetailsRequest {
  bedrooms?: number | null;
  bathrooms?: number | null;
  furnished?: boolean | null;
  petPolicy?: string | null;
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
  applicableCategories: string[] | null;
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
  propertyCategory: PropertyCategory;
  propertyType: PropertyType;
  status: PropertyStatus;
}

export interface PropertyResponse {
  identifier: string;
  propertyCategory: PropertyCategory;
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

  // Investment & Financial
  purchasePrice: number | null;
  purchasePriceCurrency: string | null;
  purchaseDate: string | null;
  currentMarketValue: number | null;
  currentMarketValueCurrency: string | null;
  marketValueDate: string | null;
  mortgageType: MortgageType | null;
  mortgageAmount: number | null;
  mortgageAmountCurrency: string | null;
  mortgageInterestRate: number | null;
  mortgageStartDate: string | null;
  mortgageEndDate: string | null;
  monthlyMortgagePayment: number | null;
  monthlyMortgagePaymentCurrency: string | null;
  annualPropertyTax: number | null;
  annualPropertyTaxCurrency: string | null;
  annualInsurance: number | null;
  annualInsuranceCurrency: string | null;
  annualHoaFee: number | null;
  annualHoaFeeCurrency: string | null;
  annualManagementFee: number | null;
  annualManagementFeeCurrency: string | null;
  annualMaintenanceReserve: number | null;
  annualMaintenanceReserveCurrency: string | null;
  annualPropertyTaxDueMonth: number | null;
  annualInsuranceDueMonth: number | null;
  annualHoaFeeDueMonth: number | null;
  annualManagementFeeDueMonth: number | null;
  annualMaintenanceReserveDueMonth: number | null;
  depreciationMethod: DepreciationMethod | null;
  depreciationYears: number | null;
  landValue: number | null;
  landValueCurrency: string | null;

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

export interface CreatePropertyRequest {
  propertyCategory: PropertyCategory;
  propertyType: PropertyType;
  status: PropertyStatus;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  geocodeAccuracy?: string | null;
  areaValue?: number | null;
  areaUnit?: string | null;

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

  // Investment & Financial
  purchasePrice?: number | null;
  purchasePriceCurrency?: string | null;
  purchaseDate?: string | null;
  currentMarketValue?: number | null;
  currentMarketValueCurrency?: string | null;
  marketValueDate?: string | null;
  mortgageType?: MortgageType | null;
  mortgageAmount?: number | null;
  mortgageAmountCurrency?: string | null;
  mortgageInterestRate?: number | null;
  mortgageStartDate?: string | null;
  mortgageEndDate?: string | null;
  monthlyMortgagePayment?: number | null;
  monthlyMortgagePaymentCurrency?: string | null;
  annualPropertyTax?: number | null;
  annualPropertyTaxCurrency?: string | null;
  annualInsurance?: number | null;
  annualInsuranceCurrency?: string | null;
  annualHoaFee?: number | null;
  annualHoaFeeCurrency?: string | null;
  annualManagementFee?: number | null;
  annualManagementFeeCurrency?: string | null;
  annualMaintenanceReserve?: number | null;
  annualMaintenanceReserveCurrency?: string | null;
  annualPropertyTaxDueMonth?: number | null;
  annualInsuranceDueMonth?: number | null;
  annualHoaFeeDueMonth?: number | null;
  annualManagementFeeDueMonth?: number | null;
  annualMaintenanceReserveDueMonth?: number | null;
  depreciationMethod?: DepreciationMethod | null;
  depreciationYears?: number | null;
  landValue?: number | null;
  landValueCurrency?: string | null;

  // Category-specific details (only matching category should be provided)
  residentialDetails?: ResidentialDetailsRequest | null;
  commercialDetails?: CommercialDetailsRequest | null;
  industrialDetails?: IndustrialDetailsRequest | null;
  agriculturalDetails?: AgriculturalDetailsRequest | null;
}

export interface UpdatePropertyRequest {
  propertyType: PropertyType;
  status: PropertyStatus;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  latitude?: number | null;
  longitude?: number | null;
  geocodeAccuracy?: string | null;
  areaValue?: number | null;
  areaUnit?: string | null;

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

  // Investment & Financial
  purchasePrice?: number | null;
  purchasePriceCurrency?: string | null;
  purchaseDate?: string | null;
  currentMarketValue?: number | null;
  currentMarketValueCurrency?: string | null;
  marketValueDate?: string | null;
  mortgageType?: MortgageType | null;
  mortgageAmount?: number | null;
  mortgageAmountCurrency?: string | null;
  mortgageInterestRate?: number | null;
  mortgageStartDate?: string | null;
  mortgageEndDate?: string | null;
  monthlyMortgagePayment?: number | null;
  monthlyMortgagePaymentCurrency?: string | null;
  annualPropertyTax?: number | null;
  annualPropertyTaxCurrency?: string | null;
  annualInsurance?: number | null;
  annualInsuranceCurrency?: string | null;
  annualHoaFee?: number | null;
  annualHoaFeeCurrency?: string | null;
  annualManagementFee?: number | null;
  annualManagementFeeCurrency?: string | null;
  annualMaintenanceReserve?: number | null;
  annualMaintenanceReserveCurrency?: string | null;
  annualPropertyTaxDueMonth?: number | null;
  annualInsuranceDueMonth?: number | null;
  annualHoaFeeDueMonth?: number | null;
  annualManagementFeeDueMonth?: number | null;
  annualMaintenanceReserveDueMonth?: number | null;
  depreciationMethod?: DepreciationMethod | null;
  depreciationYears?: number | null;
  landValue?: number | null;
  landValueCurrency?: string | null;

  // Category-specific details (only matching category should be provided)
  // Note: propertyCategory is NOT here — it's immutable after creation
  residentialDetails?: ResidentialDetailsRequest | null;
  commercialDetails?: CommercialDetailsRequest | null;
  industrialDetails?: IndustrialDetailsRequest | null;
  agriculturalDetails?: AgriculturalDetailsRequest | null;
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

// --- Property Dashboard ---

export interface PropertyDashboardResponse {
  summary: DashboardSummaryMetrics;
  cashFlow: CashFlowChartData;
  equity: EquityChartData;
  expenseBreakdown: ExpenseBreakdownChartData;
  occupancy: OccupancyChartData;
  dataCompleteness: DashboardDataCompleteness;
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
  occupancyPercent: number;
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
