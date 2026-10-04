export interface Box {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export interface Size {
  width: number;
  height: number;
}

export interface Placement {
  top: number;
  left: number;
  placement: 'below' | 'above';
  /** Set only when the panel fits on neither side and must scroll internally. */
  maxHeight?: number;
}

const GAP = 4;
const MARGIN = 8;

/** Fixed-position coordinates for a panel anchored to a trigger: flips above, clamps to viewport. */
export const computePlacement = (
  trigger: Box,
  panel: Size,
  viewport: Size,
  align: 'left' | 'right'
): Placement => {
  const roomBelow = viewport.height - trigger.bottom - GAP - MARGIN;
  const roomAbove = trigger.top - GAP - MARGIN;
  const below =
    panel.height <= roomBelow ||
    (panel.height > roomAbove && roomBelow >= roomAbove);

  const rawLeft =
    align === 'right' ? trigger.right - panel.width : trigger.left;
  const left = Math.max(
    MARGIN,
    Math.min(rawLeft, viewport.width - panel.width - MARGIN)
  );

  const room = below ? roomBelow : roomAbove;
  const height = Math.min(panel.height, room);
  const top = below ? trigger.bottom + GAP : trigger.top - GAP - height;
  return {
    top,
    left,
    placement: below ? 'below' : 'above',
    maxHeight: panel.height > room ? room : undefined,
  };
};
