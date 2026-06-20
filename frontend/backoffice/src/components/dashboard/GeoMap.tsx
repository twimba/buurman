/// <reference types="@types/google.maps" />
import { useEffect, useState } from 'react';
import { APIProvider, Map, useMap } from '@vis.gl/react-google-maps';

import { env } from '../../config/env';
import type { CountryStats } from '../../generated/models';
import { COUNTRY_CENTROIDS } from '../../lib/countryCentroids';
import { formatEurMinor } from '../../lib/money';

export type GeoMetric = 'teams' | 'properties' | 'value';

const regionNames =
  typeof Intl !== 'undefined' && 'DisplayNames' in Intl
    ? new Intl.DisplayNames(['en'], { type: 'region' })
    : null;

const countryName = (code: string): string => {
  if (/^[A-Za-z]{2}$/.test(code)) {
    try {
      return regionNames?.of(code.toUpperCase()) ?? code;
    } catch {
      return code;
    }
  }
  return code;
};

const metricValue = (c: CountryStats, m: GeoMetric): number =>
  m === 'teams'
    ? c.teams
    : m === 'properties'
      ? c.properties
      : c.monthlyValueEurMinor;

// Indigo ramp (light → dark) for bubble intensity.
const RAMP = ['#c7d2fe', '#a5b4fc', '#818cf8', '#6366f1', '#4f46e5'];

// Clean, desaturated base map so the data bubbles dominate.
const MAP_STYLES: google.maps.MapTypeStyle[] = [
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  {
    featureType: 'road',
    elementType: 'labels',
    stylers: [{ visibility: 'off' }],
  },
  {
    featureType: 'administrative',
    elementType: 'labels',
    stylers: [{ saturation: -60 }],
  },
  { featureType: 'water', stylers: [{ color: '#dfe6f3' }] },
  { featureType: 'landscape', stylers: [{ color: '#f3f5fb' }] },
];

type Hover = { c: CountryStats; x: number; y: number };

const Bubbles = ({
  countries,
  metric,
  onHover,
}: {
  countries: CountryStats[];
  metric: GeoMetric;
  onHover: (h: Hover | null) => void;
}) => {
  const map = useMap();
  useEffect(() => {
    if (!map) {
      return;
    }
    const max = Math.max(1, ...countries.map((c) => metricValue(c, metric)));
    const bounds = new google.maps.LatLngBounds();
    const markers = countries.map((c) => {
      const center = COUNTRY_CENTROIDS[c.code.toUpperCase()];
      const v = metricValue(c, metric);
      const ratio = v / max;
      bounds.extend(center);
      const marker = new google.maps.Marker({
        position: center,
        icon: {
          path: google.maps.SymbolPath.CIRCLE,
          scale: 8 + Math.sqrt(ratio) * 22,
          fillColor:
            RAMP[Math.min(RAMP.length - 1, Math.floor(ratio * RAMP.length))],
          fillOpacity: 0.82,
          strokeColor: '#ffffff',
          strokeWeight: 1.5,
        },
        zIndex: Math.round(ratio * 1000),
        optimized: false,
      });
      const move = (e: google.maps.MapMouseEvent) => {
        const de = e.domEvent as MouseEvent;
        onHover({ c, x: de.clientX, y: de.clientY });
      };
      marker.addListener('mouseover', move);
      marker.addListener('mousemove', move);
      marker.addListener('mouseout', () => onHover(null));
      return marker;
    });
    markers.forEach((m) => m.setMap(map));
    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, 56);
    }
    return () => {
      markers.forEach((m) => {
        google.maps.event.clearInstanceListeners(m);
        m.setMap(null);
      });
    };
  }, [map, countries, metric, onHover]);
  return null;
};

const Tooltip = ({ hover }: { hover: Hover }) => {
  const { c } = hover;
  const rows: [string, string][] = [
    ['Teams', c.teams.toLocaleString()],
    ['Properties', c.properties.toLocaleString()],
    ['Contracts', c.contracts.toLocaleString()],
    ['Monthly value', formatEurMinor(c.monthlyValueEurMinor)],
  ];
  return (
    <div
      className="pointer-events-none fixed z-50 w-52 -translate-x-1/2 -translate-y-[calc(100%+14px)] rounded-xl border border-border-default bg-surface-card p-3"
      style={{ left: hover.x, top: hover.y, boxShadow: 'var(--shadow-raised)' }}
    >
      <p className="mb-2 flex items-center gap-2 text-sm font-bold tracking-[-0.01em] text-text-primary">
        <span className="text-[15px]">{countryName(c.code)}</span>
        <span className="ml-auto text-[11px] font-semibold uppercase tracking-wider text-text-muted">
          {c.code.toUpperCase()}
        </span>
      </p>
      <dl className="space-y-1">
        {rows.map(([k, v]) => (
          <div
            key={k}
            className="flex items-baseline justify-between gap-3 text-xs"
          >
            <dt className="text-text-secondary">{k}</dt>
            <dd className="font-semibold tabular-nums text-text-primary">
              {v}
            </dd>
          </div>
        ))}
      </dl>
    </div>
  );
};

export const GeoMap = ({
  countries,
  metric,
}: {
  countries: CountryStats[];
  metric: GeoMetric;
}) => {
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');
  const [hover, setHover] = useState<Hover | null>(null);
  const plotted = countries.filter(
    (c) => COUNTRY_CENTROIDS[c.code.toUpperCase()] && metricValue(c, metric) > 0
  );

  if (!apiKey) {
    return (
      <p className="py-6 text-center text-xs text-text-secondary">
        Map unavailable — maps aren’t configured for this environment.
      </p>
    );
  }
  if (plotted.length === 0) {
    return (
      <p className="py-6 text-center text-xs text-text-secondary">
        No mappable country data yet.
      </p>
    );
  }

  return (
    <div className="relative h-[260px] overflow-hidden rounded-lg border border-border-default">
      <APIProvider apiKey={apiKey}>
        <Map
          defaultCenter={{ lat: 48, lng: 8 }}
          defaultZoom={3}
          gestureHandling="greedy"
          disableDefaultUI
          styles={MAP_STYLES}
          className="h-full w-full"
        >
          <Bubbles countries={plotted} metric={metric} onHover={setHover} />
        </Map>
      </APIProvider>
      {hover && <Tooltip hover={hover} />}
    </div>
  );
};
