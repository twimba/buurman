import { useState, useCallback } from 'react';
import {
  APIProvider,
  Map,
  AdvancedMarker,
  MapMouseEvent,
} from '@vis.gl/react-google-maps';
import { Loader2, MapPin, MousePointerClick } from 'lucide-react';
import { env } from '../../config/env';

// Approximate center coordinates for common countries
const COUNTRY_CENTERS: Record<string, { lat: number; lng: number }> = {
  NL: { lat: 52.13, lng: 5.29 },
  DE: { lat: 51.16, lng: 10.45 },
  BE: { lat: 50.5, lng: 4.47 },
  FR: { lat: 46.23, lng: 2.21 },
  GB: { lat: 55.38, lng: -3.44 },
  US: { lat: 37.09, lng: -95.71 },
  PT: { lat: 39.4, lng: -8.22 },
  ES: { lat: 40.46, lng: -3.75 },
  IT: { lat: 41.87, lng: 12.57 },
  AT: { lat: 47.52, lng: 14.55 },
  CH: { lat: 46.82, lng: 8.23 },
  LU: { lat: 49.82, lng: 6.13 },
  IE: { lat: 53.14, lng: -7.69 },
  DK: { lat: 56.26, lng: 9.5 },
  SE: { lat: 60.13, lng: 18.64 },
  NO: { lat: 60.47, lng: 8.47 },
  FI: { lat: 61.92, lng: 25.75 },
  PL: { lat: 51.92, lng: 19.15 },
  CZ: { lat: 49.82, lng: 15.47 },
  GR: { lat: 39.07, lng: 21.82 },
  BR: { lat: -14.24, lng: -51.93 },
  CA: { lat: 56.13, lng: -106.35 },
  AU: { lat: -25.27, lng: 133.78 },
};

const DEFAULT_CENTER = { lat: 50.0, lng: 10.0 }; // Europe center

interface InteractiveMapProps {
  street: string;
  city: string;
  latitude?: number | null;
  longitude?: number | null;
  geocodeAccuracy?: string | null;
  isGeocoding?: boolean;
  onLocationChange?: (lat: number, lng: number) => void;
  height?: string;
  defaultCountry?: string;
}

const placeholderCls =
  'bg-[#f8f9fc] dark:bg-[#1a1d28] rounded-lg flex flex-col items-center justify-center text-center';

function getAccuracyMessage(
  accuracy: string | null | undefined
): string | null {
  switch (accuracy) {
    case 'ROOFTOP':
    case 'RANGE_INTERPOLATED':
      return 'Drag the pin to fine-tune the location';
    case 'MANUAL':
      return 'Pin placed manually — drag to adjust if needed';
    case 'CITY':
    case 'GEOMETRIC_CENTER':
    case 'APPROXIMATE':
      return 'We found the neighborhood but not the exact spot — drag the pin to your property!';
    case 'COUNTRY':
      return 'Well, we found the country at least! Drag the pin to the right location.';
    default:
      return null;
  }
}

function getZoomForAccuracy(accuracy: string | null | undefined): number {
  switch (accuracy) {
    case 'ROOFTOP':
    case 'RANGE_INTERPOLATED':
    case 'MANUAL':
      return 15;
    case 'CITY':
    case 'GEOMETRIC_CENTER':
    case 'APPROXIMATE':
      return 12;
    case 'COUNTRY':
      return 5;
    default:
      return 15;
  }
}

function getCountryCenter(countryCode?: string): { lat: number; lng: number } {
  if (!countryCode) return DEFAULT_CENTER;
  return COUNTRY_CENTERS[countryCode.toUpperCase()] ?? DEFAULT_CENTER;
}

