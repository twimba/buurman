import { describe, expect, it } from 'vitest';
import { unitToUpdateRequest, UPDATE_UNIT_REQUEST_KEYS } from '../unitRequests';
import { UnitType, UnitStatus } from '@/types/unit';
import type { Unit } from '@/types/unit';

const fullUnit: Unit = {
  identifier: 'unt_01TEST',
  propertyIdentifier: 'prp_01TEST',
  unitNumber: '1A',
  name: 'Front flat',
  floor: 1,
  unitType: UnitType.APARTMENT,
  status: UnitStatus.OCCUPIED,
  implicit: false,
  sortOrder: 0,
  areaValue: 65.5,
  areaUnit: 'sqm',
  wozValue: 250000,
  wozValueCurrency: 'EUR',
  wozSharePct: 50,
  allocationShare: 0.5,
  energyEfficiencyRating: 'B',
  energyCertificateExpiryDate: '2030-01-01',
  heatingType: 'CENTRAL',
  coolingType: 'NONE',
  hotWaterSystem: 'BOILER',
  insulationNotes: 'Double glazing throughout',
  flooringType: 'HARDWOOD',
  windowType: 'DOUBLE_PANE',
  hasSmokeDetectors: true,
  hasCoDetectors: true,
  hasFireExtinguisher: false,
  hasAdaptedBathroom: false,
  accessibilityNotes: 'Step-free access',
  createdAt: '2024-01-01T00:00:00Z',
};

describe('unitToUpdateRequest', () => {
  it('carries exactly the keys UpdateUnitRequest declares -- never fewer', () => {
    const result = unitToUpdateRequest(fullUnit);
    expect(Object.keys(result).sort()).toEqual(
      Object.keys(UPDATE_UNIT_REQUEST_KEYS).sort()
    );
  });

  it('maps every field to its matching value, never dropping or renaming one', () => {
    expect(unitToUpdateRequest(fullUnit)).toEqual({
      unitNumber: '1A',
      name: 'Front flat',
      floor: 1,
      unitType: UnitType.APARTMENT,
      status: UnitStatus.OCCUPIED,
      areaValue: 65.5,
      areaUnit: 'sqm',
      wozValue: 250000,
      wozValueCurrency: 'EUR',
      energyEfficiencyRating: 'B',
      energyCertificateExpiryDate: '2030-01-01',
      heatingType: 'CENTRAL',
      coolingType: 'NONE',
      hotWaterSystem: 'BOILER',
      insulationNotes: 'Double glazing throughout',
      flooringType: 'HARDWOOD',
      windowType: 'DOUBLE_PANE',
      hasSmokeDetectors: true,
      hasCoDetectors: true,
      hasFireExtinguisher: false,
      hasAdaptedBathroom: false,
      accessibilityNotes: 'Step-free access',
    });
  });

  it('never emits wozSharePct/allocationShare -- the backend request has no writer for either', () => {
    const result = unitToUpdateRequest(fullUnit);
    expect(result).not.toHaveProperty('wozSharePct');
    expect(result).not.toHaveProperty('allocationShare');
  });

  it('preserves undefined optional fields rather than inventing a value', () => {
    const sparseUnit: Unit = {
      ...fullUnit,
      name: undefined,
      areaValue: undefined,
      insulationNotes: undefined,
    };
    const result = unitToUpdateRequest(sparseUnit);
    expect(result.name).toBeUndefined();
    expect(result.areaValue).toBeUndefined();
    expect(result.insulationNotes).toBeUndefined();
  });
});
