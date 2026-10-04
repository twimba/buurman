import { describe, expect, it } from 'vitest';
import { computePopoverPlacement } from '../popoverPlacement';

const viewport = { width: 1280, height: 800 };
const box = (top: number, left: number, w = 160, h = 40) => ({
  top,
  bottom: top + h,
  left,
  right: left + w,
});

describe('computePopoverPlacement', () => {
  it('opens below when there is room', () => {
    const p = computePopoverPlacement({
      trigger: box(100, 200),
      popover: { width: 288, height: 300 },
      viewport,
      align: 'left',
    });
    expect(p.side).toBe('bottom');
    expect(p.top).toBe(148);
    expect(p.left).toBe(200);
  });

  it('flips above when the trigger sits at the viewport bottom', () => {
    const p = computePopoverPlacement({
      trigger: box(740, 200),
      popover: { width: 288, height: 400 },
      viewport,
      align: 'left',
    });
    expect(p.side).toBe('top');
    expect(p.top).toBe(740 - 8 - 400);
    expect(p.maxHeight).toBeGreaterThanOrEqual(400);
  });

  it('uses the larger side and clamps max-height when neither fits', () => {
    // room above 300-16=284, below 800-340-16=444
    const p = computePopoverPlacement({
      trigger: box(300, 200),
      popover: { width: 288, height: 900 },
      viewport,
    });
    expect(p.side).toBe('bottom');
    expect(p.maxHeight).toBe(444);
    const q = computePopoverPlacement({
      trigger: box(500, 200),
      popover: { width: 288, height: 900 },
      viewport,
    });
    expect(q.side).toBe('top');
    expect(q.maxHeight).toBe(484);
    expect(q.top).toBe(500 - 8 - 484);
  });

  it('honours a top preference when it fits', () => {
    const p = computePopoverPlacement({
      trigger: box(300, 200),
      popover: { width: 100, height: 100 },
      viewport,
      preferred: 'top',
    });
    expect(p.side).toBe('top');
  });

  it('clamps horizontally on both edges', () => {
    const rightOverflow = computePopoverPlacement({
      trigger: box(100, 1100),
      popover: { width: 320, height: 100 },
      viewport,
      align: 'left',
    });
    expect(rightOverflow.left).toBe(1280 - 320 - 8);
    const leftOverflow = computePopoverPlacement({
      trigger: box(100, 0, 60),
      popover: { width: 320, height: 100 },
      viewport,
      align: 'right',
    });
    expect(leftOverflow.left).toBe(8);
  });
});
