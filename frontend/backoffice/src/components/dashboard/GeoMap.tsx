/// <reference types="@types/google.maps" />
import { useEffect, useState } from 'react';
import {
  APIProvider,
  Map as GoogleMap,
  useMap,
} from '@vis.gl/react-google-maps';
import { feature } from 'topojson-client';

// Vendored Natural Earth 50m country polygons. The `?url` import emits a content-hashed asset
// served from our own origin, so the browser fetches it once and then serves it from immutable
// cache forever — no external CDN dependency and no repeat downloads.
import geoUrl from '../../assets/countries-50m.json?url';
import { env } from '../../config/env';
import type { CountryStats } from '../../generated/models';
import { COUNTRY_CENTROIDS } from '../../lib/countryCentroids';
import { formatEurMinor } from '../../lib/money';

export type GeoMetric = 'teams' | 'properties' | 'value';

// Our stats use ISO-2; world-atlas features are keyed by ISO 3166-1 numeric id.
const ISO2_TO_NUM: Record<string, number> = {
  NL: 528,
  BE: 56,
  DE: 276,
  FR: 250,
  GB: 826,
  IE: 372,
  LU: 442,
  PT: 620,
  ES: 724,
  IT: 380,
  AT: 40,
  CH: 756,
  DK: 208,
  SE: 752,
  NO: 578,
  FI: 246,
  PL: 616,
  CZ: 203,
  SK: 703,
  HU: 348,
  RO: 642,
  BG: 100,
  GR: 300,
  HR: 191,
  SI: 705,
  EE: 233,
  LV: 428,
  LT: 440,
  CY: 196,
  MT: 470,
  US: 840,
  CA: 124,
  BR: 76,
  AU: 36,
  NZ: 554,
  ZA: 710,
  AE: 784,
  IN: 356,
  JP: 392,
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
  // Hide the basemap's own country/province border lines so they don't sit a pixel or two off
  // from our choropleth polygon strokes (the doubled-border artifact). Our polygons own the borders.
  {
    featureType: 'administrative',
    elementType: 'geometry',
    stylers: [{ visibility: 'off' }],
  },
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
    const res = await fetch(geoUrl);
    const topo = await res.json();
    geoCache = feature(topo, topo.objects.countries) as unknown as object;
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
    const byNum = new Map<number, CountryStats>();
    countries.forEach((c) => {
      const num = ISO2_TO_NUM[c.code.toUpperCase()];
      if (num !== undefined) {
        byNum.set(num, c);
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
      map.data.setStyle((feat) => {
        const c = byNum.get(Number(feat.getId()));
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
        const c = e.feature ? byNum.get(Number(e.feature.getId())) : undefined;
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
