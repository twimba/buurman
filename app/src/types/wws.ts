export interface WwsCalculationRequest {
  systemVersion: string;
  propertyIdentifier: string;
  contractIdentifier?: string;
  surfaceAreaSqm?: number;
  numberOfRooms?: number;
  numberOfHeatedRooms?: number;
  energyLabel?: string;
  kitchenQualityPoints?: number;
  bathroomQualityPoints?: number;
  wozValue?: number;
  outdoorSpaceSqm?: number;
  parkingType?: string;
  parkingSpaces?: number;
  locationBonus?: number;
  renovationInvestment?: number;
  accessibilityFeatures?: number;
  commonAreaSqm?: number;
}

export interface WwsCalculationResponse {
  identifier?: string;
  totalPoints: number;
  sectorClassification: 'REGULATED' | 'MID_SEGMENT' | 'FREE_SECTOR';
  maxRentIndication?: number;
  systemVersion: string;
  calculationDate: string;
  breakdown: WwsCategoryBreakdown[];
}

export interface WwsCategoryBreakdown {
  key: string;
  name: string;
  nameNl: string;
  points: number;
  explanation: string;
}

export interface WwsPreFillResponse {
  surfaceAreaSqm?: number;
  numberOfRooms?: number;
  numberOfHeatedRooms?: number;
  energyLabel?: string;
  outdoorSpaceSqm?: number;
  parkingType?: string;
  parkingSpaces?: number;
  accessibilityFeatures?: number;
  propertyAddress?: string;
}
