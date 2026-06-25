/// <reference types="@types/google.maps" />
import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { APIProvider, Map, useMap } from '@vis.gl/react-google-maps';
import { ScatterplotLayer } from '@deck.gl/layers';
import { HeatmapLayer } from '@deck.gl/aggregation-layers';
import type { Layer } from '@deck.gl/core';
import { Flame, MapPin, X } from 'lucide-react';

import { env } from '../../config/env';
import type { PropertyPoint } from '../../generated/models';
import { usePropertyLocations } from '../../hooks/dashboard';
import { useDeckOverlay } from '../../lib/useDeckOverlay';

type MapMode = 'markers' | 'heatmap';

// On-brand density ramp (cool indigo → vivid magenta) so the heatmap matches the dashboard palette
// instead of deck.gl's default green-yellow-red.
const HEAT_COLOR_RANGE: [number, number, number][] = [
  [224, 231, 255],
  [165, 180, 252],
  [129, 140, 248],
  [99, 102, 241],
  [124, 58, 237],
  [217, 70, 239],
];

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

/**
 * Renders property points through a deck.gl overlay: GPU scatter for the markers view and a
 * deck.gl HeatmapLayer for density. Replaces the deprecated Google visualization HeatmapLayer
 * (removed May 2026) and the legacy google.maps.Marker clusterer.
 */
const PropertyDeckLayers = ({
  points,
  mode,
}: {
  points: PropertyPoint[];
  mode: MapMode;
}) => {
  const layers = useMemo<Layer[]>(() => {
    const data = points.filter(
      (p) => Number.isFinite(p.lat) && Number.isFinite(p.lng)
    );
    if (mode === 'heatmap') {
      return [
        new HeatmapLayer<PropertyPoint>({
          id: 'property-heat',
          data,
          getPosition: (p) => [p.lng, p.lat],
          getWeight: 1,
          radiusPixels: 36,
          intensity: 1.1,
          threshold: 0.04,
          colorRange: HEAT_COLOR_RANGE,
        }),
      ];
    }
    return [
      new ScatterplotLayer<PropertyPoint>({
        id: 'property-markers',
        data,
        getPosition: (p) => [p.lng, p.lat],
        getFillColor: [79, 70, 229, 210],
        getLineColor: [255, 255, 255, 230],
        lineWidthMinPixels: 1,
        stroked: true,
        radiusUnits: 'pixels',
        getRadius: 5,
        radiusMinPixels: 3,
        radiusMaxPixels: 9,
        pickable: true,
      }),
    ];
  }, [points, mode]);

  useDeckOverlay(layers);
  return null;
};

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
  const { data, isLoading, isError } = usePropertyLocations(true);
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');
  const mapId = env('VITE_GOOGLE_MAPS_MAP_ID') || 'DEMO_MAP_ID';
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
            Maps API key not configured (set VITE_GOOGLE_MAPS_API_KEY).
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
              mapId={mapId}
              gestureHandling="greedy"
              disableDefaultUI={false}
              className="h-full w-full"
            >
              <FitBounds points={points} />
              <PropertyDeckLayers points={points} mode={mode} />
            </Map>
          </APIProvider>
        )}
        {mode === 'heatmap' && points.length > 0 && (
          <div
            className="pointer-events-none absolute bottom-3 left-3 z-10 rounded-lg border border-border-default bg-surface-card/90 px-3 py-2 backdrop-blur"
            style={{ boxShadow: 'var(--shadow-raised)' }}
          >
            <p className="mb-1 text-[10px] font-semibold uppercase tracking-wider text-text-muted">
              Density
            </p>
            <div
              className="h-2 w-32 rounded-full"
              style={{
                background: `linear-gradient(to right, ${HEAT_COLOR_RANGE.map(
                  ([r, g, b]) => `rgb(${r}, ${g}, ${b})`
                ).join(', ')})`,
              }}
            />
            <div className="mt-1 flex justify-between text-[10px] text-text-secondary">
              <span>Sparse</span>
              <span>Dense</span>
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};
