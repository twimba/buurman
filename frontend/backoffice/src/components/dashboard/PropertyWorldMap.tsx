/// <reference types="@types/google.maps" />
import {
  Component,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import {
  APIProvider,
  Map,
  useMap,
  useMapsLibrary,
} from '@vis.gl/react-google-maps';
import { MarkerClusterer } from '@googlemaps/markerclusterer';
import { Flame, MapPin, X } from 'lucide-react';

import { env } from '../../config/env';
import type { PropertyPoint } from '../../generated/models';
import { usePropertyLocations } from '../../hooks/dashboard';

type MapMode = 'markers' | 'heatmap';

/** Fits the viewport to all points once, when they first arrive. */
const FitBounds = ({ points }: { points: PropertyPoint[] }) => {
  const map = useMap();
  useEffect(() => {
    if (!map || points.length === 0) {
      return;
    }
    const bounds = new google.maps.LatLngBounds();
    points.forEach((p) => bounds.extend({ lat: p.lat, lng: p.lng }));
    map.fitBounds(bounds, 48);
  }, [map, points]);
  return null;
};

/** Clustered markers — the clusterer collapses dense areas so large sets stay renderable. */
const MarkersLayer = ({ points }: { points: PropertyPoint[] }) => {
  const map = useMap();
  useEffect(() => {
    if (!map) {
      return;
    }
    const markers = points.map(
      (p) => new google.maps.Marker({ position: { lat: p.lat, lng: p.lng } })
    );
    const clusterer = new MarkerClusterer({ map, markers });
    return () => {
      clusterer.clearMarkers();
      markers.forEach((m) => m.setMap(null));
    };
  }, [map, points]);
  return null;
};

// Minimal shape we use from the visualization library — avoids @types/google.maps version quirks.
interface HeatmapLayerLike {
  setMap(map: google.maps.Map | null): void;
}
interface VisualizationLib {
  HeatmapLayer: new (opts: {
    data: google.maps.LatLng[];
    radius?: number;
    opacity?: number;
  }) => HeatmapLayerLike;
}

/**
 * Density heatmap via the Google visualization library. That library is deprecated and absent in
 * some Maps versions, so this guards the constructor, filters non-finite coords (an invalid LatLng
 * throws — unlike a Marker), and reports unavailability instead of crashing the panel.
 */
const HeatmapLayer = ({
  points,
  onUnavailable,
}: {
  points: PropertyPoint[];
  onUnavailable: () => void;
}) => {
  const map = useMap();
  const viz = useMapsLibrary(
    'visualization'
  ) as unknown as VisualizationLib | null;
  useEffect(() => {
    if (!map || !viz) {
      return;
    }
    if (typeof viz.HeatmapLayer !== 'function') {
      onUnavailable();
      return;
    }
    const data = points
      .filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng))
      .map((p) => new google.maps.LatLng(p.lat, p.lng));
    let layer: HeatmapLayerLike | null = null;
    try {
      layer = new viz.HeatmapLayer({ data, radius: 18, opacity: 0.7 });
      layer.setMap(map);
    } catch (e) {
      console.error('Property heatmap unavailable', e);
      onUnavailable();
      return;
    }
    return () => layer?.setMap(null);
  }, [map, viz, points, onUnavailable]);
  return null;
};

/** Keeps a single failing map layer from tearing down the whole dashboard tree. */
class LayerErrorBoundary extends Component<
  { onError: () => void; children: ReactNode },
  { failed: boolean }
> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error: unknown) {
    console.error('Map layer error', error);
    this.props.onError();
  }

  render() {
    return this.state.failed ? null : this.props.children;
  }
}

const ToggleButton = ({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    aria-pressed={active}
    className={`focus-ring inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
      active
        ? 'bg-primary-600 text-white'
        : 'bg-surface-card text-text-secondary hover:text-text-primary'
    }`}
  >
    {children}
  </button>
);

export const PropertyWorldMap = ({ onClose }: { onClose: () => void }) => {
  const [mode, setMode] = useState<MapMode>('markers');
  const [heatmapUnavailable, setHeatmapUnavailable] = useState(false);
  const markHeatmapUnavailable = useCallback(
    () => setHeatmapUnavailable(true),
    []
  );
  const { data, isLoading, isError } = usePropertyLocations(true);
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');
  const points = data?.points ?? [];
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  // Close on Escape.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  // Modal contract: lock background scroll, move focus in, restore it to the opener on close.
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();
    return () => {
      document.body.style.overflow = prevOverflow;
      opener?.focus?.();
    };
  }, []);

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex flex-col bg-surface-page"
      role="dialog"
      aria-modal="true"
      aria-labelledby="property-world-map-title"
    >
      <header className="flex items-center justify-between gap-3 border-b border-border-default bg-surface-card px-4 py-2.5">
        <div className="flex items-baseline gap-2">
          <h2
            id="property-world-map-title"
            className="text-sm font-semibold text-text-primary"
          >
            Property world map
          </h2>
          {data && (
            <span className="text-xs text-text-muted tabular-nums">
              {data.capped
                ? `${data.total.toLocaleString()} properties shown (capped — more exist)`
                : `${data.total.toLocaleString()} properties`}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 rounded-lg border border-border-default p-0.5">
            <ToggleButton
              active={mode === 'markers'}
              onClick={() => setMode('markers')}
            >
              <MapPin className="h-4 w-4" aria-hidden="true" /> Markers
            </ToggleButton>
            <ToggleButton
              active={mode === 'heatmap'}
              onClick={() => setMode('heatmap')}
            >
              <Flame className="h-4 w-4" aria-hidden="true" /> Heatmap
            </ToggleButton>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            aria-label="Close map"
            className="focus-ring rounded-md p-1.5 text-text-secondary hover:text-text-primary"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div className="relative flex-1">
        {!apiKey ? (
          <div className="flex h-full items-center justify-center p-6 text-center text-sm text-text-secondary">
            Google Maps API key not configured (set VITE_GOOGLE_MAPS_API_KEY).
          </div>
        ) : isLoading ? (
          <div className="flex h-full items-center justify-center text-sm text-text-secondary">
            Loading {points.length > 0 ? '' : 'property locations…'}
          </div>
        ) : isError ? (
          <div className="flex h-full items-center justify-center text-sm text-error-text">
            Failed to load property locations.
          </div>
        ) : points.length === 0 ? (
          <div className="flex h-full items-center justify-center text-sm text-text-secondary">
            No geocoded properties to plot.
          </div>
        ) : (
          <APIProvider apiKey={apiKey}>
            <Map
              defaultCenter={{ lat: 20, lng: 0 }}
              defaultZoom={2}
              gestureHandling="greedy"
              disableDefaultUI={false}
              className="h-full w-full"
            >
              <FitBounds points={points} />
              <LayerErrorBoundary onError={markHeatmapUnavailable}>
                {mode === 'markers' || heatmapUnavailable ? (
                  // Fall back to markers so the map is never blank when the heatmap can't render.
                  <MarkersLayer points={points} />
                ) : (
                  <HeatmapLayer
                    points={points}
                    onUnavailable={markHeatmapUnavailable}
                  />
                )}
              </LayerErrorBoundary>
            </Map>
          </APIProvider>
        )}

        {mode === 'heatmap' && heatmapUnavailable && (
          <div className="pointer-events-none absolute inset-x-0 top-3 flex justify-center">
            <span className="pointer-events-auto rounded-md bg-surface-card px-3 py-1.5 text-xs text-text-secondary shadow-md">
              Heatmap isn’t supported by the loaded Maps version — switch to
              Markers.
            </span>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};
