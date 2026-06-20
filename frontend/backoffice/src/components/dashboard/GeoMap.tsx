/// <reference types="@types/google.maps" />
import { useEffect, useState } from 'react';
import {
  APIProvider,
  Map as GoogleMap,
  useMap,
} from '@vis.gl/react-google-maps';

import { env } from '../../config/env';
import type { CountryStats } from '../../generated/models';
import { COUNTRY_CENTROIDS } from '../../lib/countryCentroids';
import { formatEurMinor } from '../../lib/money';

export type GeoMetric = 'teams' | 'properties' | 'value';

// Low-res world country polygons (features keyed by ISO 3166-1 alpha-3 id). Served with CORS by
// jsDelivr; fetched once and cached for the session.
const GEOJSON_URL =
  'https://cdn.jsdelivr.net/gh/johan/world.geo.json@master/countries.geo.json';

// Our stats use ISO-2; the GeoJSON features are keyed by ISO-3.
const ISO2_TO_3: Record<string, string> = {
  NL: 'NLD',
  BE: 'BEL',
  DE: 'DEU',
  FR: 'FRA',
  GB: 'GBR',
  IE: 'IRL',
  LU: 'LUX',
  PT: 'PRT',
  ES: 'ESP',
  IT: 'ITA',
  AT: 'AUT',
  CH: 'CHE',
  DK: 'DNK',
  SE: 'SWE',
  NO: 'NOR',
  FI: 'FIN',
  PL: 'POL',
  CZ: 'CZE',
  SK: 'SVK',
  HU: 'HUN',
  RO: 'ROU',
  BG: 'BGR',
  GR: 'GRC',
  HR: 'HRV',
  SI: 'SVN',
  EE: 'EST',
  LV: 'LVA',
  LT: 'LTU',
  CY: 'CYP',
  MT: 'MLT',
  US: 'USA',
  CA: 'CAN',
  BR: 'BRA',
  AU: 'AUS',
  NZ: 'NZL',
  ZA: 'ZAF',
  AE: 'ARE',
  IN: 'IND',
  JP: 'JPN',
};

const regionNames =
  typeof Intl !== 'undefined' && 'DisplayNames' in Intl
    ? new Intl.DisplayNames(['en'], { type: 'region' })
    : null;

const countryName = (code: string): string => {
  try {
    return regionNames?.of(code.toUpperCase()) ?? code;
  } catch {
    return code;
  }
};

const metricValue = (c: CountryStats, m: GeoMetric): number =>
  m === 'teams'
    ? c.teams
    : m === 'properties'
      ? c.properties
      : c.monthlyValueEurMinor;

// Indigo ramp (light → dark) for choropleth intensity.
const RAMP = ['#e0e7ff', '#c7d2fe', '#a5b4fc', '#818cf8', '#6366f1', '#4f46e5'];

const MAP_STYLES: google.maps.MapTypeStyle[] = [
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'road', stylers: [{ visibility: 'off' }] },
  {
    featureType: 'administrative',
    elementType: 'labels',
    stylers: [{ saturation: -70 }],
  },
  { featureType: 'water', stylers: [{ color: '#dfe6f3' }] },
  { featureType: 'landscape', stylers: [{ color: '#f3f5fb' }] },
];

type Hover = { c: CountryStats; x: number; y: number };

let geoCache: object | null = null;
const loadGeo = async (): Promise<object | null> => {
  if (geoCache) {
    return geoCache;
  }
  try {
    const res = await fetch(GEOJSON_URL);
    geoCache = (await res.json()) as object;
    return geoCache;
  } catch {
    return null;
  }
};

const Choropleth = ({
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
    let cancelled = false;
    const byIso3 = new Map<string, CountryStats>();
    countries.forEach((c) => {
      const i3 = ISO2_TO_3[c.code.toUpperCase()];
      if (i3) {
        byIso3.set(i3, c);
      }
    });
    const max = Math.max(1, ...countries.map((c) => metricValue(c, metric)));

    const clear = () => {
      google.maps.event.clearListeners(map.data, 'mouseover');
      google.maps.event.clearListeners(map.data, 'mousemove');
      google.maps.event.clearListeners(map.data, 'mouseout');
      map.data.forEach((f) => map.data.remove(f));
    };

    loadGeo().then((geo) => {
      if (cancelled || !map || !geo) {
        return;
      }
      clear();
      map.data.addGeoJson(geo);
      map.data.setStyle((feature) => {
        const c = byIso3.get(String(feature.getId()));
        if (!c) {
          return {
            fillColor: '#e8ebf3',
            fillOpacity: 0.35,
            strokeColor: '#cdd2e2',
            strokeWeight: 0.5,
          };
        }
        const ratio = metricValue(c, metric) / max;
        return {
          fillColor:
            RAMP[
              Math.min(RAMP.length - 1, Math.ceil(ratio * (RAMP.length - 1)))
            ],
          fillOpacity: 0.9,
          strokeColor: '#ffffff',
          strokeWeight: 0.8,
        };
      });

      const show = (e: google.maps.Data.MouseEvent) => {
        const c = e.feature ? byIso3.get(String(e.feature.getId())) : undefined;
        if (!c) {
          onHover(null);
          return;
        }
        map.data.revertStyle();
        map.data.overrideStyle(e.feature, {
          strokeColor: '#4f46e5',
          strokeWeight: 2,
          zIndex: 10,
          fillOpacity: 1,
        });
        const de = e.domEvent as MouseEvent;
        onHover({ c, x: de.clientX, y: de.clientY });
      };
      map.data.addListener('mouseover', show);
      map.data.addListener('mousemove', show);
      map.data.addListener('mouseout', () => {
        map.data.revertStyle();
        onHover(null);
      });

      // Frame the countries that actually have data.
      const bounds = new google.maps.LatLngBounds();
      let any = false;
      countries.forEach((c) => {
        const center = COUNTRY_CENTROIDS[c.code.toUpperCase()];
        if (center && metricValue(c, metric) > 0) {
          bounds.extend(center);
          any = true;
        }
      });
      if (any) {
        map.fitBounds(bounds, 64);
      }
    });

    return () => {
      cancelled = true;
      if (map) {
        clear();
      }
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
      className="pointer-events-none fixed z-50 w-52 -translate-x-1/2 -translate-y-[calc(100%+16px)] rounded-xl border border-border-default bg-surface-card p-3"
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

  if (!apiKey) {
    return (
      <p className="py-6 text-center text-xs text-text-secondary">
        Map unavailable — maps aren’t configured for this environment.
      </p>
    );
  }

  return (
    <div className="relative h-full min-h-[320px] overflow-hidden rounded-lg border border-border-default">
      <APIProvider apiKey={apiKey}>
        <GoogleMap
          defaultCenter={{ lat: 48, lng: 8 }}
          defaultZoom={4}
          gestureHandling="greedy"
          disableDefaultUI
          styles={MAP_STYLES}
          className="h-full w-full"
        >
          <Choropleth
            countries={countries}
            metric={metric}
            onHover={setHover}
          />
        </GoogleMap>
      </APIProvider>
      {hover && <Tooltip hover={hover} />}
    </div>
  );
};
