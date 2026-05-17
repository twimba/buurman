import { useEffect, useRef, useState, type ImgHTMLAttributes } from 'react';
import { cn } from '../utils/cn';

export interface LazyImageProps extends Omit<
  ImgHTMLAttributes<HTMLImageElement>,
  'src'
> {
  /** Image URL. Loaded only when the wrapper enters the viewport (or within rootMargin). */
  src: string;
  /** Class on the outer wrapper div (positioning, sizing). */
  wrapperClassName?: string;
  /** Pre-mount placeholder color. Default tailwind bg-surface-inset. */
  placeholderClassName?: string;
  /**
   * IntersectionObserver rootMargin. Default expands by one full viewport so
   * the image is already decoded by the time the user scrolls to it.
   */
  rootMargin?: string;
}

/**
 * Image element that only requests its src after the wrapper enters the
 * viewport (or within `rootMargin`). Falls back to native `loading="lazy"` on
 * the `<img>` tag for browsers without IntersectionObserver. Once loaded, the
 * observer disconnects so further scroll events are cheap.
 */
export function LazyImage({
  src,
  alt,
  className,
  wrapperClassName,
  placeholderClassName,
  rootMargin = '100% 0% 100% 0%',
  ...imgProps
}: LazyImageProps) {
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const [shouldLoad, setShouldLoad] = useState(() => {
    if (typeof window === 'undefined') {
      return true;
    }
    return typeof IntersectionObserver === 'undefined';
  });

  useEffect(() => {
    if (shouldLoad || typeof IntersectionObserver === 'undefined') {
      return;
    }
    const node = wrapperRef.current;
    if (!node) {
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setShouldLoad(true);
          io.disconnect();
        }
      },
      { rootMargin }
    );
    io.observe(node);
    return () => io.disconnect();
  }, [shouldLoad, rootMargin]);

  return (
    <div
      ref={wrapperRef}
      className={cn(
        'relative w-full h-full',
        !shouldLoad && (placeholderClassName ?? 'bg-surface-inset'),
        wrapperClassName
      )}
    >
      {shouldLoad ? (
        <img
          src={src}
          alt={alt}
          loading="lazy"
          decoding="async"
          className={className}
          {...imgProps}
        />
      ) : null}
    </div>
  );
}

LazyImage.displayName = 'LazyImage';
