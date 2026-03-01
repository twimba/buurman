// Enums — re-exported from generated
export {
  OccupancyPeriodResponseType as OccupancyType,
  type OccupancyPeriodResponseType,
  OccupancyPeriodResponseEndReason as OccupancyEndReason,
  type OccupancyPeriodResponseEndReason,
} from '../generated/models';

// Interfaces — kept manual (generated adds | null to optional fields)

import {
  OccupancyPeriodResponseType,
  OccupancyPeriodResponseEndReason,
} from '../generated/models';

// Request interfaces — manual (generated adds | null to all optional fields)

export interface CreateOccupancyPeriodRequest {
  startDate: string;
  type: OccupancyPeriodResponseType;
  endDate?: string;
  occupantName?: string;
  monthlyImputedRent?: number;
  notes?: string;
}

export interface UpdateOccupancyPeriodRequest {
  startDate?: string;
  type?: OccupancyPeriodResponseType;
  endDate?: string;
  occupantName?: string;
  monthlyImputedRent?: number;
  notes?: string;
}

export interface EndOccupancyPeriodRequest {
  endDate: string;
  endReason?: OccupancyPeriodResponseEndReason;
  notes?: string;
}

export interface OccupancyPeriodResponse {
  identifier: string;
  propertyIdentifier: string;
  startDate: string;
  endDate?: string | null;
  type: OccupancyPeriodResponseType;
  occupantName?: string | null;
  monthlyImputedRent?: number | null;
  endReason?: OccupancyPeriodResponseEndReason | null;
  notes?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface TimelineEntry {
  type: 'SELF_OCCUPANCY' | 'CONTRACT' | 'VACANCY';
  identifier: string;
  startDate: string;
  endDate?: string | null;
  description?: string | null;
  metadata?: string | null;
}

export interface FinancingTimelineEntry {
  identifier: string;
  startDate: string;
  endDate?: string | null;
  financingType: string;
  status: string;
  lenderName?: string | null;
  originalAmount: number;
  originalAmountCurrency: string;
  interestRate?: number | null;
}

export interface PropertyTimelineResponse {
  acquisitionDate?: string | null;
  entries: TimelineEntry[];
  financings: FinancingTimelineEntry[];
}

export const OCCUPANCY_TYPE_LABELS: Record<
  OccupancyPeriodResponseType,
  string
> = {
  [OccupancyPeriodResponseType.PERSONAL]: 'Personal',
  [OccupancyPeriodResponseType.FAMILY]: 'Family',
  [OccupancyPeriodResponseType.BUSINESS]: 'Business',
};

export const OCCUPANCY_END_REASON_LABELS: Record<OccupancyPeriodResponseEndReason, string> = {
  [OccupancyPeriodResponseEndReason.CONVERTING_TO_RENTAL]: 'Converting to Rental',
  [OccupancyPeriodResponseEndReason.SELLING]: 'Selling',
  [OccupancyPeriodResponseEndReason.RENOVATION]: 'Renovation',
  [OccupancyPeriodResponseEndReason.OTHER]: 'Other',
};
