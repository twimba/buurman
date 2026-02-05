export enum CalendarFeedType {
  ALL_PAYMENTS = 'ALL_PAYMENTS',
  CONTRACT = 'CONTRACT',
  PROPERTY_PAYMENTS = 'PROPERTY_PAYMENTS',
  TENANT_PAYMENTS = 'TENANT_PAYMENTS',
}

export interface CalendarFeedResponse {
  identifier: string;
  feedType: CalendarFeedType;
  contractIdentifier?: string;
  propertyIdentifier?: string;
  tenantIdentifier?: string;
  entityLabel?: string;
  enabled: boolean;
  feedUrl: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCalendarFeedRequest {
  feedType: CalendarFeedType;
  contractIdentifier?: string;
  propertyIdentifier?: string;
  tenantIdentifier?: string;
}
