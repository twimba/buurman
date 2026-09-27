// --- API model types: re-exported from generated (source of truth) ---

// PropertyResponse.status was dropped (BUUR-106): a property can hold several
// independently-let units, so there is no single well-defined status at the property level
// anymore. `PropertyStatus` lives on here as an alias of the per-unit `UnitStatus` — same
// values — for call sites that still display a single derived status for one unit/row.
export {
  PropertyResponsePropertyCategory as PropertyCategory,
  type PropertyResponsePropertyCategory,
  PropertyResponsePropertyType as PropertyType,
  type PropertyResponsePropertyType,
  UnitStatus as PropertyStatus,
} from '../generated/models';

export type {
  PropertyResponse,
  PropertySummary,
  CreatePropertyRequest,
  UpdatePropertyRequest,
  DocumentResponse,
  PhotoResponse,
  AmenityResponse,
  PropertyAmenityResponse,
  PropertyAmenityRequest,
  ResidentialDetailsResponse,
  CommercialDetailsResponse,
  IndustrialDetailsResponse,
  AgriculturalDetailsResponse,
  ResidentialDetailsRequest,
  CommercialDetailsRequest,
  IndustrialDetailsRequest,
  AgriculturalDetailsRequest,
  PropertyDashboardResponse,
  FutureTrendData,
  FutureMonthDataPoint,
  CashFlowChartData,
  MonthlyDataPoint,
  EquityChartData,
  ExpenseBreakdownChartData,
  ExpenseTimelineMonth,
  CategorySlice,
  OccupancyChartData,
  OccupancyDataPoint,
  PropertyOutdoorAreaResponse as OutdoorAreaResponse,
  PropertyOutdoorAreaRequest as OutdoorAreaRequest,
  SummaryMetrics as DashboardSummaryMetrics,
  DataCompleteness as DashboardDataCompleteness,
} from '../generated/models';

import {
  PropertyResponsePropertyCategory,
  PropertyResponsePropertyType,
} from '../generated/models';

// --- Frontend-only types (not in the OpenAPI spec) ---

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
  impersonatedBy?: string;
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
  'A++++',
  'A+++',
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