export const InteractiveMap = ({
  street,
  city,
  latitude,
  longitude,
  geocodeAccuracy,
  isGeocoding = false,
  onLocationChange,
  height = 'h-96',
  defaultCountry,
}: InteractiveMapProps) => {
  const apiKey = env('VITE_GOOGLE_MAPS_API_KEY');
  const [clickToPlaceActive, setClickToPlaceActive] = useState(false);
  const isInteractive = !!onLocationChange;

  const handleDragEnd = useCallback(
    (event: google.maps.MapMouseEvent) => {
      if (!onLocationChange || !event.latLng) return;
      onLocationChange(event.latLng.lat(), event.latLng.lng());
    },
    [onLocationChange]
  );

  const handleMapClick = useCallback(
    (event: MapMouseEvent) => {
      if (!onLocationChange || !clickToPlaceActive) return;
      const detail = event.detail;
      if (detail.latLng) {
        onLocationChange(detail.latLng.lat, detail.latLng.lng);
        setClickToPlaceActive(false);
      }
    },
    [onLocationChange, clickToPlaceActive]
  );

  // No API key
  if (!apiKey) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Map Preview Unavailable
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          Configure VITE_GOOGLE_MAPS_API_KEY to enable maps
        </p>
        <div className="mt-3 text-xs text-[#9ca0b8] dark:text-[#5c6180] bg-[#f1f3f9] dark:bg-[#1e2130] rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Geocoding in progress
  if (isGeocoding) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <Loader2 className="h-10 w-10 text-[#5c7cfa] animate-spin mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Finding location...
        </p>
        <div className="mt-3 text-xs text-[#9ca0b8] dark:text-[#5c6180] bg-[#f1f3f9] dark:bg-[#1e2130] rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Click-to-place mode (no coordinates, user activated manual placement)
  if (clickToPlaceActive && latitude == null && longitude == null) {
    const center = getCountryCenter(defaultCountry);
    return (
      <div className="space-y-2">
        <div
          className={`w-full ${height} rounded-lg overflow-hidden border border-[#e2e6f0] dark:border-[#2a2e3f]`}
        >
          <APIProvider apiKey={apiKey}>
            <Map
              center={center}
              zoom={5}
              mapId="interactive-map"
              gestureHandling="cooperative"
              disableDefaultUI={false}
              onClick={handleMapClick}
            />
          </APIProvider>
        </div>
        <p className="text-sm text-[#5c7cfa] dark:text-[#7b9cff] text-center">
          Our map skills failed us this time. Click anywhere on the map to place
          your property pin.
        </p>
      </div>
    );
  }

  // No coordinates, interactive mode — offer click-to-place
  if ((latitude == null || longitude == null) && isInteractive) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Location Not Found
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mb-3">
          We couldn&apos;t find this address on the map
        </p>
        <button
          type="button"
          onClick={() => setClickToPlaceActive(true)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors text-sm font-medium"
        >
          <MousePointerClick className="h-4 w-4" />
          Place pin manually
        </button>
      </div>
    );
  }

  // No coordinates, read-only mode
  if (latitude == null || longitude == null) {
    return (
      <div className={`${height} ${placeholderCls}`}>
        <MapPin className="h-12 w-12 text-[#c9cfd9] dark:text-[#3a3f54] mb-3" />
        <p className="text-[#6b7194] dark:text-[#8b90a8] font-medium mb-1">
          Location Not Found
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          Could not find this address on the map
        </p>
        <div className="mt-3 text-xs text-[#9ca0b8] dark:text-[#5c6180] bg-[#f1f3f9] dark:bg-[#1e2130] rounded p-2 font-mono">
          {street}, {city}
        </div>
      </div>
    );
  }

  // Has coordinates — render map with marker
  const coordinates = { lat: latitude, lng: longitude };
  const zoom = getZoomForAccuracy(geocodeAccuracy);
  const accuracyMessage = isInteractive
    ? getAccuracyMessage(geocodeAccuracy)
    : null;

  return (
    <div className="space-y-2">
      <div
        className={`w-full ${height} rounded-lg overflow-hidden border border-[#e2e6f0] dark:border-[#2a2e3f]`}
      >
        <APIProvider apiKey={apiKey}>
          <Map
            center={coordinates}
            zoom={zoom}
            mapId="interactive-map"
            gestureHandling="cooperative"
            disableDefaultUI={false}
          >
            <AdvancedMarker
              position={coordinates}
              title={`${street}, ${city}`}
              draggable={isInteractive}
              onDragEnd={isInteractive ? handleDragEnd : undefined}
            />
          </Map>
        </APIProvider>
      </div>
      {accuracyMessage && (
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] text-center">
          {accuracyMessage}
        </p>
      )}
    </div>
  );
};
