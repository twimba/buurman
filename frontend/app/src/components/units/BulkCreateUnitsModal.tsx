import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ModalWrapper, Button } from '@buurman/ui';
import { useBulkCreateUnits } from '@/hooks/useUnitHooks';
import {
  UnitType,
  BulkCreateUnitsRequestNumberingPattern as NumberingPattern,
} from '@/types/unit';
import type {
  PropertyIdentifier,
  UnitResponse,
  BulkCreateUnitsRequest,
} from '@/types/unit';

interface BulkCreateUnitsModalProps {
  propertyIdentifier: PropertyIdentifier;
  street: string;
  open: boolean;
  onClose: () => void;
  onCreated: (units: UnitResponse[]) => void;
  /**
   * 'split' (default): converting a property's sole implicit unit into several real ones --
   * shows the reassurance that the existing unit keeps its history. 'expand': a property that
   * is already multi-unit is simply getting more units, so that reassurance would be false
   * (nothing is being renumbered) and is not shown.
   */
  mode?: 'split' | 'expand';
}

/**
 * Fires exactly one `bulkCreateUnits` request -- never a client-side loop of single `create`
 * calls. Used both for the delicate 1 -> N split (from the single-unit Info tab) and for
 * adding more units to an already multi-unit property (from the Units tab).
 */
export const BulkCreateUnitsModal = ({
  propertyIdentifier,
  street,
  open,
  onClose,
  onCreated,
  mode = 'split',
}: BulkCreateUnitsModalProps) => {
  const { t } = useTranslation(['units', 'common']);
  const bulkCreate = useBulkCreateUnits(propertyIdentifier);

  const [countInput, setCountInput] = useState('');
  const [numberingPattern, setNumberingPattern] = useState<NumberingPattern>(
    NumberingPattern.NUMERIC
  );
  const [unitType, setUnitType] = useState<UnitType>(UnitType.APARTMENT);
  const [startFloor, setStartFloor] = useState('0');

  // No reset-on-open effect: callers only ever mount this component while `open` is true (see
  // BulkCreateUnitsModal usages), so every open is a fresh mount with fresh `useState` defaults
  // and a fresh `useBulkCreateUnits()` mutation -- there is no stale `isSuccess` to clear.
  useEffect(() => {
    if (bulkCreate.isSuccess && bulkCreate.data) {
      onCreated(bulkCreate.data);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bulkCreate.isSuccess, bulkCreate.data]);

  const count = parseInt(countInput, 10);
  const isValidCount = Number.isInteger(count) && count >= 1 && count <= 200;
  const parsedStartFloor = parseInt(startFloor, 10) || 0;

  const handleSubmit = () => {
    if (!isValidCount) {
      return;
    }
    const payload: BulkCreateUnitsRequest = {
      count,
      numberingPattern,
      unitType,
      ...(numberingPattern === NumberingPattern.FLOOR_DOT_INDEX
        ? { startFloor: parsedStartFloor }
        : {}),
    };
    bulkCreate.mutate(payload);
  };

  return (
    <ModalWrapper
      open={open}
      onClose={onClose}
      title={
        mode === 'split' ? t('split.title', { street }) : t('grid.addUnit')
      }
      size="sm"
      preventClose={bulkCreate.isPending}
      onSubmit={handleSubmit}
      footer={
        <>
          <Button
            type="button"
            variant="ghost"
            onClick={onClose}
            disabled={bulkCreate.isPending}
          >
            {t('common:buttons.cancel')}
          </Button>
          <Button
            type="button"
            variant="primary"
            onClick={handleSubmit}
            disabled={!isValidCount || bulkCreate.isPending}
          >
            {t('split.submit')}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {mode === 'split' && (
          <p className="text-sm text-text-secondary">
            {t('split.keepsHistory')}
          </p>
        )}

        <div>
          <label
            htmlFor="bulk-unit-count"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('split.count')}
          </label>
          <input
            id="bulk-unit-count"
            type="number"
            min={1}
            max={200}
            value={countInput}
            onChange={(e) => setCountInput(e.target.value)}
            disabled={bulkCreate.isPending}
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60"
          />
        </div>

        <div>
          <label
            htmlFor="bulk-numbering-pattern"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('split.numbering')}
          </label>
          <select
            id="bulk-numbering-pattern"
            value={numberingPattern}
            onChange={(e) =>
              setNumberingPattern(e.target.value as NumberingPattern)
            }
            disabled={bulkCreate.isPending}
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60"
          >
            {Object.values(NumberingPattern).map((pattern) => (
              <option key={pattern} value={pattern}>
                {t(`split.numberingPattern.${pattern}`)}
              </option>
            ))}
          </select>
        </div>

        {numberingPattern === NumberingPattern.FLOOR_DOT_INDEX && (
          <div>
            <label
              htmlFor="bulk-start-floor"
              className="block text-sm font-medium text-text-secondary mb-1"
            >
              {t('split.startFloor')}
            </label>
            <input
              id="bulk-start-floor"
              type="number"
              value={startFloor}
              onChange={(e) => setStartFloor(e.target.value)}
              disabled={bulkCreate.isPending}
              className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60"
            />
          </div>
        )}

        <div>
          <label
            htmlFor="bulk-unit-type"
            className="block text-sm font-medium text-text-secondary mb-1"
          >
            {t('split.unitType')}
          </label>
          <select
            id="bulk-unit-type"
            value={unitType}
            onChange={(e) => setUnitType(e.target.value as UnitType)}
            disabled={bulkCreate.isPending}
            className="w-full border border-border-strong rounded px-3 py-2 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-60"
          >
            {Object.values(UnitType).map((type) => (
              <option key={type} value={type}>
                {t(`type.${type}`)}
              </option>
            ))}
          </select>
        </div>
      </div>
    </ModalWrapper>
  );
};
