/// <reference types="@types/google.maps" />
import { useEffect, useMemo, useState } from 'react';
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
  KR: 410,
  SG: 702,
  ID: 360,
  TR: 792,
  RU: 643,
  MX: 484,
  AR: 32,
  CL: 152,
  CO: 170,
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

// Brand sequential scale (light → deep indigo → violet) for choropleth intensity. Used both as the
// continuous interpolation source and as the legend gradient, so the two always match.
const RAMP = [
  '#eef2ff',
  '#c7d2fe',
  '#a5b4fc',
  '#818cf8',
  '#6366f1',
  '#4f46e5',
  '#4338ca',
  '#3730a3',
];

type RGB = [number, number, number];

const hexToRgb = (hex: string): RGB => {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
};

const RAMP_RGB = RAMP.map(hexToRgb);

/** Continuous color across the full RAMP for t ∈ [0,1] — smooth gradation, not 6 hard buckets. */
const rampColor = (t: number): string => {
  const clamped = Math.min(1, Math.max(0, t));
  const span = RAMP_RGB.length - 1;
  const pos = clamped * span;
  const i = Math.min(span - 1, Math.floor(pos));
  const f = pos - i;
  const [r1, g1, b1] = RAMP_RGB[i];
  const [r2, g2, b2] = RAMP_RGB[i + 1];
  const r = Math.round(r1 + (r2 - r1) * f);
  const g = Math.round(g1 + (g2 - g1) * f);
  const b = Math.round(b1 + (b2 - b1) * f);
  return `rgb(${r}, ${g}, ${b})`;
};

/**
 * Perceptual scale: sqrt spreads the low end so a couple of dominant countries don't wash every
 * other data-country into the same pale tone (the classic choropleth "one bright, rest invisible"
 * failure). Returns t ∈ [0,1].
 */
const intensity = (value: number, max: number): number =>
  max <= 0 ? 0 : Math.sqrt(value / max);

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
  // Calm, low-chroma basemap so the indigo choropleth reads as the figure and the world as ground.
  {
    featureType: 'water',
    elementType: 'geometry',
    stylers: [{ color: '#e2e8f5' }],
  },
  {
    featureType: 'landscape',
    elementType: 'geometry',
    stylers: [{ color: '#f5f6fb' }],
  },
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

  const byNum = useMemo(() => {
    const m = new Map<number, CountryStats>();
    countries.forEach((c) => {
      const num = ISO2_TO_NUM[c.code.toUpperCase()];
      if (num !== undefined) {
        m.set(num, c);
      }
    });
    return m;
  }, [countries]);

  // Geometry + listeners + initial framing. Runs only when the map, the data set, or the hover
  // handler changes — NOT on metric toggle, so switching metric never re-parses the 241 features
  // or resets the user's pan/zoom.
  useEffect(() => {
    if (!map) {
      return;
    }
    let cancelled = false;
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

      // Frame the countries that have data. Extend by their real polygon extents (not a single
      // centroid) so one data-country frames sensibly instead of zooming to a point.
      const bounds = new google.maps.LatLngBounds();
      let any = false;
      map.data.forEach((feat) => {
        if (byNum.has(Number(feat.getId()))) {
          feat.getGeometry()?.forEachLatLng((ll) => bounds.extend(ll));
          any = true;
        }
      });
      if (any) {
        map.fitBounds(bounds, 64);
        // Cap the zoom so a small/single country keeps regional context instead of a solid block.
        google.maps.event.addListenerOnce(map, 'idle', () => {
          const z = map.getZoom();
          if (z !== undefined && z > 5) {
            map.setZoom(5);
          }
        });
      }
    });

    return () => {
      cancelled = true;
      if (map) {
        clear();
      }
    };
  }, [map, byNum, onHover]);

  // Style only — re-applied on metric change (and data change). The Data layer applies this
  // function to all features, including those added asynchronously above.
  useEffect(() => {
    if (!map) {
      return;
    }
    const max = Math.max(1, ...countries.map((c) => metricValue(c, metric)));
    map.data.setStyle((feat) => {
      const c = byNum.get(Number(feat.getId()));
      if (!c) {
        // Countries with no data: a faint neutral fill so the world stays legible behind the metric.
        return {
          fillColor: '#e6e9f4',
          fillOpacity: 0.4,
          strokeColor: '#d4d9ea',
          strokeWeight: 0.5,
        };
      }
      const value = metricValue(c, metric);
      return {
        fillColor: value > 0 ? rampColor(intensity(value, max)) : '#e6e9f4',
        fillOpacity: value > 0 ? 0.92 : 0.5,
        strokeColor: '#ffffff',
        strokeWeight: 0.9,
      };
    });
  }, [map, byNum, countries, metric]);
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

const METRIC_LABEL: Record<GeoMetric, string> = {
  teams: 'Teams',
  properties: 'Properties',
  value: 'Monthly value',
};

const formatMetric = (value: number, metric: GeoMetric): string =>
  metric === 'value' ? formatEurMinor(value) : value.toLocaleString();

/** Compact choropleth legend: the brand gradient with 0 → max scale and the active metric name. */
const Legend = ({ max, metric }: { max: number; metric: GeoMetric }) => (
  <div
    className="pointer-events-none absolute bottom-3 left-3 z-10 rounded-lg border border-border-default bg-surface-card/90 px-3 py-2 backdrop-blur"
    style={{ boxShadow: 'var(--shadow-raised)' }}
  >
    <p className="mb-1 text-[10px] font-semibold uppercase tracking-wider text-text-muted">
      {METRIC_LABEL[metric]}
    </p>
    <div
      className="h-2 w-32 rounded-full"
      style={{ background: `linear-gradient(to right, ${RAMP.join(', ')})` }}
    />
    <div className="mt-1 flex justify-between text-[10px] tabular-nums text-text-secondary">
      <span>0</span>
      <span>{formatMetric(max, metric)}</span>
    </div>
  </div>
);

export const GeoMap = ({
  countries,
  metric,
}: {
  countries: CountryStats[];
  metric: GeoMetric;
}) => {
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');
  const [hover, setHover] = useState<Hover | null>(null);
  const max = useMemo(
    () => Math.max(0, ...countries.map((c) => metricValue(c, metric))),
    [countries, metric]
  );
  const hasData = countries.some((c) => metricValue(c, metric) > 0);

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
          // Fill via absolute inset rather than h-full: the panel chain (flex-1 / min-h only,
          // no definite pixel height) makes a percentage height collapse to 0, which left the
          // map blank. inset-0 resolves height from the container's offsets instead.
          className="absolute inset-0"
        >
          <Choropleth
            countries={countries}
            metric={metric}
            onHover={setHover}
          />
        </GoogleMap>
      </APIProvider>
      {hasData && <Legend max={max} metric={metric} />}
      {hover && <Tooltip hover={hover} />}
      {/* Non-visual equivalent of the color-only choropleth: a screen-reader/keyboard-accessible
          table of every country with data for the active metric. */}
      <table className="sr-only">
        <caption>{`Country breakdown for ${METRIC_LABEL[metric]}`}</caption>
        <thead>
          <tr>
            <th scope="col">Country</th>
            <th scope="col">{METRIC_LABEL[metric]}</th>
          </tr>
        </thead>
        <tbody>
          {countries
            .filter((c) => metricValue(c, metric) > 0)
            .map((c) => (
              <tr key={c.code}>
                <th scope="row">{countryName(c.code)}</th>
                <td>{formatMetric(metricValue(c, metric), metric)}</td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
};
