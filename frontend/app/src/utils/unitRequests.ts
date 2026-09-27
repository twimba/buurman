import type { Unit, UpdateUnitRequest } from '@/types/unit';

/**
 * Every key `UpdateUnitRequest` can carry, kept as a real value (not just a
 * type) so a test can diff `Object.keys(unitToUpdateRequest(fixture))`
 * against it. If `UpdateUnitRequest` grows a field, this object literal fails
 * to typecheck until a line is added for it -- `tsc --noEmit` catches the gap
 * before any test runs.
 */
export const UPDATE_UNIT_REQUEST_KEYS: Record<keyof UpdateUnitRequest, true> =
  {
    unitNumber: true,
    name: true,
    floor: true,
    unitType: true,
    status: true,
    areaValue: true,
    areaUnit: true,
    wozValue: true,
    wozValueCurrency: true,
    energyEfficiencyRating: true,
    energyCertificateExpiryDate: true,
    heatingType: true,
    coolingType: true,
    hotWaterSystem: true,
    insulationNotes: true,
    flooringType: true,
    windowType: true,
    hasSmokeDetectors: true,
    hasCoDetectors: true,
    hasFireExtinguisher: true,
    hasAdaptedBathroom: true,
    accessibilityNotes: true,
  };

/**
 * The unit PUT endpoint replaces the whole record: any field left out of the
 * request body is cleared server-side (PREAMBLE amendment A4). This is the
 * ONLY place an `UpdateUnitRequest` may be constructed -- every host (the
 * Info tab's inline form, the unit detail page, and any later task that edits
 * a unit) must seed its draft from this function and layer edits on top of
 * the complete object it returns. Never build an `UpdateUnitRequest` by hand
 * from a subset of fields.
 */
export const unitToUpdateRequest = (unit: Unit): UpdateUnitRequest => ({
  unitNumber: unit.unitNumber,
  name: unit.name,
  floor: unit.floor,
  unitType: unit.unitType,
  status: unit.status,
  areaValue: unit.areaValue,
  areaUnit: unit.areaUnit,
  wozValue: unit.wozValue,
  wozValueCurrency: unit.wozValueCurrency,
  energyEfficiencyRating: unit.energyEfficiencyRating,
  energyCertificateExpiryDate: unit.energyCertificateExpiryDate,
  heatingType: unit.heatingType,
  coolingType: unit.coolingType,
  hotWaterSystem: unit.hotWaterSystem,
  insulationNotes: unit.insulationNotes,
  flooringType: unit.flooringType,
  windowType: unit.windowType,
  hasSmokeDetectors: unit.hasSmokeDetectors,
  hasCoDetectors: unit.hasCoDetectors,
  hasFireExtinguisher: unit.hasFireExtinguisher,
  hasAdaptedBathroom: unit.hasAdaptedBathroom,
  accessibilityNotes: unit.accessibilityNotes,
});
