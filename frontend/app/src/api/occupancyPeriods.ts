import client from './client';
import {
  OccupancyPeriodResponse,
  CreateOccupancyPeriodRequest,
  UpdateOccupancyPeriodRequest,
  EndOccupancyPeriodRequest,
  PropertyTimelineResponse,
} from '../types/occupancyPeriod';

const basePath = (propertyIdentifier: string) =>
  `/properties/${propertyIdentifier}/occupancy-periods`;

export const getOccupancyPeriods = async (
  propertyIdentifier: string
): Promise<OccupancyPeriodResponse[]> => {
  const response = await client.get(basePath(propertyIdentifier));
  return response.data;
};

export const getOccupancyPeriod = async (
  propertyIdentifier: string,
  periodIdentifier: string
): Promise<OccupancyPeriodResponse> => {
  const response = await client.get(
    `${basePath(propertyIdentifier)}/${periodIdentifier}`
  );
  return response.data;
};

export const createOccupancyPeriod = async (
  propertyIdentifier: string,
  data: CreateOccupancyPeriodRequest
): Promise<OccupancyPeriodResponse> => {
  const response = await client.post(basePath(propertyIdentifier), data);
  return response.data;
};

export const updateOccupancyPeriod = async (
  propertyIdentifier: string,
  periodIdentifier: string,
  data: UpdateOccupancyPeriodRequest
): Promise<OccupancyPeriodResponse> => {
  const response = await client.put(
    `${basePath(propertyIdentifier)}/${periodIdentifier}`,
    data
  );
  return response.data;
};

export const endOccupancyPeriod = async (
  propertyIdentifier: string,
  periodIdentifier: string,
  data: EndOccupancyPeriodRequest
): Promise<OccupancyPeriodResponse> => {
  const response = await client.post(
    `${basePath(propertyIdentifier)}/${periodIdentifier}/end`,
    data
  );
  return response.data;
};

export const deleteOccupancyPeriod = async (
  propertyIdentifier: string,
  periodIdentifier: string
): Promise<void> => {
  await client.delete(`${basePath(propertyIdentifier)}/${periodIdentifier}`);
};

export const getPropertyTimeline = async (
  propertyIdentifier: string
): Promise<PropertyTimelineResponse> => {
  const response = await client.get(`${basePath(propertyIdentifier)}/timeline`);
  return response.data;
};
