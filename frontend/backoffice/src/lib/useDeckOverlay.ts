import { useEffect, useRef } from 'react';
import { useMap } from '@vis.gl/react-google-maps';
import { GoogleMapsOverlay } from '@deck.gl/google-maps';
import type { Layer } from '@deck.gl/core';

/**
 * Attaches a deck.gl GoogleMapsOverlay to the surrounding <Map> and keeps its layers in sync.
 * deck.gl is Google's own recommended replacement for the (removed) visualization HeatmapLayer,
 * and renders choropleth/scatter far better than the Maps Data layer. Must be rendered inside <Map>.
 */
export const useDeckOverlay = (layers: Layer[]): void => {
  const map = useMap();
  const overlayRef = useRef<GoogleMapsOverlay | null>(null);

  useEffect(() => {
    if (!map) {
      return;
    }
    const overlay = new GoogleMapsOverlay({ layers });
    overlay.setMap(map);
    overlayRef.current = overlay;
    return () => {
      overlay.setMap(null);
      overlayRef.current = null;
    };
    // Overlay lifecycle is tied to the map; layer updates are handled by the effect below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map]);

  useEffect(() => {
    overlayRef.current?.setProps({ layers });
  }, [layers]);
};
