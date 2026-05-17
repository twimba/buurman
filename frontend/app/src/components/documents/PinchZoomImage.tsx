import { useGesture } from '@use-gesture/react';
import { useEffect, useRef, useState, type ImgHTMLAttributes } from 'react';

interface PinchZoomImageProps
  extends Omit<ImgHTMLAttributes<HTMLImageElement>, 'onDrag'> {
  /** Maximum scale via pinch. Default 4×. */
  maxScale?: number;
  /**
   * Called when the user pinches OR releases. Consumer may disable horizontal
   * swipe-prev/next while `scale > 1` so the page-swipe gesture doesn't fight
   * the image-pan gesture.
   */
  onZoomChange?: (zoomed: boolean) => void;
}

/**
 * `<img>` with pinch-to-zoom + pan when zoomed (touch only). On non-touch
 * pointers the image renders normally — pan/zoom is unavailable but the
 * surrounding modal still works.
 *
 * Double-tap toggles between 1× and 2× as an iOS-style fallback for users who
 * can't pinch (e.g. one-handed).
 */
export function PinchZoomImage({
  maxScale = 4,
  onZoomChange,
  style,
  ...imgProps
}: PinchZoomImageProps) {
  const ref = useRef<HTMLImageElement | null>(null);
  const [scale, setScale] = useState(1);
  const [tx, setTx] = useState(0);
  const [ty, setTy] = useState(0);

  useEffect(() => {
    onZoomChange?.(scale > 1);
  }, [scale, onZoomChange]);

  useGesture(
    {
      onPinch: ({ offset: [s], origin: [, ] }) => {
        const next = Math.min(maxScale, Math.max(1, s));
        setScale(next);
        if (next === 1) {
          setTx(0);
          setTy(0);
        }
      },
      onDrag: ({ offset: [x, y] }) => {
        if (scale > 1) {
          setTx(x);
          setTy(y);
        }
      },
    },
    {
      target: ref,
      eventOptions: { passive: false },
      pinch: { scaleBounds: { min: 1, max: maxScale } },
      drag: { from: () => [tx, ty] },
    }
  );

  const handleDoubleClick = () => {
    if (scale > 1) {
      setScale(1);
      setTx(0);
      setTy(0);
    } else {
      setScale(2);
    }
  };

  return (
    <img
      ref={ref}
      onDoubleClick={handleDoubleClick}
      style={{
        ...style,
        transform: `translate(${tx}px, ${ty}px) scale(${scale})`,
        transformOrigin: 'center center',
        transition: scale === 1 ? 'transform 160ms ease-out' : 'none',
        touchAction: 'none',
        userSelect: 'none',
        WebkitUserSelect: 'none',
      }}
      draggable={false}
      {...imgProps}
    />
  );
}

PinchZoomImage.displayName = 'PinchZoomImage';
