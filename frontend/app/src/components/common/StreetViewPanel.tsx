/// <reference types="@types/google.maps" />
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMapsLibrary } from '@vis.gl/react-google-maps';
import { Loader2, MapPin } from 'lucide-react';

interface StreetViewPanelProps {
  lat: number;
  lng: number;
  title?: string;
}

type Status = 'loading' | 'ok' | 'none';

/**
 * Google Street View panorama for a coordinate. Probes StreetViewService for the nearest
 * panorama within a small radius and falls back gracefully when no imagery exists (common
 * for rural addresses), instead of rendering a blank/grey pane.
 */
export const StreetViewPanel = ({ lat, lng, title }: StreetViewPanelProps) => {
  const { t } = useTranslation('properties');
  const streetViewLib = useMapsLibrary('streetView');
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<Status>('loading');

  useEffect(() => {
    if (!streetViewLib || !containerRef.current) {
      return;
    }
    const container = containerRef.current;
    let panorama: google.maps.StreetViewPanorama | null = null;
    let cancelled = false;
    const service = new streetViewLib.StreetViewService();
    setStatus('loading');

    service
      .getPanorama({ location: { lat, lng }, radius: 60 })
      .then((response) => {
        if (cancelled || !containerRef.current) {
          return;
        }
        panorama = new streetViewLib.StreetViewPanorama(containerRef.current, {
          pano: response.data.location?.pano,
          visible: true,
          addressControl: false,
          fullscreenControl: false,
          motionTracking: false,
          motionTrackingControl: false,
          enableCloseButton: false,
        });
        setStatus('ok');
      })
      .catch(() => {
        if (!cancelled) {
          setStatus('none');
        }
      });

    return () => {
      cancelled = true;
      // Fully dispose: StreetViewPanorama is a heavy WebGL object with its own listeners. Without
      // this, dragging a pin (each lat/lng change) stacks a new panorama into the same node while
      // old ones keep their GL context + listeners — a real memory/GPU/listener leak.
      if (panorama) {
        panorama.setVisible(false);
        google.maps.event.clearInstanceListeners(panorama);
        panorama = null;
      }
      container.replaceChildren();
    };
  }, [streetViewLib, lat, lng]);

  return (
    <div className="relative h-full w-full">
      <div
        ref={containerRef}
        aria-label={title}
        className={`h-full w-full ${status === 'ok' ? '' : 'invisible'}`}
      />
      {status !== 'ok' && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-surface-page text-center">
          {status === 'loading' ? (
            <>
              <Loader2 className="mb-3 h-10 w-10 animate-spin text-primary-500" />
              <p className="text-text-secondary">{t('map.loading')}</p>
            </>
          ) : (
            <>
              <MapPin className="mb-3 h-12 w-12 text-text-disabled" />
              <p className="text-text-secondary">{t('map.noStreetView')}</p>
            </>
          )}
        </div>
      )}
    </div>
  );
};
