export enum OccupancyType {
  PERSONAL = 'PERSONAL',
  FAMILY = 'FAMILY',
  BUSINESS = 'BUSINESS',
}

export const OCCUPANCY_TYPE_LABELS: Record<OccupancyType, string> = {
  [OccupancyType.PERSONAL]: 'Personal',
  [OccupancyType.FAMILY]: 'Family',
  [OccupancyType.BUSINESS]: 'Business',
};

export enum OccupancyEndReason {
  CONVERTING_TO_RENTAL = 'CONVERTING_TO_RENTAL',
  SELLING = 'SELLING',
  RENOVATION = 'RENOVATION',
  OTHER = 'OTHER',
}

export const OCCUPANCY_END_REASON_LABELS: Record<OccupancyEndReason, string> = {
  [OccupancyEndReason.CONVERTING_TO_RENTAL]: 'Converting to Rental',
  [OccupancyEndReason.SELLING]: 'Selling',
  [OccupancyEndReason.RENOVATION]: 'Renovation',
  [OccupancyEndReason.OTHER]: 'Other',
};

export interface OccupancyPeriodResponse {
  identifier: string;
  propertyIdentifier: string;
  startDate: string;
  endDate?: string | null;
  type: OccupancyType;
  occupantName?: string | null;
  monthlyImputedRent?: number | null;
  endReason?: OccupancyEndReason | null;
  notes?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface CreateOccupancyPeriodRequest {
  startDate: string;
  type: OccupancyType;
  endDate?: string;
  occupantName?: string;
  monthlyImputedRent?: number;
  notes?: string;
}

export interface UpdateOccupancyPeriodRequest {
  startDate?: string;
  type?: OccupancyType;
  endDate?: string;
  occupantName?: string;
  monthlyImputedRent?: number;
  notes?: string;
}

export interface EndOccupancyPeriodRequest {
  endDate: string;
  endReason?: OccupancyEndReason;
  notes?: string;
}

export interface TimelineEntry {
  type: 'SELF_OCCUPANCY' | 'CONTRACT' | 'VACANCY';
  identifier: string;
  startDate: string;
  endDate?: string | null;
  description?: string | null;
  metadata?: string | null;
}

export interface PropertyTimelineResponse {
  entries: TimelineEntry[];
}
