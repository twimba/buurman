// Thin re-exports of generated models + frontend-only stable names.
//
// UnitIdentifier is a branded string (see orval.config.ts) so that a PropertyIdentifier can never
// be passed where a unit is expected. PropertyIdentifier is a plain string alias.

export {
  UnitType,
  type UnitType as UnitTypeValue,
  UnitStatus,
  type UnitStatus as UnitStatusValue,
  BulkCreateUnitsRequestNumberingPattern,
  type BulkCreateUnitsRequestNumberingPattern as NumberingPattern,
} from '../generated/models';

export type {
  UnitIdentifier,
  PropertyIdentifier,
  UnitResponse,
  UnitResponse as Unit,
  UnitGridRowResponse,
  UnitGridRowResponse as UnitGridRow,
  UnitSummaryResponse,
  UnitShareEntry,
  CreateUnitRequest,
  UpdateUnitRequest,
  BulkCreateUnitsRequest,
  UnitResidentialDetailsResponse,
  UpdateUnitResidentialDetailsRequest,
  AmenityResponse,
  AmenityIdentifier,
  UpdateUnitAmenitiesRequest,
} from '../generated/models';
