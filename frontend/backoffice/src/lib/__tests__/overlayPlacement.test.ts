import { describe, expect, it } from 'vitest';
import { computePlacement } from '../overlayPlacement';

const viewport = { width: 1000, height: 800 };
const panel = { width: 300, height: 200 };
const rect = (left: number, top: number, w = 20, h = 20) => ({
  left,
  top,
  right: left + w,
  bottom: top + h,
});

describe('computePlacement', () => {
  it('places below the trigger when there is room', () => {
    const p = computePlacement(rect(100, 100), panel, viewport, 'left');
    expect(p.placement).toBe('below');
    expect(p.top).toBe(124);
    expect(p.left).toBe(100);
  });
  it('flips above when there is no room below', () => {
    const p = computePlacement(rect(100, 700), panel, viewport, 'left');
    expect(p.placement).toBe('above');
    expect(p.top).toBe(700 - 4 - 200);
  });
  it('aligns the right edges for align right', () => {
    const p = computePlacement(rect(600, 100), panel, viewport, 'right');
    expect(p.left).toBe(620 - 300);
  });
  it('clamps horizontally inside the viewport', () => {
    expect(computePlacement(rect(950, 100), panel, viewport, 'left').left).toBe(
      1000 - 300 - 8
    );
    expect(
      computePlacement(rect(-50, 100), panel, viewport, 'right').left
    ).toBe(8);
  });
  it('picks the roomier side and caps height when neither fits', () => {
    const tall = { width: 300, height: 900 };
    const p = computePlacement(rect(100, 500), tall, viewport, 'left');
    expect(p.placement).toBe('above');
    expect(p.maxHeight).toBe(500 - 4 - 8);
  });
});
