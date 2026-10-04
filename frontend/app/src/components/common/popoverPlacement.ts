export type PopoverSide = 'bottom' | 'top';

export interface Box {
  top: number;
  bottom: number;
  left: number;
  right: number;
}

export interface PlacementInput {
  /** Trigger rect in viewport coordinates (getBoundingClientRect). */
  trigger: Box;
  /** Natural (unclamped) popover size. */
  popover: { width: number; height: number };
  viewport: { width: number; height: number };
  preferred?: PopoverSide;
  /** Which trigger edge the popover lines up with before horizontal clamping. */
  align?: 'left' | 'right';
  /** Distance between trigger and popover. */
  gap?: number;
  /** Minimum distance kept to the viewport edges. */
  margin?: number;
}

export interface Placement {
  side: PopoverSide;
  /** Viewport (position: fixed) coordinates of the popover's top-left corner. */
  top: number;
  left: number;
  maxHeight: number;
}

/**
 * Picks the side with room (preferred first, flipping when it does not fit), clamps the height to
 * the space available on that side and keeps the popover horizontally inside the viewport.
 */
export const computePopoverPlacement = ({
  trigger,
  popover,
  viewport,
  preferred = 'bottom',
  align = 'right',
  gap = 8,
  margin = 8,
}: PlacementInput): Placement => {
  const room: Record<PopoverSide, number> = {
    bottom: Math.max(0, viewport.height - trigger.bottom - gap - margin),
    top: Math.max(0, trigger.top - gap - margin),
  };
  const other: PopoverSide = preferred === 'bottom' ? 'top' : 'bottom';

  let side: PopoverSide;
  if (popover.height <= room[preferred]) {
    side = preferred;
  } else if (popover.height <= room[other]) {
    side = other;
  } else {
    side = room[other] > room[preferred] ? other : preferred;
  }

  const maxHeight = room[side];
  const height = Math.min(popover.height, maxHeight);
  const top =
    side === 'bottom' ? trigger.bottom + gap : trigger.top - gap - height;

  const desiredLeft =
    align === 'right' ? trigger.right - popover.width : trigger.left;
  const maxLeft = viewport.width - popover.width - margin;
  const left = Math.max(margin, Math.min(desiredLeft, maxLeft));

  return { side, top, left, maxHeight };
};
